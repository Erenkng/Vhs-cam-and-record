package io.github.erenkng.vhscam.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import io.github.erenkng.vhscam.core.audio.TapeAudioConfig
import io.github.erenkng.vhscam.core.audio.TapeAudioDsp
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Media3 audio processor that runs [TapeAudioDsp] over 16-bit PCM. */
@OptIn(UnstableApi::class)
class VhsAudioProcessor(private val config: TapeAudioConfig) : BaseAudioProcessor() {

    private var dsp: TapeAudioDsp? = null
    private var scratch = ShortArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        dsp = TapeAudioDsp(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, config)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val processor = dsp ?: return
        val channels = inputAudioFormat.channelCount
        val samples = inputBuffer.remaining() / 2
        val frames = samples / channels
        if (frames == 0) return
        if (scratch.size < samples) scratch = ShortArray(samples)
        val shorts = inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        shorts.get(scratch, 0, frames * channels)
        inputBuffer.position(inputBuffer.position() + frames * channels * 2)
        processor.process(scratch, frames, scratch)
        val output = replaceOutputBuffer(frames * channels * 2)
        output.asShortBuffer().put(scratch, 0, frames * channels)
        output.position(output.position() + frames * channels * 2)
        output.flip()
    }

    override fun onReset() {
        dsp = null
    }
}
