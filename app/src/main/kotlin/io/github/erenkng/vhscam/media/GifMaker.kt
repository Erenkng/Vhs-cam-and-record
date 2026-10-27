package io.github.erenkng.vhscam.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import io.github.erenkng.vhscam.core.gif.GifEncoder
import io.github.erenkng.vhscam.core.media.FileNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import kotlin.coroutines.coroutineContext

/** Turns a few seconds of a tape into a small, looping, web-safe GIF in the gallery. */
class GifMaker(private val context: Context, private val media: MediaRepository) {

    private val _progress = MutableStateFlow<Float?>(null)

    /** 0..1 while a GIF is being made, null otherwise. */
    val progress: StateFlow<Float?> = _progress.asStateFlow()

    /** Makes a GIF of the [CLIP_MILLIS] from [startMillis] on (or the end of the video). */
    suspend fun fromVideo(video: Uri, startMillis: Long): Uri {
        _progress.value = 0f
        val file = File(context.cacheDir, "gif_${System.nanoTime()}.gif")
        try {
            withContext(Dispatchers.Default) { encode(video, startMillis, file) }
            val now = System.currentTimeMillis()
            return media.savePhoto(file, FileNames.gif(LocalDateTime.now()), now, mimeType = "image/gif")
        } finally {
            file.delete()
            _progress.value = null
        }
    }

    private suspend fun encode(video: Uri, startMillis: Long, file: File) {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, video)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val start = startMillis.coerceIn(0L, (duration - CLIP_MILLIS).coerceAtLeast(0L))
            val count = (minOf(CLIP_MILLIS, duration - start) / FRAME_MILLIS).toInt().coerceAtLeast(1)
            var encoder: GifEncoder? = null
            file.outputStream().buffered().use { out ->
                for (i in 0 until count) {
                    coroutineContext.ensureActive()
                    val timeUs = (start + i * FRAME_MILLIS) * 1_000L
                    // Frames come upright and fitted inside LONG_EDGE x LONG_EDGE.
                    val frame = retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, LONG_EDGE, LONG_EDGE)
                        ?: continue
                    val gif = encoder ?: GifEncoder(out, frame.width, frame.height).also { encoder = it }
                    gif.addFrame(pixels(frame, gif), (FRAME_MILLIS / 10).toInt())
                    _progress.value = (i + 1f) / count
                }
                encoder?.finish() ?: throw IOException("No frames in $video")
            }
        }
    }

    /** The frame's pixels at the GIF's size (the first frame decides it). */
    private fun pixels(frame: Bitmap, gif: GifEncoder): IntArray {
        val fitted = if (frame.width == gif.width && frame.height == gif.height) {
            frame
        } else {
            Bitmap.createScaledBitmap(frame, gif.width, gif.height, true).also { frame.recycle() }
        }
        val pixels = IntArray(gif.width * gif.height)
        fitted.getPixels(pixels, 0, gif.width, 0, 0, gif.width, gif.height)
        fitted.recycle()
        return pixels
    }

    companion object {
        const val CLIP_MILLIS = 3_000L

        /** 10 pictures a second, like a GIF off a 90s web page. */
        const val FRAME_MILLIS = 100L
        const val LONG_EDGE = 320
    }
}
