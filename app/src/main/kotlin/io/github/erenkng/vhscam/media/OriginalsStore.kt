package io.github.erenkng.vhscam.media

import android.content.Context
import android.graphics.Bitmap
import io.github.erenkng.vhscam.camera.CapturedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties

/** An untouched capture, kept so the photo's look can be changed later. */
class Original(val photo: CapturedPhoto, val mirror: Boolean, val capturedAtMillis: Long)

/**
 * Keeps the camera's own JPEG of every photo in app storage, keyed by the MediaStore id of
 * the finished photo. Nothing here shows up in the phone's gallery.
 */
class OriginalsStore(context: Context) {

    private val dir = File(context.filesDir, "originals")

    private fun jpeg(id: Long) = File(dir, "$id.jpg")

    private fun meta(id: Long) = File(dir, "$id.properties")

    fun has(id: Long): Boolean = jpeg(id).exists() && meta(id).exists()

    suspend fun save(id: Long, photo: CapturedPhoto, mirror: Boolean, capturedAtMillis: Long) = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val bytes = photo.jpeg ?: photo.bitmap?.let { bitmap ->
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.toByteArray()
            }
        } ?: return@withContext
        jpeg(id).writeBytes(bytes)
        val properties = Properties().apply {
            setProperty(ROTATION, photo.rotationDegrees.toString())
            setProperty(FRONT, photo.front.toString())
            setProperty(MIRROR, mirror.toString())
            setProperty(TAKEN, capturedAtMillis.toString())
        }
        meta(id).outputStream().use { properties.store(it, null) }
    }

    suspend fun load(id: Long): Original? = withContext(Dispatchers.IO) {
        if (!has(id)) return@withContext null
        val properties = Properties()
        meta(id).inputStream().use { properties.load(it) }
        Original(
            photo = CapturedPhoto(
                jpeg = jpeg(id).readBytes(),
                bitmap = null,
                rotationDegrees = properties.getProperty(ROTATION)?.toIntOrNull() ?: 0,
                front = properties.getProperty(FRONT) == "true",
            ),
            mirror = properties.getProperty(MIRROR) == "true",
            capturedAtMillis = properties.getProperty(TAKEN)?.toLongOrNull() ?: System.currentTimeMillis(),
        )
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        jpeg(id).delete()
        meta(id).delete()
    }

    suspend fun sizeBytes(): Long = withContext(Dispatchers.IO) {
        dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val ROTATION = "rotation"
        const val FRONT = "front"
        const val MIRROR = "mirror"
        const val TAKEN = "taken"
    }
}
