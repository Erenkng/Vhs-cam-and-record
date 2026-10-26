package io.github.erenkng.vhscam.core.audio

import io.github.erenkng.vhscam.core.look.TapeSpeed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/** Character of the linear audio track of a VHS deck at a given tape speed. */
data class TapeAudioConfig(
    val lowPassHz: Float,
    val highPassHz: Float = 80f,
    /** Fractional pitch deviation of the slow "wow" (0.002 = 0.2 %). */
    val wowDepth: Float,
    val wowRateHz: Float = 0.55f,
    /** Fractional pitch deviation of the fast "flutter". */
    val flutterDepth: Float,
    val flutterRateHz: Float = 7.5f,
    /** RMS level of the tape hiss, dBFS. */
    val hissDb: Float,
    /** Soft saturation drive; 1 is almost clean. */
    val drive: Float,
    /** RMS level of mains hum, dBFS, or null for none. */
    val humDb: Float? = null,
    val humHz: Float = 50f,
) {
    companion object {
        fun forSpeed(speed: TapeSpeed): TapeAudioConfig = when (speed) {
            TapeSpeed.SP -> TapeAudioConfig(
                lowPassHz = 10_000f, wowDepth = 0.0018f, flutterDepth = 0.0006f,
                hissDb = -50f, drive = 1.4f,
            )
            TapeSpeed.LP -> TapeAudioConfig(
                lowPassHz = 8_000f, wowDepth = 0.0026f, flutterDepth = 0.0009f,
                hissDb = -46f, drive = 1.7f,
            )
            TapeSpeed.EP -> TapeAudioConfig(
                lowPassHz = 6_000f, wowDepth = 0.0036f, flutterDepth = 0.0014f,
                hissDb = -42f, drive = 2.0f,
            )
        }
    }
}

/**
 * Streaming "linear track" tape emulation: mono sum, band limiting, wow & flutter, hiss and soft
 * saturation. Works on interleaved 16-bit PCM and writes the mono result to every channel so the
 * output keeps the input layout.
 */
class TapeAudioDsp(
    private val sampleRate: Int,
    private val channelCount: Int,
    private val config: TapeAudioConfig,
    private val seed: Long = 1997L,
) {
    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(channelCount > 0) { "channelCount must be positive" }
    }

    private val highPass = Biquad.highPass(sampleRate.toFloat(), config.highPassHz, BUTTERWORTH_Q)
    private val lowPass1 = Biquad.lowPass(sampleRate.toFloat(), config.lowPassHz, 0.541f)
    private val lowPass2 = Biquad.lowPass(sampleRate.toFloat(), config.lowPassHz, 1.307f)
    private val hissTilt = Biquad.highPass(sampleRate.toFloat(), 1_500f, BUTTERWORTH_Q)

    private val delayLine = FloatArray(nextPowerOfTwo((sampleRate * 0.05f).toInt()))
    private val delayMask = delayLine.size - 1
    private var writeIndex = 0
    private var sampleCounter = 0L

    private val baseDelay = 0.010f * sampleRate
    private val wowAmplitude = config.wowDepth / (2f * PI.toFloat() * config.wowRateHz) * sampleRate
    private val flutterAmplitude =
        config.flutterDepth / (2f * PI.toFloat() * config.flutterRateHz) * sampleRate
    private val wowPhase: Float
    private var drift = 0f
    private var driftTarget = 0f

    private val hissGain = dbToGain(config.hissDb) * HISS_NORMALISATION
    private val humGain = config.humDb?.let { dbToGain(it) * sqrt(2f) } ?: 0f
    private val driveNorm = tanh(config.drive)

    private var random = Random(seed)

    init {
        wowPhase = random.nextFloat() * 2f * PI.toFloat()
    }

    fun reset() {
        highPass.reset()
        lowPass1.reset()
        lowPass2.reset()
        hissTilt.reset()
        delayLine.fill(0f)
        writeIndex = 0
        sampleCounter = 0L
        drift = 0f
        driftTarget = 0f
        random = Random(seed)
    }

    /**
     * Processes [frames] interleaved frames from [input] into [output] (same layout).
     * [output] may be the same array as [input].
     */
    fun process(input: ShortArray, frames: Int, output: ShortArray) {
        require(input.size >= frames * channelCount && output.size >= frames * channelCount)
        var i = 0
        for (frame in 0 until frames) {
            var sum = 0f
            for (ch in 0 until channelCount) sum += input[i + ch] / 32768f
            val y = processSample(sum / channelCount)
            val s = (y * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            for (ch in 0 until channelCount) output[i + ch] = s
            i += channelCount
        }
    }

    /** Processes one mono sample in -1..1. */
    fun processSample(x: Float): Float {
        val t = sampleCounter.toFloat() / sampleRate
        sampleCounter++

        // Slow random drift of the capstan speed on top of the periodic wow.
        if (sampleCounter % (sampleRate / 4).coerceAtLeast(1) == 0L) {
            driftTarget = (random.nextFloat() * 2f - 1f) * wowAmplitude * 0.4f
        }
        drift += (driftTarget - drift) * 0.0005f

        val hp = highPass.process(x)
        delayLine[writeIndex] = hp
        val delay = baseDelay +
            wowAmplitude * sin(2f * PI.toFloat() * config.wowRateHz * t + wowPhase) +
            flutterAmplitude * sin(2f * PI.toFloat() * config.flutterRateHz * t) +
            drift
        val wobbled = readDelayed(delay)
        writeIndex = (writeIndex + 1) and delayMask

        val white = (random.nextFloat() + random.nextFloat() - 1f)
        val hiss = hissTilt.process(white) * hissGain
        val hum = if (humGain > 0f) {
            val w = 2f * PI.toFloat() * config.humHz * t
            humGain * (0.7f * sin(w) + 0.3f * sin(3f * w))
        } else {
            0f
        }

        val saturated = tanh(config.drive * (wobbled + hiss + hum)) / driveNorm
        return (lowPass2.process(lowPass1.process(saturated)) * OUTPUT_GAIN).coerceIn(-1f, 1f)
    }

    private fun readDelayed(delaySamples: Float): Float {
        val readPos = writeIndex - delaySamples
        val base = kotlin.math.floor(readPos).toInt()
        val frac = readPos - base
        val a = delayLine[base and delayMask]
        val b = delayLine[(base + 1) and delayMask]
        return a + (b - a) * frac
    }

    private companion object {
        const val BUTTERWORTH_Q = 0.7071f
        const val OUTPUT_GAIN = 0.95f

        // RMS of (u1 + u2 - 1) for uniform u is 1/sqrt(6). The tilt and the final low-pass then
        // shape it, so the hiss level is approximate.
        val HISS_NORMALISATION = sqrt(6f)

        fun dbToGain(db: Float): Float = 10f.pow(db / 20f)

        fun nextPowerOfTwo(n: Int): Int {
            var p = 1
            while (p < n) p = p shl 1
            return p
        }
    }
}

/** Transposed direct form II biquad (RBJ audio EQ cookbook coefficients). */
class Biquad private constructor(
    private val b0: Float,
    private val b1: Float,
    private val b2: Float,
    private val a1: Float,
    private val a2: Float,
) {
    private var z1 = 0f
    private var z2 = 0f

    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    fun reset() {
        z1 = 0f
        z2 = 0f
    }

    companion object {
        fun lowPass(sampleRate: Float, cutoffHz: Float, q: Float): Biquad {
            val w0 = 2.0 * PI * (cutoffHz.coerceAtMost(sampleRate * 0.45f)) / sampleRate
            val cosW = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = ((1.0 - cosW) / 2.0 / a0).toFloat(),
                b1 = ((1.0 - cosW) / a0).toFloat(),
                b2 = ((1.0 - cosW) / 2.0 / a0).toFloat(),
                a1 = (-2.0 * cosW / a0).toFloat(),
                a2 = ((1.0 - alpha) / a0).toFloat(),
            )
        }

        fun highPass(sampleRate: Float, cutoffHz: Float, q: Float): Biquad {
            val w0 = 2.0 * PI * cutoffHz / sampleRate
            val cosW = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val a0 = 1.0 + alpha
            return Biquad(
                b0 = ((1.0 + cosW) / 2.0 / a0).toFloat(),
                b1 = (-(1.0 + cosW) / a0).toFloat(),
                b2 = ((1.0 + cosW) / 2.0 / a0).toFloat(),
                a1 = (-2.0 * cosW / a0).toFloat(),
                a2 = ((1.0 - alpha) / a0).toFloat(),
            )
        }
    }
}
