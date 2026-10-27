package io.github.erenkng.vhscam.media

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import io.github.erenkng.vhscam.camera.CapturedPhoto
import io.github.erenkng.vhscam.core.media.FilmLab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Properties

/** A roll at the lab: [shots] prints that come out at [readyAtMillis]. */
data class LabRoll(val id: String, val shots: Int, val readyAtMillis: Long)

/** The single-use camera's film: shots on the roll in the camera, and rolls at the lab. */
data class RollState(val inCamera: Int = 0, val atLab: List<LabRoll> = emptyList()) {
    val left: Int get() = FilmLab.ROLL_SIZE - inCamera

    /** There is film to show: shots on the roll or rolls at the lab. */
    val hasFilm: Boolean get() = inCamera > 0 || atLab.isNotEmpty()
}

/**
 * Keeps the single-use camera's shots out of the gallery until they are developed: shots wait
 * on the roll in the camera, a full roll goes to the lab, and the morning after, its prints
 * move into the gallery. Everything waits in app storage, so none of it shows up in the
 * phone's gallery early.
 */
class FilmRollStore(
    context: Context,
    private val media: MediaRepository,
    private val originals: OriginalsStore,
) {

    private val root = File(context.filesDir, "rolls")
    private val camera = File(root, "camera")
    private val lab = File(root, "lab")
    private val mutex = Mutex()
    private val zone get() = ZoneId.systemDefault()

    private val _state = MutableStateFlow(RollState())
    val state: StateFlow<RollState> = _state.asStateFlow()

    /** Reads the rolls from storage; the state is empty until this runs. */
    suspend fun load() = mutex.withLock { refresh() }

    /**
     * Winds a finished [print] onto the roll in the camera, with the untouched [original] when
     * the look may be changed later. Returns the roll that went to the lab because this was
     * its last frame, or null.
     */
    suspend fun addShot(print: File, capturedAtMillis: Long, original: Original?): LabRoll? = mutex.withLock {
        withContext(Dispatchers.IO) {
            camera.mkdirs()
            val frame = frames(camera).size + 1
            print.copyTo(File(camera, frameName(frame, JPEG)), overwrite = true)
            val properties = Properties().apply { setProperty(TAKEN, capturedAtMillis.toString()) }
            val originalBytes = original?.photo?.let { photo ->
                photo.jpeg ?: photo.bitmap?.let { bitmap ->
                    ByteArrayOutputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        out.toByteArray()
                    }
                }
            }
            if (original != null && originalBytes != null) {
                File(camera, frameName(frame, ORIGINAL)).writeBytes(originalBytes)
                properties.setProperty(ROTATION, original.photo.rotationDegrees.toString())
                properties.setProperty(FRONT, original.photo.front.toString())
                properties.setProperty(MIRROR, original.mirror.toString())
            }
            File(camera, frameName(frame, META)).outputStream().use { properties.store(it, null) }
        }
        val full = withContext(Dispatchers.IO) { frames(camera).size >= FilmLab.ROLL_SIZE }
        val sent = if (full) send(System.currentTimeMillis()) else null
        refresh()
        sent
    }

    /** Takes the roll out of the camera, however many shots it has, and drops it at the lab. */
    suspend fun sendToLab(nowMillis: Long = System.currentTimeMillis()): LabRoll? = mutex.withLock {
        send(nowMillis).also { refresh() }
    }

    private suspend fun send(nowMillis: Long): LabRoll? = withContext(Dispatchers.IO) {
        val shots = frames(camera).size
        if (shots == 0) return@withContext null
        val sentAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        val readyAt = FilmLab.readyAt(sentAt).atZone(zone).toInstant().toEpochMilli()
        val id = "roll_$nowMillis"
        lab.mkdirs()
        val dir = File(lab, id)
        if (!camera.renameTo(dir)) throw IOException("Could not move the roll to $dir")
        Properties().apply { setProperty(READY, readyAt.toString()) }
            .let { p -> File(dir, ROLL_META).outputStream().use { p.store(it, null) } }
        LabRoll(id, shots, readyAt)
    }

    /**
     * Moves the prints of every roll that is ready (or of every roll, with [everything]) into
     * the gallery. Returns how many prints came out.
     */
    suspend fun develop(nowMillis: Long = System.currentTimeMillis(), everything: Boolean = false): Int = mutex.withLock {
        val developed = withContext(Dispatchers.IO) {
            val ready = lab.listFiles()?.filter { it.isDirectory && (everything || readyAt(it) <= nowMillis) }.orEmpty()
            var count = 0
            for (dir in ready.sortedBy { it.name }) {
                for (frame in frames(dir)) {
                    try {
                        developFrame(dir, frame)
                        count++
                        // Gone from the roll as soon as it is in the gallery, so a crash can't print it twice.
                        listOf(JPEG, ORIGINAL, META).forEach { File(dir, frameName(frame, it)).delete() }
                    } catch (e: IOException) {
                        Log.w(TAG, "Could not develop frame $frame of ${dir.name}; trying again next time", e)
                    }
                }
                // A roll with frames left over stays at the lab and is tried again.
                if (frames(dir).isEmpty()) dir.deleteRecursively()
            }
            count
        }
        refresh()
        developed
    }

    private suspend fun developFrame(dir: File, frame: Int) {
        val properties = withContext(Dispatchers.IO) {
            Properties().apply { File(dir, frameName(frame, META)).inputStream().use { load(it) } }
        }
        val taken = properties.getProperty(TAKEN)?.toLongOrNull() ?: System.currentTimeMillis()
        val capturedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(taken), zone)
        val uri = media.savePhoto(File(dir, frameName(frame, JPEG)), FilmLab.printName(capturedAt, frame), taken)
        val original = File(dir, frameName(frame, ORIGINAL))
        if (original.exists()) {
            val photo = CapturedPhoto(
                jpeg = withContext(Dispatchers.IO) { original.readBytes() },
                bitmap = null,
                rotationDegrees = properties.getProperty(ROTATION)?.toIntOrNull() ?: 0,
                front = properties.getProperty(FRONT) == "true",
            )
            originals.save(ContentUris.parseId(uri), photo, properties.getProperty(MIRROR) == "true", taken)
        }
    }

    private suspend fun refresh() {
        _state.value = withContext(Dispatchers.IO) {
            RollState(
                inCamera = frames(camera).size,
                atLab = lab.listFiles()?.filter { it.isDirectory }.orEmpty()
                    .map { LabRoll(it.name, frames(it).size, readyAt(it)) }
                    .sortedBy { it.readyAtMillis },
            )
        }
    }

    private fun readyAt(dir: File): Long = try {
        Properties().apply { File(dir, ROLL_META).inputStream().use { load(it) } }
            .getProperty(READY)?.toLongOrNull() ?: 0L
    } catch (e: IOException) {
        0L
    }

    /** Frame numbers on a roll, in order. */
    private fun frames(dir: File): List<Int> =
        dir.listFiles()?.mapNotNull { FRAME.matchEntire(it.name)?.groupValues?.get(1)?.toInt() }.orEmpty().sorted()

    private fun frameName(frame: Int, suffix: String) = "frame_" + frame.toString().padStart(2, '0') + suffix

    private companion object {
        const val TAG = "FilmRollStore"
        const val JPEG = ".jpg"
        const val ORIGINAL = "_original.jpg"
        const val META = ".properties"
        const val ROLL_META = "roll.properties"
        const val TAKEN = "taken"
        const val ROTATION = "rotation"
        const val FRONT = "front"
        const val MIRROR = "mirror"
        const val READY = "ready"
        val FRAME = Regex("frame_(\\d+)\\.jpg")
    }
}
