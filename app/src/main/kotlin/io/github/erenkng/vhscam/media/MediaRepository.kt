package io.github.erenkng.vhscam.media

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.video.MediaStoreOutputOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** A photo or video this app saved. */
data class CapturedMedia(
    val uri: Uri,
    val isVideo: Boolean,
    val displayName: String,
    val dateTakenMillis: Long,
    val durationMillis: Long,
    val width: Int,
    val height: Int,
)

/**
 * Saves to and reads from DCIM/VHS Cam through MediaStore. On Android 10+ an app can see and
 * delete what it created without any storage permission.
 */
class MediaRepository(private val context: Context) {

    private val resolver get() = context.contentResolver

    private val _version = MutableStateFlow(0)

    /** Bumped whenever something is added or removed, so lists know to reload. */
    val version: StateFlow<Int> = _version.asStateFlow()

    private val _latest = MutableStateFlow<CapturedMedia?>(null)

    /** The most recent capture, for the gallery button on the camera screen. */
    val latest: StateFlow<CapturedMedia?> = _latest.asStateFlow()

    private fun changed() {
        _version.value++
    }

    suspend fun savePhoto(jpeg: File, displayName: String, takenAtMillis: Long): Uri = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.MediaColumns.DATE_TAKEN, takenAtMillis)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        insertFromFile(IMAGES, values, jpeg)
    }

    /** Copies a finished video (e.g. after tape-audio processing) into the gallery. */
    suspend fun saveVideo(video: File, displayName: String, takenAtMillis: Long): Uri = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.MediaColumns.DATE_TAKEN, takenAtMillis)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        insertFromFile(VIDEOS, values, video)
    }

    /** Output options for recording straight into the gallery. */
    fun videoOutputOptions(displayName: String, takenAtMillis: Long): MediaStoreOutputOptions {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.MediaColumns.DATE_TAKEN, takenAtMillis)
        }
        return MediaStoreOutputOptions.Builder(resolver, VIDEOS).setContentValues(values).build()
    }

    /** Call after CameraX finished writing a video straight into MediaStore. */
    suspend fun onVideoRecorded(uri: Uri) {
        refreshLatest(uri, isVideo = true)
        changed()
    }

    private fun insertFromFile(collection: Uri, values: ContentValues, file: File): Uri {
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert failed")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: throw IOException("Cannot open $uri")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            throw e
        }
        val isVideo = collection == VIDEOS
        _latest.value = query(collection, isVideo, uri)?.firstOrNull() ?: _latest.value
        changed()
        return uri
    }

    private suspend fun refreshLatest(uri: Uri, isVideo: Boolean) = withContext(Dispatchers.IO) {
        _latest.value = query(if (isVideo) VIDEOS else IMAGES, isVideo, uri)?.firstOrNull() ?: _latest.value
    }

    /** Everything in DCIM/VHS Cam that this app can see, newest first. */
    suspend fun loadAll(): List<CapturedMedia> = withContext(Dispatchers.IO) {
        val all = (query(IMAGES, isVideo = false).orEmpty() + query(VIDEOS, isVideo = true).orEmpty())
            .sortedByDescending { it.dateTakenMillis }
        _latest.value = all.firstOrNull()
        all
    }

    suspend fun delete(media: CapturedMedia): Boolean = withContext(Dispatchers.IO) {
        val deleted = try {
            resolver.delete(media.uri, null, null) > 0
        } catch (e: SecurityException) {
            false
        }
        if (deleted) {
            if (_latest.value?.uri == media.uri) _latest.value = null
            changed()
        }
        deleted
    }

    private fun query(collection: Uri, isVideo: Boolean, single: Uri? = null): List<CapturedMedia>? {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            if (isVideo) add(MediaStore.MediaColumns.DURATION)
        }.toTypedArray()
        val selection: String
        val args: Array<String>
        if (single != null) {
            selection = "${MediaStore.MediaColumns._ID} = ?"
            args = arrayOf(ContentUris.parseId(single).toString())
        } else {
            selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            args = arrayOf("$RELATIVE_PATH%")
        }
        return resolver.query(
            collection, projection, selection, args, "${MediaStore.MediaColumns.DATE_TAKEN} DESC",
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val taken = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val width = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val height = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val duration = if (isVideo) c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION) else -1
            buildList {
                while (c.moveToNext()) {
                    add(
                        CapturedMedia(
                            uri = ContentUris.withAppendedId(collection, c.getLong(id)),
                            isVideo = isVideo,
                            displayName = c.getString(name) ?: "",
                            dateTakenMillis = c.getLong(taken),
                            durationMillis = if (duration >= 0) c.getLong(duration) else 0L,
                            width = c.getInt(width),
                            height = c.getInt(height),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val RELATIVE_PATH = "DCIM/VHS Cam/"
        private val IMAGES: Uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        private val VIDEOS: Uri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }
}
