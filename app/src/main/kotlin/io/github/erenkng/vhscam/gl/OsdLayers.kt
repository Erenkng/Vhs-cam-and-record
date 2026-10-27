package io.github.erenkng.vhscam.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.opengl.GLES30
import android.opengl.GLUtils
import io.github.erenkng.vhscam.core.look.Look

/**
 * The two OSD textures of a frame: text the camera burns into the picture (degraded with it)
 * and a crisp overlay drawn after the look. Bitmaps are re-painted only when their text or
 * size changes. Must be used on the GL thread.
 */
class OsdLayers(private val painter: OsdPainter) {

    var burnedTexture = 0
        private set
    var overlayTexture = 0
        private set
    var hasBurned = false
        private set
    var hasOverlay = false
        private set

    private var burnedKey: Any? = null
    private var overlayKey: Any? = null

    fun update(
        look: Look,
        content: OsdContent,
        burnedWidth: Int,
        burnedHeight: Int,
        overlayWidth: Int,
        overlayHeight: Int,
    ) {
        val burnedText: List<String?> = when (look) {
            Look.VHS -> listOf(content.camcorderTime, content.camcorderDate)
            Look.FILM, Look.DISPOSABLE -> listOf(content.filmStamp)
            else -> emptyList()
        }
        val newBurnedKey = listOf(look, burnedText, burnedWidth, burnedHeight)
        if (newBurnedKey != burnedKey) {
            burnedKey = newBurnedKey
            hasBurned = burnedText.any { it != null }
            if (hasBurned) {
                burnedTexture = upload(burnedTexture, burnedWidth, burnedHeight) { canvas, w, h ->
                    when (look) {
                        Look.VHS -> painter.camcorderStamp(canvas, w, h, content.camcorderTime, content.camcorderDate)
                        Look.FILM, Look.DISPOSABLE -> content.filmStamp?.let { painter.filmStamp(canvas, w, h, it) }
                        else -> Unit
                    }
                }
            }
        }

        val overlayText: List<Any?> = when (look) {
            Look.VHS -> listOf(content.playIndicator, content.speedLabel, content.counter)
            Look.CCD -> listOf(content.digitalStamp)
            Look.CCTV -> listOf(content.cctvStamp)
            else -> emptyList()
        }
        val newOverlayKey = listOf(look, overlayText, overlayWidth, overlayHeight)
        if (newOverlayKey != overlayKey) {
            overlayKey = newOverlayKey
            hasOverlay = when (look) {
                Look.VHS -> content.playIndicator || content.counter != null
                Look.CCD -> content.digitalStamp != null
                Look.CCTV -> true
                else -> false
            }
            if (hasOverlay) {
                overlayTexture = upload(overlayTexture, overlayWidth, overlayHeight) { canvas, w, h ->
                    when (look) {
                        Look.VHS -> painter.vcrOverlay(canvas, w, h, content.playIndicator, content.speedLabel, content.counter)
                        Look.CCD -> content.digitalStamp?.let { painter.digitalStamp(canvas, w, h, it) }
                        Look.CCTV -> painter.cctvOverlay(canvas, w, h, CCTV_CAMERA, content.cctvStamp)
                        else -> Unit
                    }
                }
            }
        }
    }

    private inline fun upload(texture: Int, width: Int, height: Int, paint: (Canvas, Int, Int) -> Unit): Int {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)
        // GL textures start at the bottom row, so paint upside down.
        canvas.scale(1f, -1f, 0f, h / 2f)
        paint(canvas, w, h)
        val id = if (texture != 0) texture else IntArray(1).also { GLES30.glGenTextures(1, it, 0) }[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        RenderTarget.setLinearClamp(GLES30.GL_TEXTURE_2D)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        return id
    }

    fun release() {
        val ids = intArrayOf(burnedTexture, overlayTexture).filter { it != 0 }.toIntArray()
        if (ids.isNotEmpty()) GLES30.glDeleteTextures(ids.size, ids, 0)
        burnedTexture = 0
        overlayTexture = 0
        burnedKey = null
        overlayKey = null
    }

    private companion object {
        const val CCTV_CAMERA = "CAM 01"
    }
}
