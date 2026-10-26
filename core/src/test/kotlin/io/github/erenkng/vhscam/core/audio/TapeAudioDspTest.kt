package io.github.erenkng.vhscam.core.audio

import io.github.erenkng.vhscam.core.look.TapeSpeed
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class TapeAudioDspTest {

    private val sampleRate = 48_000

    /** Filters only: no hiss, wow, flutter or saturation colouring. */
    private fun cleanConfig(speed: TapeSpeed) = TapeAudioConfig.forSpeed(speed).copy(
        wowDepth = 0f,
        flutterDepth = 0f,
        hissDb = -200f,
        drive = 0.01f,
    )

    private fun rmsDbOfSine(config: TapeAudioConfig, frequency: Float): Double {
        val dsp = TapeAudioDsp(sampleRate, 1, config)
        val seconds = 1.0
        val n = (sampleRate * seconds).toInt()
        var sumSquares = 0.0
        var counted = 0
        for (i in 0 until n) {
            val x = 0.5f * sin(2.0 * PI * frequency * i / sampleRate).toFloat()
            val y = dsp.processSample(x)
            if (i > sampleRate / 4) { // skip the settling time of the filters
                sumSquares += (y * y).toDouble()
                counted++
            }
        }
        return 20.0 * log10(sqrt(sumSquares / counted) + 1e-12)
    }

    @Test
    fun passesTheMidrange() {
        val level = rmsDbOfSine(cleanConfig(TapeSpeed.SP), 1_000f)
        val input = 20.0 * log10(0.5 / sqrt(2.0))
        assertTrue(level > input - 1.5, "1 kHz should pass: $level dB vs $input dB")
    }

    @Test
    fun cutsHighsAccordingToTapeSpeed() {
        val mid = rmsDbOfSine(cleanConfig(TapeSpeed.EP), 1_000f)
        val high = rmsDbOfSine(cleanConfig(TapeSpeed.EP), 14_000f)
        assertTrue(mid - high > 20.0, "EP should cut 14 kHz by > 20 dB, got ${mid - high}")

        val spHigh = rmsDbOfSine(cleanConfig(TapeSpeed.SP), 9_000f)
        val epHigh = rmsDbOfSine(cleanConfig(TapeSpeed.EP), 9_000f)
        assertTrue(spHigh > epHigh + 6.0, "SP keeps more top end than EP: $spHigh vs $epHigh")
    }

    @Test
    fun cutsRumble() {
        val mid = rmsDbOfSine(cleanConfig(TapeSpeed.SP), 1_000f)
        val low = rmsDbOfSine(cleanConfig(TapeSpeed.SP), 25f)
        assertTrue(mid - low > 15.0, "25 Hz should be cut by > 15 dB, got ${mid - low}")
    }

    @Test
    fun staysBoundedAndFiniteWithEverythingOn() {
        val dsp = TapeAudioDsp(sampleRate, 2, TapeAudioConfig.forSpeed(TapeSpeed.EP).copy(humDb = -55f))
        val frames = sampleRate
        val pcm = ShortArray(frames * 2) { i -> if (i % 2 == 0) 32767 else -32768 }
        dsp.process(pcm, frames, pcm)
        for (i in 0 until frames) {
            // Mono output is written to both channels.
            assertTrue(pcm[2 * i] == pcm[2 * i + 1])
        }
        val dsp2 = TapeAudioDsp(sampleRate, 1, TapeAudioConfig.forSpeed(TapeSpeed.EP))
        repeat(sampleRate) {
            val y = dsp2.processSample(if (it % 2 == 0) 1f else -1f)
            assertTrue(y.isFinite() && y in -1f..1f)
        }
    }

    @Test
    fun addsAudibleHissToSilence() {
        val dsp = TapeAudioDsp(sampleRate, 1, TapeAudioConfig.forSpeed(TapeSpeed.EP))
        var sumSquares = 0.0
        repeat(sampleRate) { sumSquares += dsp.processSample(0f).let { (it * it).toDouble() } }
        val db = 20.0 * log10(sqrt(sumSquares / sampleRate))
        assertTrue(db in -70.0..-30.0, "hiss level $db dBFS")
    }

    @Test
    fun isDeterministicForAGivenSeed() {
        val config = TapeAudioConfig.forSpeed(TapeSpeed.LP)
        val input = ShortArray(4_800) { (sin(it * 0.05) * 12_000).toInt().toShort() }
        val a = input.copyOf().also { TapeAudioDsp(sampleRate, 1, config, seed = 7).process(it, it.size, it) }
        val b = input.copyOf().also { TapeAudioDsp(sampleRate, 1, config, seed = 7).process(it, it.size, it) }
        assertContentEquals(a, b)
    }
}
