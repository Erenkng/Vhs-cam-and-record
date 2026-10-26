package io.github.erenkng.vhscam.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import io.github.erenkng.vhscam.audio.TapeAudioExporter
import io.github.erenkng.vhscam.core.audio.TapeAudioConfig
import io.github.erenkng.vhscam.core.look.PhotoLook
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.core.media.FileNames
import io.github.erenkng.vhscam.gl.LookSpec
import io.github.erenkng.vhscam.gl.OsdContent
import io.github.erenkng.vhscam.gl.StillRenderer
import io.github.erenkng.vhscam.media.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.random.Random

/** Turns a captured JPEG into a finished retro photo in the gallery. */
class PhotoPipeline(
    private val context: Context,
    private val stillRenderer: StillRenderer,
    private val media: MediaRepository,
) {

    suspend fun develop(
        photo: CapturedPhoto,
        look: PhotoLook,
        spec: LookSpec,
        osd: OsdContent,
        mirror: Boolean,
        lookAspect: Float,
        capturedAt: LocalDateTime,
        capturedAtMillis: Long,
    ): Uri {
        val upright = withContext(Dispatchers.Default) {
            decodeUpright(photo, neededLongEdge = max(look.stillLongEdge, 1280))
        }
        val rendered = stillRenderer.render(
            source = upright,
            spec = spec,
            osdContent = osd,
            mirror = mirror,
            lookAspect = lookAspect,
            outputLongEdge = look.stillLongEdge,
            seedFrame = Random.nextInt(1, 1_000_000),
        )
        upright.recycle()

        val file = withContext(Dispatchers.IO) {
            val finished = if (look == PhotoLook.POLAROID) instantFrame(rendered) else rendered
            val quality = (spec as? LookSpec.Ccd)?.params?.jpegQuality ?: JPEG_QUALITY
            val out = File(context.cacheDir, "develop_${System.nanoTime()}.jpg")
            out.outputStream().use { finished.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            if (finished !== rendered) finished.recycle()
            rendered.recycle()
            writeExif(out, capturedAt)
            out
        }
        return try {
            media.savePhoto(file, FileNames.photo(capturedAt, look), capturedAtMillis)
        } finally {
            file.delete()
        }
    }

    private fun decodeUpright(photo: CapturedPhoto, neededLongEdge: Int): Bitmap {
        val decoded = photo.bitmap ?: run {
            val jpeg = photo.jpeg ?: error("Empty capture")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
            var sample = 1
            val longEdge = max(bounds.outWidth, bounds.outHeight)
            while (longEdge / (sample * 2) >= neededLongEdge) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options) ?: error("Cannot decode capture")
        }
        val longEdge = max(decoded.width, decoded.height)
        val scale = if (longEdge > neededLongEdge * 2) neededLongEdge * 2f / longEdge else 1f
        if (photo.rotationDegrees == 0 && scale == 1f && decoded.config == Bitmap.Config.ARGB_8888) return decoded
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(photo.rotationDegrees.toFloat())
        }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        return if (rotated.config == Bitmap.Config.ARGB_8888) {
            rotated
        } else {
            rotated.copy(Bitmap.Config.ARGB_8888, false).also { rotated.recycle() }
        }
    }

    /** Polaroid 600 proportions: an 88 x 107 mm card around a 79 x 79 mm picture. */
    private fun instantFrame(image: Bitmap): Bitmap {
        val s = image.width.toFloat()
        val card = Bitmap.createBitmap((s * 88f / 79f).toInt(), (s * 107f / 79f).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(card)
        canvas.drawColor(CARD_COLOR)
        canvas.drawBitmap(image, s * 4.5f / 79f, s * 6f / 79f, Paint(Paint.FILTER_BITMAP_FLAG))
        return card
    }

    private fun writeExif(file: File, capturedAt: LocalDateTime) {
        try {
            val stamp = capturedAt.format(EXIF_TIME)
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, stamp)
                setAttribute(ExifInterface.TAG_DATETIME, stamp)
                setAttribute(ExifInterface.TAG_SOFTWARE, "VHS Cam")
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }
        } catch (e: IOException) {
            Log.w(TAG, "Could not write EXIF", e)
        }
    }

    private companion object {
        const val TAG = "PhotoPipeline"
        const val JPEG_QUALITY = 92
        const val CARD_COLOR = 0xFFF3F0E8.toInt()
        val EXIF_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    }
}

/** Gives a finished recording its tape sound and puts it in the gallery. */
class VideoPipeline(
    private val context: Context,
    private val exporter: TapeAudioExporter,
    private val media: MediaRepository,
) {

    private val recordingsDir: File get() = File(context.cacheDir, "recordings").apply { mkdirs() }

    /** A temporary file for CameraX to record into before the audio is processed. */
    fun newRecordingFile(): File = File(recordingsDir, "rec_${System.currentTimeMillis()}.mp4")

    suspend fun finish(
        recorded: File,
        hasAudio: Boolean,
        speed: TapeSpeed,
        capturedAt: LocalDateTime,
        capturedAtMillis: Long,
        onProgress: (Float) -> Unit,
    ): Uri {
        val name = FileNames.video(capturedAt)
        if (!hasAudio) return saveAndDelete(recorded, name, capturedAtMillis)
        val processed = File(recordingsDir, "tape_${recorded.name}")
        return try {
            exporter.export(recorded, processed, TapeAudioConfig.forSpeed(speed), onProgress)
            val uri = media.saveVideo(processed, name, capturedAtMillis)
            recorded.delete()
            uri
        } catch (e: Exception) {
            Log.w(TAG, "Tape audio failed, keeping the original sound", e)
            saveAndDelete(recorded, name, capturedAtMillis)
        } finally {
            processed.delete()
        }
    }

    private suspend fun saveAndDelete(file: File, name: String, takenAt: Long): Uri =
        try {
            media.saveVideo(file, name, takenAt)
        } finally {
            file.delete()
        }

    /** Saves recordings left behind by a crash or kill before they were processed. */
    suspend fun recoverLeftovers() = withContext(Dispatchers.IO) {
        recordingsDir.listFiles()?.forEach { file ->
            when {
                file.name.startsWith("tape_") -> file.delete()
                file.length() > 0 -> try {
                    val time = LocalDateTime.now()
                    saveAndDelete(file, FileNames.video(time), file.lastModified())
                } catch (e: IOException) {
                    Log.w(TAG, "Could not recover ${file.name}", e)
                }
                else -> file.delete()
            }
        }
    }

    private companion object {
        const val TAG = "VideoPipeline"
    }
}
