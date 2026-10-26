package io.github.erenkng.vhscam.gl

import android.opengl.GLES11Ext
import android.opengl.GLES30
import io.github.erenkng.vhscam.core.geometry.Affine2
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Runs the GLSL passes of a look. The pass graph mirrors tools/shader-lab/lab.js:
 *
 * - input: camera/photo -> upright working frame (+ burned-in OSD)
 * - VHS: chroma (quarter width) -> tape playback
 * - CCD: single pass
 * - film / instant film: glow (quarter size) -> look pass
 * - present: working frame -> output surface (+ crisp OSD, vignette, scanlines, peaking)
 *
 * Must be created and used on a thread with a current GLES 3 context.
 */
class LookRenderer(shaders: ShaderLibrary, oesInput: Boolean) {

    private val inputTarget = if (oesInput) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES30.GL_TEXTURE_2D
    private val input = shaders.program("input.frag", setOf(if (oesInput) "INPUT_OES" else "INPUT_2D"))
    private val vhsChroma = shaders.program("vhs_chroma.frag")
    private val vhsTape = shaders.program("vhs_tape.frag")
    private val ccd = shaders.program("ccd.frag")
    private val glow = shaders.program("glow.frag")
    private val film = shaders.program("film.frag")
    private val polaroid = shaders.program("polaroid.frag")
    private val present = shaders.program("present.frag")

    private val working = TargetSlot()
    private val chroma = TargetSlot()
    private val glowTarget = TargetSlot()
    private val result = TargetSlot()

    private val vertexArray: Int
    private val vertexBuffer: Int
    private val blankTexture: Int
    private val matrix = FloatArray(16)
    private val identity = Affine2.IDENTITY.toGlMatrix()

    init {
        val ids = IntArray(1)
        GLES30.glGenVertexArrays(1, ids, 0)
        vertexArray = ids[0]
        GLES30.glBindVertexArray(vertexArray)
        GLES30.glGenBuffers(1, ids, 0)
        vertexBuffer = ids[0]
        val quad = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val data = ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(quad).also { it.position(0) }
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffer)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quad.size * 4, data, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)

        GLES30.glGenTextures(1, ids, 0)
        blankTexture = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, blankTexture)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, 1, 1, 0, GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE, ByteBuffer.allocateDirect(4),
        )
        RenderTarget.setLinearClamp(GLES30.GL_TEXTURE_2D)
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
    }

    /** The processed frame of the last [process] call, in upright space. */
    val resultTarget: RenderTarget? get() = result.peek()

    /**
     * Renders [spec] from [inputTexture] into the working targets.
     *
     * @param uprightToTexture maps upright uv to input texture coordinates.
     */
    fun process(
        inputTexture: Int,
        uprightToTexture: Affine2,
        workWidth: Int,
        workHeight: Int,
        spec: LookSpec,
        time: Float,
        frame: Int,
        osd: OsdLayers,
        osdMirror: Boolean,
    ) {
        GLES30.glBindVertexArray(vertexArray)
        val w = workWidth.coerceAtLeast(4)
        val h = workHeight.coerceAtLeast(4)
        val a = working.get(w, h)
        val out = result.get(w, h)

        a.bind()
        input.use()
        input.setMatrix("uUvTransform", uprightToTexture.toGlMatrix(matrix))
        input.set("uPixelX", uprightToTexture.a / w, uprightToTexture.b / w)
        input.set("uPixelY", uprightToTexture.c / h, uprightToTexture.d / h)
        input.set("uOsdEnabled", if (osd.hasBurned) 1f else 0f)
        input.set("uOsdMirror", if (osdMirror) 1f else 0f)
        input.setTexture("uInput", 0, inputTarget, inputTexture)
        input.setTexture("uOsd", 1, GLES30.GL_TEXTURE_2D, if (osd.hasBurned) osd.burnedTexture else blankTexture)
        draw()

        when (spec) {
            is LookSpec.Vhs -> {
                val p = spec.params
                val c = chroma.get((w / 4f).roundToInt(), h)
                c.bind()
                vhsChroma.use()
                vhsChroma.setMatrix("uUvTransform", identity)
                vhsChroma.set("uSrcSize", w.toFloat(), h.toFloat())
                vhsChroma.set("uChromaBlur", p.chromaBlur)
                vhsChroma.set("uChromaShift", p.chromaShift)
                vhsChroma.setTexture("uSrc", 0, GLES30.GL_TEXTURE_2D, a.texture)
                draw()

                out.bind()
                vhsTape.use()
                vhsTape.setMatrix("uUvTransform", identity)
                vhsTape.set("uSrcSize", w.toFloat(), h.toFloat())
                vhsTape.set("uTime", time)
                vhsTape.setUnsigned("uFrame", frame)
                vhsTape.set("uLumaBlur", p.lumaBlur)
                vhsTape.set("uSharpen", p.sharpen)
                vhsTape.set("uChromaNoise", p.chromaNoise)
                vhsTape.set("uLumaNoise", p.lumaNoise)
                vhsTape.set("uJitter", p.jitter)
                vhsTape.set("uTracking", p.tracking)
                vhsTape.set("uHeadSwitch", p.headSwitch)
                vhsTape.set("uDropouts", p.dropouts)
                vhsTape.set("uSaturation", p.saturation)
                vhsTape.set("uContrast", p.contrast)
                vhsTape.set("uBlackLift", p.blackLift)
                vhsTape.set("uTint", p.tintR, p.tintG, p.tintB)
                vhsTape.set("uGlitch", spec.glitch)
                vhsTape.setTexture("uSrc", 0, GLES30.GL_TEXTURE_2D, a.texture)
                vhsTape.setTexture("uChroma", 1, GLES30.GL_TEXTURE_2D, c.texture)
                draw()
            }
            is LookSpec.Ccd -> {
                val p = spec.params
                out.bind()
                ccd.use()
                ccd.setMatrix("uUvTransform", identity)
                ccd.set("uSrcSize", w.toFloat(), h.toFloat())
                ccd.set("uScale", minOf(w, h) / 1200f)
                ccd.setUnsigned("uFrame", frame)
                ccd.set("uSaturation", p.saturation)
                ccd.set("uContrast", p.contrast)
                ccd.set("uKnee", p.highlightKnee)
                ccd.set("uFringe", p.fringe)
                ccd.set("uSharpen", p.sharpen)
                ccd.set("uNoise", p.noise)
                ccd.set("uTint", p.tintR, p.tintG, p.tintB)
                ccd.set("uVignette", p.vignette)
                ccd.setTexture("uSrc", 0, GLES30.GL_TEXTURE_2D, a.texture)
                draw()
            }
            is LookSpec.Film -> {
                val p = spec.params
                val g = renderGlow(a, w, h, threshold = 0.72f, radius = minOf(w, h) * 0.02f)
                out.bind()
                film.use()
                film.setMatrix("uUvTransform", identity)
                film.set("uSrcSize", w.toFloat(), h.toFloat())
                film.set("uScale", minOf(w, h) / 1600f)
                film.setUnsigned("uFrame", frame)
                film.set("uSaturation", p.saturation)
                film.set("uContrast", p.contrast)
                film.set("uFade", p.fade)
                film.set("uWarm", p.warmR, p.warmG, p.warmB)
                film.set("uGrain", p.grain)
                film.set("uGrainSize", p.grainSize)
                film.set("uHalation", p.halation)
                film.set("uLeak", p.lightLeak)
                film.set("uLeakSeed", spec.leakSeed)
                film.set("uVignette", p.vignette)
                film.set("uCornerSoftness", p.cornerSoftness)
                film.setTexture("uSrc", 0, GLES30.GL_TEXTURE_2D, a.texture)
                film.setTexture("uGlow", 1, GLES30.GL_TEXTURE_2D, g.texture)
                draw()
            }
            is LookSpec.Polaroid -> {
                val p = spec.params
                val g = renderGlow(a, w, h, threshold = 0.6f, radius = minOf(w, h) * 0.03f)
                out.bind()
                polaroid.use()
                polaroid.setMatrix("uUvTransform", identity)
                polaroid.set("uSrcSize", w.toFloat(), h.toFloat())
                polaroid.set("uScale", minOf(w, h) / 1600f)
                polaroid.set("uSeed", spec.seed)
                polaroid.set("uSaturation", p.saturation)
                polaroid.set("uContrast", p.contrast)
                polaroid.set("uFade", p.fade)
                polaroid.set("uTint", p.tintR, p.tintG, p.tintB)
                polaroid.set("uSoftness", p.softness)
                polaroid.set("uBloom", p.bloom)
                polaroid.set("uChemistry", p.chemistry)
                polaroid.set("uVignette", p.vignette)
                polaroid.setTexture("uSrc", 0, GLES30.GL_TEXTURE_2D, a.texture)
                polaroid.setTexture("uGlow", 1, GLES30.GL_TEXTURE_2D, g.texture)
                draw()
            }
        }
    }

    private fun renderGlow(source: RenderTarget, w: Int, h: Int, threshold: Float, radius: Float): RenderTarget {
        val g = glowTarget.get((w / 4f).roundToInt(), (h / 4f).roundToInt())
        g.bind()
        glow.use()
        glow.setMatrix("uUvTransform", identity)
        glow.set("uSrcSize", w.toFloat(), h.toFloat())
        glow.set("uThreshold", threshold)
        glow.set("uRadius", radius)
        glow.setTexture("uSrc", 0, GLES30.GL_TEXTURE_2D, source.texture)
        draw()
        return g
    }

    /**
     * Draws the processed frame onto the current surface ([target] == null) or into [target].
     *
     * @param outputToUpright maps output uv to upright uv; uv outside the frame comes out black.
     * @param peaking 0..1 strength of the focus-peaking highlight (preview only).
     */
    fun present(
        outputToUpright: Affine2,
        width: Int,
        height: Int,
        spec: LookSpec,
        osd: OsdLayers,
        osdMirror: Boolean,
        peaking: Float,
        target: RenderTarget? = null,
    ) {
        val out = result.peek() ?: return
        val clean = working.peek() ?: return
        GLES30.glBindVertexArray(vertexArray)
        if (target != null) {
            target.bind()
        } else {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, width, height)
        }
        val vhs = spec as? LookSpec.Vhs
        present.use()
        present.setMatrix("uUvTransform", outputToUpright.toGlMatrix(matrix))
        present.set("uSrcSize", out.width.toFloat(), out.height.toFloat())
        present.set("uOverlayEnabled", if (osd.hasOverlay) 1f else 0f)
        present.set("uOverlayMirror", if (osdMirror) 1f else 0f)
        present.set("uVignette", vhs?.params?.vignette ?: 0f)
        present.set("uScanlines", vhs?.params?.scanlines ?: 0f)
        present.set("uPeaking", peaking)
        present.set("uPeakColor", 1f, 0.25f, 0.65f)
        present.setTexture("uSrc", 0, GLES30.GL_TEXTURE_2D, out.texture)
        present.setTexture("uOverlay", 1, GLES30.GL_TEXTURE_2D, if (osd.hasOverlay) osd.overlayTexture else blankTexture)
        present.setTexture("uPeakSrc", 2, GLES30.GL_TEXTURE_2D, clean.texture)
        draw()
    }

    private fun draw() {
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }

    fun release() {
        listOf(input, vhsChroma, vhsTape, ccd, glow, film, polaroid, present).forEach { it.release() }
        listOf(working, chroma, glowTarget, result).forEach { it.release() }
        GLES30.glDeleteBuffers(1, intArrayOf(vertexBuffer), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vertexArray), 0)
        GLES30.glDeleteTextures(1, intArrayOf(blankTexture), 0)
    }
}
