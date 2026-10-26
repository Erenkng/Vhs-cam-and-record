package io.github.erenkng.vhscam.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import io.github.erenkng.vhscam.core.audio.TapeAudioConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Re-processes only the audio of a recording with [VhsAudioProcessor]. The video track is
 * copied as-is, so this takes a fraction of the clip's length.
 */
@OptIn(UnstableApi::class)
class TapeAudioExporter(private val context: Context) {

    suspend fun export(
        input: File,
        output: File,
        config: TapeAudioConfig,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.Main) {
        output.delete()
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                })
                .build()
            val item = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(input)))
                .setEffects(Effects(listOf(VhsAudioProcessor(config)), emptyList()))
                .build()
            transformer.start(item, output.absolutePath)
            continuation.invokeOnCancellation { transformer.cancel() }

            val holder = ProgressHolder()
            launch {
                while (continuation.isActive) {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(holder.progress / 100f)
                    }
                    delay(100)
                }
            }
        }
    }
}
