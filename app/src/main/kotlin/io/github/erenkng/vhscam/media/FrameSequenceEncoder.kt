package io.github.erenkng.vhscam.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import io.github.erenkng.vhscam.core.media.StopMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Turns still frames into a silent MP4, each frame held for 1 / rate of a second. */
@OptIn(UnstableApi::class)
class FrameSequenceEncoder(private val context: Context) {

    /**
     * Encodes [frames] (JPEGs, ideally all [width] x [height]) at [rate] pictures a second into
     * [output]. A frame of another size is fitted in with black bars.
     */
    suspend fun encode(
        frames: List<File>,
        rate: Int,
        width: Int,
        height: Int,
        output: File,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.Main) {
        require(frames.isNotEmpty()) { "No frames" }
        output.delete()
        // Encoders want even sizes; every frame is scaled into the same one.
        val size = Presentation.createForWidthAndHeight(width and 1.inv(), height and 1.inv(), Presentation.LAYOUT_SCALE_TO_FIT)
        val frameMillis = (1_000L / rate).coerceAtLeast(1L)
        val items = frames.map { frame ->
            val item = MediaItem.Builder()
                .setUri(Uri.fromFile(frame))
                .setMimeType(MimeTypes.IMAGE_JPEG)
                .setImageDurationMs(frameMillis)
                .build()
            EditedMediaItem.Builder(item)
                // A whole number of video frames per picture, so the timing stays even.
                .setFrameRate(rate * StopMotion.repeat(rate))
                .setEffects(Effects(emptyList(), listOf(size)))
                .build()
        }
        val composition = Composition.Builder(EditedMediaItemSequence.withVideoFrom(items)).build()
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                })
                .build()
            transformer.start(composition, output.absolutePath)
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
