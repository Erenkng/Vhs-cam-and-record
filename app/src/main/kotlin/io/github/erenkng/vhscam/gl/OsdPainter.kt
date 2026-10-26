package io.github.erenkng.vhscam.gl

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import io.github.erenkng.vhscam.R
import kotlin.math.max
import kotlin.math.tan

/**
 * Draws the on-screen displays. Layouts match tools/shader-lab/osd.js so the lab previews
 * what the app produces. Sizes are relative to the layer, which is in upright space.
 */
class OsdPainter(context: Context) {

    private val vt323: Typeface =
        ResourcesCompat.getFont(context, R.font.vt323) ?: Typeface.MONOSPACE

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = vt323 }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = vt323
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(217, 0, 0, 0)
    }
    private val shape = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private fun outlinedText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int) {
        stroke.textSize = size
        stroke.strokeWidth = max(1f, size * 0.12f)
        fill.textSize = size
        fill.color = color
        canvas.drawText(text, x, y, stroke)
        canvas.drawText(text, x, y, fill)
    }

    /** Camcorder date/time, bottom-left. Burned in before the tape degrades the picture. */
    fun camcorderStamp(canvas: Canvas, w: Int, h: Int, time: String?, date: String?) {
        val size = h * 0.075f
        val x = w * 0.07f
        var y = h * 0.91f
        listOfNotNull(time, date).asReversed().forEach { line ->
            outlinedText(canvas, line, x, y, size, Color.WHITE)
            y -= size * 1.05f
        }
    }

    /** VCR overlay: "PLAY ▶" top-left, speed and tape counter top-right. */
    fun vcrOverlay(canvas: Canvas, w: Int, h: Int, play: Boolean, speed: String?, counter: String?) {
        val size = h * 0.07f
        val y = h * 0.13f
        if (play) {
            val x = w * 0.07f
            outlinedText(canvas, "PLAY", x, y, size, Color.WHITE)
            fill.textSize = size
            val tx = x + fill.measureText("PLAY ")
            val th = size * 0.5f
            path.reset()
            path.moveTo(tx, y - th * 1.15f)
            path.lineTo(tx + th * 0.9f, y - th * 0.65f)
            path.lineTo(tx, y - th * 0.15f)
            path.close()
            stroke.strokeWidth = max(1f, size * 0.12f)
            canvas.drawPath(path, stroke)
            shape.color = Color.WHITE
            shape.setShadowLayer(0f, 0f, 0f, 0)
            canvas.drawPath(path, shape)
        }
        if (counter != null) {
            val text = if (speed != null) "$speed  $counter" else counter
            fill.textSize = size
            val tw = fill.measureText(text)
            outlinedText(canvas, text, w * 0.93f - tw, y, size, Color.WHITE)
        }
    }

    /** Digital camera imprint, amber, bottom-right. */
    fun digitalStamp(canvas: Canvas, w: Int, h: Int, text: String) {
        val size = h * 0.055f
        fill.textSize = size
        val tw = fill.measureText(text)
        outlinedText(canvas, text, w * 0.94f - tw, h * 0.94f, size, AMBER)
    }

    /** Seven-segment LED imprint of a 35 mm data back, glowing orange, bottom-right. */
    fun filmStamp(canvas: Canvas, w: Int, h: Int, text: String) {
        val dh = h * 0.045f
        val dw = dh * 0.56f
        val t = dh * 0.13f
        val gap = dh * 0.22f
        val slant = tan(Math.toRadians(8.0)).toFloat()
        fun advance(ch: Char) = when (ch) {
            ' ' -> dw * 0.6f
            '\'' -> dh * 0.22f
            else -> dw
        } + gap
        var x = w * 0.94f - text.sumOf { advance(it).toDouble() }.toFloat()
        val base = h * 0.93f

        shape.color = FILM_ORANGE
        shape.setShadowLayer(dh * 0.35f, 0f, 0f, FILM_GLOW)

        fun poly(vararg points: Float) {
            path.reset()
            var i = 0
            while (i < points.size) {
                val px = points[i] + (base - points[i + 1]) * slant
                if (i == 0) path.moveTo(px, points[i + 1]) else path.lineTo(px, points[i + 1])
                i += 2
            }
            path.close()
            canvas.drawPath(path, shape)
        }
        fun hSeg(x0: Float, x1: Float, y: Float) = poly(
            x0, y, x0 + t / 2, y - t / 2, x1 - t / 2, y - t / 2, x1, y, x1 - t / 2, y + t / 2, x0 + t / 2, y + t / 2,
        )
        fun vSeg(xx: Float, y0: Float, y1: Float) = poly(
            xx, y0, xx + t / 2, y0 + t / 2, xx + t / 2, y1 - t / 2, xx, y1, xx - t / 2, y1 - t / 2, xx - t / 2, y0 + t / 2,
        )

        for (ch in text) {
            if (ch == '\'') {
                poly(x + t * 0.5f, base - dh, x + t * 1.4f, base - dh, x + t * 0.9f, base - dh * 0.72f, x + t * 0.2f, base - dh * 0.72f)
            } else {
                val segments = SEGMENTS[ch]
                if (segments != null) {
                    val top = base - dh
                    val mid = base - dh / 2
                    val l = x + t / 2
                    val r = x + dw - t / 2
                    for (s in segments) {
                        when (s) {
                            'a' -> hSeg(l, r, top)
                            'g' -> hSeg(l, r, mid)
                            'd' -> hSeg(l, r, base)
                            'f' -> vSeg(l, top, mid)
                            'b' -> vSeg(r, top, mid)
                            'e' -> vSeg(l, mid, base)
                            'c' -> vSeg(r, mid, base)
                        }
                    }
                }
            }
            x += advance(ch)
        }
        shape.setShadowLayer(0f, 0f, 0f, 0)
    }

    private companion object {
        val AMBER = Color.rgb(0xFF, 0xB2, 0x38)
        val FILM_ORANGE = Color.rgb(0xFF, 0x6A, 0x1A)
        val FILM_GLOW = Color.argb(230, 0xFF, 0x6E, 0x1E)
        val SEGMENTS = mapOf(
            '0' to "abcdef", '1' to "bc", '2' to "abged", '3' to "abgcd", '4' to "fgbc",
            '5' to "afgcd", '6' to "afgedc", '7' to "abc", '8' to "abcdefg", '9' to "abcdfg",
        )
    }
}
