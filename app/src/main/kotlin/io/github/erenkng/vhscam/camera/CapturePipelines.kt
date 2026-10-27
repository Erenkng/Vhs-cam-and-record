package io.github.erenkng.vhscam.camera

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.exifinterface.media.ExifInterface
import io.github.erenkng.vhscam.R
import io.github.erenkng.vhscam.audio.TapeAudioExporter
import io.github.erenkng.vhscam.core.audio.TapeAudioConfig
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.core.media.FileNames
import io.github.erenkng.vhscam.gl.LookSpec
import io.github.erenkng.vhscam.gl.OsdContent
import io.github.erenkng.vhscam.gl.StillRenderer
import io.github.erenkng.vhscam.media.MediaRepository
import io.github.erenkng.vhscam.media.OriginalsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
    private val originals: OriginalsStore,
) {

    /**
     * Develops [photo] with a look and saves it. With [keepOriginal] the camera's own JPEG is
     * kept too, so the look can be changed later (see [OriginalsStore]).
     */
    suspend fun develop(
        photo: CapturedPhoto,
        look: Look,
        spec: LookSpec,
        osd: OsdContent,
        mirror: Boolean,
        lookAspect: Float,
        capturedAt: LocalDateTime,
        capturedAtMillis: Long,
        keepOriginal: Boolean = false,
    ): Uri {
        val file = print(photo, look, spec, osd, mirror, lookAspect, capturedAt)
        val uri = try {
            media.savePhoto(file, FileNames.photo(capturedAt, look), capturedAtMillis)
        } finally {
            file.delete()
        }
        if (keepOriginal) {
            try {
                originals.save(ContentUris.parseId(uri), photo, mirror, capturedAtMillis)
            } catch (e: Exception) {
                Log.w(TAG, "Could not keep the original", e)
            }
        }
        return uri
    }

    /**
     * Develops [photo] with a look into a finished JPEG in the cache, frame and EXIF included,
     * without saving it anywhere. The caller deletes the file. The picture is [longEdge]
     * pixels on its long edge, the look's own size unless asked for smaller.
     */
    suspend fun print(
        photo: CapturedPhoto,
        look: Look,
        spec: LookSpec,
        osd: OsdContent,
        mirror: Boolean,
        lookAspect: Float,
        capturedAt: LocalDateTime,
        longEdge: Int = look.stillLongEdge,
    ): File {
        val upright = withContext(Dispatchers.Default) {
            decodeUpright(photo, neededLongEdge = max(longEdge, 1280))
        }
        val rendered = stillRenderer.render(
            source = upright,
            spec = spec,
            osdContent = osd,
            mirror = mirror,
            lookAspect = lookAspect,
            outputLongEdge = longEdge,
            seedFrame = Random.nextInt(1, 1_000_000),
        )
        upright.recycle()

        return withContext(Dispatchers.IO) {
            val finished = framed(rendered, spec)
            val quality = when (spec) {
                is LookSpec.Ccd -> spec.params.jpegQuality
                // Keep the pixel edges sharp.
                is LookSpec.GameBoy -> 96
                else -> JPEG_QUALITY
            }
            val out = File(context.cacheDir, "develop_${System.nanoTime()}.jpg")
            out.outputStream().use { finished.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            if (finished !== rendered) finished.recycle()
            rendered.recycle()
            writeExif(out, capturedAt)
            out
        }
    }

    /**
     * The first shot of a double exposure, small and upright, to lay over the viewfinder while
     * the second one is framed. [mirror] it for the front camera, whose preview is mirrored.
     */
    suspend fun ghost(photo: CapturedPhoto, mirror: Boolean): Bitmap = withContext(Dispatchers.Default) {
        val upright = decodeUpright(photo, neededLongEdge = GHOST_EDGE)
        if (mirror) mirrored(upright) else upright
    }

    /**
     * Two shots on one frame, the way film takes a double exposure: the light of both adds up
     * (a screen blend, each a little under). The result is an upright capture that develops
     * like any other, at least [longEdge] pixels on its long edge.
     */
    suspend fun blend(
        first: CapturedPhoto,
        second: CapturedPhoto,
        mirrorFirst: Boolean,
        mirrorSecond: Boolean,
        longEdge: Int,
    ): CapturedPhoto = withContext(Dispatchers.Default) {
        val edge = max(longEdge, 1280)
        val a = decodeUpright(first, edge).let { if (mirrorFirst) mirrored(it) else it }
        val b = decodeUpright(second, edge).let { if (mirrorSecond) mirrored(it) else it }
        val blended = screenBlend(a, b)
        a.recycle()
        b.recycle()
        val jpeg = ByteArrayOutputStream().use { out ->
            blended.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.toByteArray()
        }
        blended.recycle()
        CapturedPhoto(jpeg, bitmap = null, rotationDegrees = 0, front = false)
    }

    /** [b] laid over [a] with a screen blend, both a little darker so the result isn't washed out. */
    private fun screenBlend(a: Bitmap, b: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(a.width, a.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val under = ColorMatrixColorFilter(ColorMatrix().apply { setScale(EXPOSURE, EXPOSURE, EXPOSURE, 1f) })
        canvas.drawBitmap(a, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = under })
        // The second shot fills the first's frame, centred, even if the phone was turned between them.
        val scale = max(a.width.toFloat() / b.width, a.height.toFloat() / b.height)
        val fit = Matrix().apply {
            postScale(scale, scale)
            postTranslate((a.width - b.width * scale) / 2f, (a.height - b.height * scale) / 2f)
        }
        canvas.drawBitmap(
            b,
            fit,
            Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = under
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            },
        )
        return out
    }

    private fun mirrored(bitmap: Bitmap): Bitmap {
        val flipped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postScale(-1f, 1f) }, true)
        if (flipped !== bitmap) bitmap.recycle()
        return flipped
    }

    /**
     * Renders a look onto [upright] the way [develop] would, frame included, without saving.
     * [upright] is left as it is, ready for the next preview.
     */
    suspend fun preview(
        upright: Bitmap,
        spec: LookSpec,
        osd: OsdContent,
        mirror: Boolean,
        lookAspect: Float,
        longEdge: Int,
    ): Bitmap {
        val rendered = stillRenderer.render(
            source = upright,
            spec = spec,
            osdContent = osd,
            mirror = mirror,
            lookAspect = lookAspect,
            outputLongEdge = longEdge,
            seedFrame = PREVIEW_SEED,
        )
        return withContext(Dispatchers.Default) {
            // The digicam's JPEG blocks are part of its look.
            val compressed = if (spec is LookSpec.Ccd) recompressed(rendered, spec.params.jpegQuality) else rendered
            framed(compressed, spec).also { if (it !== compressed) compressed.recycle() }
        }
    }

    private fun recompressed(image: Bitmap, quality: Int): Bitmap {
        val bytes = ByteArrayOutputStream().use { out ->
            image.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return image
        image.recycle()
        return decoded
    }

    /** The paper or plastic around the picture, for looks that have one. */
    private fun framed(rendered: Bitmap, spec: LookSpec): Bitmap = when (spec) {
        is LookSpec.Polaroid -> instantFrame(rendered)
        is LookSpec.GameBoy -> pixelFrame(rendered, spec.params.shades)
        else -> rendered
    }

    /** Develops photobooth shots with a look and prints them on one strip. */
    suspend fun developStrip(
        photos: List<CapturedPhoto>,
        look: Look,
        specFor: () -> LookSpec,
        osd: OsdContent,
        mirrorSelfies: Boolean,
        lookAspect: Float,
        capturedAt: LocalDateTime,
        capturedAtMillis: Long,
    ): Uri {
        // 8-bit pixels stay crisp at a whole multiple of 128.
        val frameEdge = if (look == Look.GAMEBOY) 512 else STRIP_FRAME_EDGE
        val frames = photos.map { photo ->
            val upright = withContext(Dispatchers.Default) { decodeUpright(photo, neededLongEdge = frameEdge) }
            val rendered = stillRenderer.render(
                source = upright,
                spec = specFor(),
                osdContent = osd,
                mirror = photo.front && mirrorSelfies,
                lookAspect = lookAspect,
                outputLongEdge = frameEdge,
                seedFrame = Random.nextInt(1, 1_000_000),
            )
            upright.recycle()
            rendered
        }
        val file = withContext(Dispatchers.IO) {
            val strip = photoboothStrip(frames, capturedAt)
            frames.forEach { it.recycle() }
            val out = File(context.cacheDir, "strip_${System.nanoTime()}.jpg")
            out.outputStream().use { strip.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            strip.recycle()
            writeExif(out, capturedAt)
            out
        }
        return try {
            media.savePhoto(file, FileNames.strip(capturedAt), capturedAtMillis)
        } finally {
            file.delete()
        }
    }

    /** Frames stacked on photo paper, with the date printed underneath like a booth strip. */
    private fun photoboothStrip(frames: List<Bitmap>, capturedAt: LocalDateTime): Bitmap {
        val frameWidth = frames.maxOf { it.width }
        val margin = frameWidth * 0.07f
        val gap = frameWidth * 0.045f
        val footer = frameWidth * 0.2f
        val height = margin + frames.sumOf { it.height } + gap * (frames.size - 1) + footer
        val strip = Bitmap.createBitmap((frameWidth + 2 * margin).toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(strip)
        canvas.drawColor(PAPER_COLOR)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        var y = margin
        frames.forEach { frame ->
            canvas.drawBitmap(frame, margin + (frameWidth - frame.width) / 2f, y, paint)
            y += frame.height + gap
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, R.font.vt323) ?: Typeface.MONOSPACE
            textSize = footer * 0.36f
            color = INK_COLOR
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("VHS CAM  ${capturedAt.format(STRIP_DATE)}", strip.width / 2f, strip.height - footer * 0.4f, text)
        return strip
    }

    /**
     * Decodes [photo] upright, at least [neededLongEdge] pixels on its long edge where possible.
     * The bitmap is the caller's to recycle; a capture's own bitmap is left alone.
     */
    fun decodeUpright(photo: CapturedPhoto, neededLongEdge: Int): Bitmap {
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
        val ours = decoded !== photo.bitmap
        val longEdge = max(decoded.width, decoded.height)
        val scale = if (longEdge > neededLongEdge * 2) neededLongEdge * 2f / longEdge else 1f
        if (photo.rotationDegrees == 0 && scale == 1f && decoded.config == Bitmap.Config.ARGB_8888) {
            return if (ours) decoded else decoded.copy(Bitmap.Config.ARGB_8888, false)
        }
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(photo.rotationDegrees.toFloat())
        }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded && ours) decoded.recycle()
        return if (rotated.config == Bitmap.Config.ARGB_8888 && (rotated !== decoded || ours)) {
            rotated
        } else {
            rotated.copy(Bitmap.Config.ARGB_8888, false).also { if (rotated !== photo.bitmap) rotated.recycle() }
        }
    }

    /**
     * The 8-bit camera's plain frame: a light 16 px border with a dark line around the
     * 128 x 112 picture (160 x 144 in all), at the picture's magnification.
     */
    private fun pixelFrame(image: Bitmap, shades: List<Float>): Bitmap {
        fun shade(i: Int) = Color.rgb(
            (shades[i * 3] * 255).toInt(), (shades[i * 3 + 1] * 255).toInt(), (shades[i * 3 + 2] * 255).toInt(),
        )
        val pixel = minOf(image.width, image.height) / 112f
        val border = 16 * pixel
        val framed = Bitmap.createBitmap(
            (image.width + 2 * border).toInt(), (image.height + 2 * border).toInt(), Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(framed)
        canvas.drawColor(shade(3))
        val line = Paint().apply { color = shade(0) }
        canvas.drawRect(border - pixel, border - pixel, border + image.width + pixel, border + image.height + pixel, line)
        canvas.drawBitmap(image, border, border, null)
        return framed
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
        const val PAPER_COLOR = 0xFFF4F1EA.toInt()
        const val INK_COLOR = 0xFF3B3836.toInt()
        const val STRIP_FRAME_EDGE = 900
        const val GHOST_EDGE = 720

        /** Each exposure of a double exposure gets this much of its light. */
        const val EXPOSURE = 0.82f

        /** Previews use the same grain every time, so only the changes the user makes show. */
        const val PREVIEW_SEED = 4_242
        val STRIP_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
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
        look: Look,
        speed: TapeSpeed,
        capturedAt: LocalDateTime,
        capturedAtMillis: Long,
        onProgress: (Float) -> Unit,
    ): Uri {
        val name = FileNames.video(capturedAt, look)
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
