package io.github.erenkng.vhscam.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import io.github.erenkng.vhscam.core.geometry.Affine2
import io.github.erenkng.vhscam.core.geometry.OrientationMath
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Renders a look onto a photo off-screen, with the same passes as the live preview.
 * Runs on its own GL thread; safe to call from any coroutine.
 */
class StillRenderer(private val context: Context) {

    private val dispatcher = Executors.newSingleThreadExecutor { r -> Thread(r, "VhsStill") }
        .asCoroutineDispatcher()

    private var egl: EglCore? = null
    private var renderer: LookRenderer? = null
    private var osd: OsdLayers? = null

    /**
     * @param source upright photo.
     * @param mirror flip horizontally (selfie mirroring).
     * @param lookAspect landscape aspect of the look's crop, or null to keep the photo's.
     * @param workLines working resolution: lines of the short (VHS: vertical) edge.
     * @param outputLongEdge long edge of the returned bitmap.
     */
    suspend fun render(
        source: Bitmap,
        spec: LookSpec,
        osdContent: OsdContent,
        mirror: Boolean,
        lookAspect: Float?,
        outputLongEdge: Int,
        seedFrame: Int,
    ): Bitmap = withContext(dispatcher) {
        val core = egl ?: EglCore().also { egl = it }
        core.makeDefaultCurrent()
        val looks = renderer ?: LookRenderer(ShaderLibrary(context.assets), oesInput = false).also { renderer = it }
        val layers = osd ?: OsdLayers(OsdPainter(context)).also { osd = it }

        val sourceAspect = source.width.toFloat() / source.height
        // Bitmap rows start at the top, GL textures at the bottom.
        var uprightToTexture = FLIP_Y
        if (mirror) uprightToTexture = uprightToTexture * OrientationMath.MIRROR_U
        var aspect = sourceAspect
        if (lookAspect != null) {
            val target = if (sourceAspect < 1f) 1f / lookAspect else lookAspect
            VhsSurfaceProcessor.centreCrop(sourceAspect, target)?.let { uprightToTexture = uprightToTexture * it }
            aspect = target
        }

        val (workWidth, workHeight) = if (spec is LookSpec.Vhs) {
            max(4, (VhsSurfaceProcessor.VHS_LINES * aspect).roundToInt()) to VhsSurfaceProcessor.VHS_LINES
        } else if (aspect >= 1f) {
            outputLongEdge to max(4, (outputLongEdge / aspect).roundToInt())
        } else {
            max(4, (outputLongEdge * aspect).roundToInt()) to outputLongEdge
        }
        val (outWidth, outHeight) = if (aspect >= 1f) {
            outputLongEdge to max(4, (outputLongEdge / aspect).roundToInt())
        } else {
            max(4, (outputLongEdge * aspect).roundToInt()) to outputLongEdge
        }

        val texture = IntArray(1).also { GLES30.glGenTextures(1, it, 0) }[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        RenderTarget.setLinearClamp(GLES30.GL_TEXTURE_2D)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, source, 0)

        layers.update(spec.kind, osdContent, workWidth, workHeight, outWidth, outHeight)
        looks.process(
            inputTexture = texture,
            uprightToTexture = uprightToTexture,
            workWidth = workWidth,
            workHeight = workHeight,
            spec = spec,
            time = seedFrame / 30f,
            frame = seedFrame,
            osd = layers,
            osdMirror = false,
        )
        val output = RenderTarget(outWidth, outHeight)
        // Read back top row first.
        looks.present(FLIP_Y, outWidth, outHeight, spec, layers, osdMirror = false, peaking = 0f, target = output)
        val pixels = ByteBuffer.allocateDirect(outWidth * outHeight * 4).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, outWidth, outHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixels)
        output.release()
        GLES30.glDeleteTextures(1, intArrayOf(texture), 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        pixels.rewind()
        Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888).apply {
            copyPixelsFromBuffer(pixels)
        }
    }

    suspend fun release() = withContext(dispatcher) {
        renderer?.release()
        osd?.release()
        egl?.release()
        renderer = null
        osd = null
        egl = null
    }

    private companion object {
        val FLIP_Y = Affine2(d = -1f, f = 1f)
    }
}
