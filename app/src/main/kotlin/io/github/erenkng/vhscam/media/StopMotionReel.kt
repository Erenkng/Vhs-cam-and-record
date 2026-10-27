package io.github.erenkng.vhscam.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.github.erenkng.vhscam.core.media.FileNames
import io.github.erenkng.vhscam.core.media.StopMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The stop-motion reel: [frames] shot so far, and [onion], the last one as the camera saw it,
 * to show over the viewfinder while the next one is lined up.
 */
data class ReelState(val frames: Int = 0, val onion: Bitmap? = null)

/**
 * The frames of a stop-motion animation. Each one is developed with the look as it is shot
 * and kept in app storage, so an animation can take as long as it takes, even with the app
 * closed in between; finishing turns the frames into a video in the gallery.
 */
class StopMotionReel(
    context: Context,
    private val media: MediaRepository,
    private val encoder: FrameSequenceEncoder,
) {

    private val dir = File(context.filesDir, "stopmotion")
    private val cacheDir = context.cacheDir

    /** Frames go on one at a time, in the order they were shot. */
    private val mutex = Mutex()

    /** Frames shot but still being developed. */
    private val pending = AtomicInteger()

    @Volatile
    private var stored = 0

    @Volatile
    private var onion: Bitmap? = null

    private val _state = MutableStateFlow(ReelState())
    val state: StateFlow<ReelState> = _state.asStateFlow()

    private fun publish() {
        _state.value = ReelState(frames = stored + pending.get(), onion = onion)
    }

    /** Reads the reel from storage; the state is empty until this runs. */
    suspend fun load() = mutex.withLock { reread() }

    /**
     * Adds a frame to the end of the reel. [shotOnion] shows over the viewfinder straight
     * away; [print] develops the frame once the ones shot before it are in, so they stay in
     * order however long each takes.
     */
    suspend fun add(shotOnion: Bitmap, print: suspend () -> File) {
        pending.incrementAndGet()
        onion = shotOnion
        publish()
        try {
            mutex.withLock {
                val file = print()
                try {
                    withContext(Dispatchers.IO) {
                        dir.mkdirs()
                        val number = (numbers().lastOrNull() ?: 0) + 1
                        File(dir, onionName(number)).outputStream().use { shotOnion.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                        // The frame last: without it, the onion skin is simply left over.
                        file.copyTo(File(dir, frameName(number)), overwrite = true)
                    }
                    stored++
                } finally {
                    file.delete()
                }
            }
        } finally {
            pending.decrementAndGet()
            publish()
        }
    }

    /** Takes the last frame off the reel, once the frames still being developed are in. */
    suspend fun undo() = mutex.withLock {
        withContext(Dispatchers.IO) {
            numbers().lastOrNull()?.let { last ->
                File(dir, frameName(last)).delete()
                File(dir, onionName(last)).delete()
            }
        }
        reread()
    }

    /** Throws the whole reel away. */
    suspend fun discard() = mutex.withLock {
        withContext(Dispatchers.IO) { dir.deleteRecursively() }
        reread()
    }

    /**
     * Makes the reel into a video in the gallery at [rate] pictures a second and starts a new
     * one. Frames still being developed go in first.
     */
    suspend fun finish(rate: Int, capturedAt: LocalDateTime, takenAtMillis: Long, onProgress: (Float) -> Unit): Uri =
        mutex.withLock {
            val frames = withContext(Dispatchers.IO) { numbers().map { File(dir, frameName(it)) } }
            check(frames.isNotEmpty()) { "No frames to animate" }
            val (width, height) = withContext(Dispatchers.IO) { videoSize(frames.first()) }
            val video = File(cacheDir, "anim_${System.nanoTime()}.mp4")
            try {
                encoder.encode(frames, rate, width, height, video, onProgress)
                val uri = media.saveVideo(video, FileNames.animation(capturedAt), takenAtMillis)
                withContext(Dispatchers.IO) { dir.deleteRecursively() }
                reread()
                uri
            } finally {
                video.delete()
            }
        }

    /** The first frame's size, at most [StopMotion.FRAME_LONG_EDGE] on the long edge. */
    private fun videoSize(frame: File): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(frame.absolutePath, bounds)
        val longEdge = max(bounds.outWidth, bounds.outHeight)
        if (longEdge <= 0) error("Cannot read ${frame.name}")
        val scale = minOf(1f, StopMotion.FRAME_LONG_EDGE.toFloat() / longEdge)
        return (bounds.outWidth * scale).roundToInt() to (bounds.outHeight * scale).roundToInt()
    }

    private suspend fun reread() {
        val (count, last) = withContext(Dispatchers.IO) {
            val numbers = numbers()
            numbers.size to numbers.lastOrNull()?.let { BitmapFactory.decodeFile(File(dir, onionName(it)).absolutePath) }
        }
        stored = count
        onion = last
        publish()
    }

    /** Frame numbers on the reel, in order. */
    private fun numbers(): List<Int> =
        dir.listFiles()?.mapNotNull { FRAME.matchEntire(it.name)?.groupValues?.get(1)?.toInt() }.orEmpty().sorted()

    private fun frameName(number: Int) = "frame_" + number.toString().padStart(4, '0') + ".jpg"

    private fun onionName(number: Int) = "onion_" + number.toString().padStart(4, '0') + ".jpg"

    private companion object {
        val FRAME = Regex("frame_(\\d+)\\.jpg")
    }
}
