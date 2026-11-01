package io.github.erenkng.vhscam.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import io.github.erenkng.vhscam.core.media.FileNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Puts a tape on an old TV: the whole clip playing on a CRT set in a dark room, as a 9:16
 * video for Reels and TikTok, with its sound. The picture is drawn by assets/export/tv.frag.
 */
@OptIn(UnstableApi::class)
class TvExporter(private val context: Context, private val media: MediaRepository) {

    private val _progress = MutableStateFlow<Float?>(null)

    /** 0..1 while a TV clip is being made, null otherwise. */
    val progress: StateFlow<Float?> = _progress.asStateFlow()

    suspend fun export(video: Uri): Uri {
        _progress.value = 0f
        val file = File(context.cacheDir, "tv_${System.nanoTime()}.mp4")
        try {
            val shape = withContext(Dispatchers.IO) { shapeOf(video) }
            transform(video, shape, file)
            return media.saveVideo(file, FileNames.tv(LocalDateTime.now()), System.currentTimeMillis())
        } finally {
            file.delete()
            _progress.value = null
        }
    }

    private fun shapeOf(video: Uri): ClipShape = MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(context, video)
        fun number(key: Int) = retriever.extractMetadata(key)?.toIntOrNull()
        ClipShape(
            width = number(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: 4,
            height = number(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: 3,
            rotation = number(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: 0,
        )
    }

    private suspend fun transform(video: Uri, shape: ClipShape, output: File) = withContext(Dispatchers.Main) {
        val item = EditedMediaItem.Builder(MediaItem.fromUri(video))
            .setEffects(Effects(emptyList(), listOf(TvEffect(shape))))
            .build()
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
            transformer.start(item, output.absolutePath)
            continuation.invokeOnCancellation { transformer.cancel() }

            val holder = ProgressHolder()
            launch {
                while (continuation.isActive) {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        _progress.value = holder.progress / 100f
                    }
                    delay(PROGRESS_POLL_MILLIS)
                }
            }
        }
    }

    private companion object {
        const val PROGRESS_POLL_MILLIS = 200L
    }
}

/** A clip's stored size and the turn (clockwise degrees) it is shown upright with. */
private data class ClipShape(val width: Int, val height: Int, val rotation: Int) {
    /** Width over height as the clip is watched. */
    val aspect: Float get() = if (rotation % 180 == 0) width.toFloat() / height else height.toFloat() / width
}

/** Every frame of the clip on the TV set, as a 1080 x 1920 picture. */
@UnstableApi
private class TvEffect(private val shape: ClipShape) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram = TvShaderProgram(context, shape, useHdr)
}

@UnstableApi
private class TvShaderProgram(context: Context, private val shape: ClipShape, useHdr: Boolean) :
    BaseGlShaderProgram(useHdr, 1) {

    private val program: GlProgram = try {
        GlProgram(context, VERTEX_SHADER, FRAGMENT_SHADER)
    } catch (e: Exception) {
        throw VideoFrameProcessingException(e)
    }

    /** Clockwise degrees the frames still have to turn to stand upright. */
    private var rotation = 0f

    init {
        program.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
        )
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        // Media3 hands the frames over upright; should a decoder not, the shader turns them.
        val landscapeFrames = inputWidth >= inputHeight
        rotation = if (landscapeFrames == (shape.aspect >= 1f)) 0f else shape.rotation.toFloat()
        return Size(OUTPUT_WIDTH, OUTPUT_HEIGHT)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setFloatsUniform("uOutputSize", floatArrayOf(OUTPUT_WIDTH.toFloat(), OUTPUT_HEIGHT.toFloat()))
            program.setFloatUniform("uAspect", shape.aspect)
            program.setFloatUniform("uRotation", rotation)
            program.setFloatUniform("uTime", presentationTimeUs / 1_000_000f)
            program.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            program.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    private companion object {
        const val VERTEX_SHADER = "export/tv.vert"
        const val FRAGMENT_SHADER = "export/tv.frag"
        const val OUTPUT_WIDTH = 1080
        const val OUTPUT_HEIGHT = 1920
    }
}
