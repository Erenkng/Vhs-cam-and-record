package io.github.erenkng.vhscam.core.look

/** Early-2000s CCD point-and-shoot. Distances are in pixels of a 1600 px wide frame. */
data class CcdParams(
    val saturation: Float = 1.22f,
    val contrast: Float = 1.1f,
    /** Where highlights start to clip hard. */
    val highlightKnee: Float = 0.82f,
    /** Purple fringing / lateral chromatic aberration near high-contrast edges. */
    val fringe: Float = 1.6f,
    val sharpen: Float = 0.7f,
    /** Colour noise that lives mostly in the shadows. */
    val noise: Float = 0.05f,
    val tintR: Float = 0.97f,
    val tintG: Float = 1.0f,
    val tintB: Float = 1.06f,
    val vignette: Float = 0.12f,
    /** JPEG quality used to bake in block artefacts when saving. */
    val jpegQuality: Int = 42,
)

/** Disposable 35 mm film camera. Distances are in pixels of a 2400 px wide frame. */
data class FilmParams(
    val saturation: Float = 0.92f,
    val contrast: Float = 0.96f,
    /** Faded blacks. */
    val fade: Float = 0.07f,
    val warmR: Float = 1.06f,
    val warmG: Float = 1.0f,
    val warmB: Float = 0.88f,
    val grain: Float = 0.085f,
    val grainSize: Float = 1.6f,
    /** Red glow that bleeds out of highlights. */
    val halation: Float = 0.38f,
    /** Light leak strength; the leak itself is randomised per shot. */
    val lightLeak: Float = 0.55f,
    val vignette: Float = 0.38f,
    /** Lens softness towards the corners. */
    val cornerSoftness: Float = 0.5f,
)

/** Instant film. Distances are in pixels of a 1600 px wide frame. */
data class PolaroidParams(
    val saturation: Float = 0.74f,
    val contrast: Float = 0.84f,
    val fade: Float = 0.12f,
    val tintR: Float = 1.02f,
    val tintG: Float = 1.03f,
    val tintB: Float = 0.94f,
    val softness: Float = 1.4f,
    val bloom: Float = 0.25f,
    /** Uneven chemistry near the borders. */
    val chemistry: Float = 0.4f,
    val vignette: Float = 0.32f,
)

/** Scales a look between a gentle (0) and a heavy (1) version; 0.5 is the default design. */
object PhotoLookPresets {

    fun ccd(intensity: Float = 0.5f): CcdParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = CcdParams()
        return d.copy(
            saturation = 1f + (d.saturation - 1f) * k,
            contrast = 1f + (d.contrast - 1f) * k,
            fringe = d.fringe * k,
            sharpen = d.sharpen * k,
            noise = d.noise * k,
            vignette = d.vignette * k,
            jpegQuality = (90 - (90 - d.jpegQuality) * k).toInt().coerceIn(20, 95),
        )
    }

    fun film(intensity: Float = 0.5f): FilmParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = FilmParams()
        return d.copy(
            fade = d.fade * k,
            grain = d.grain * k,
            halation = d.halation * k,
            lightLeak = d.lightLeak * k,
            vignette = d.vignette * k,
            cornerSoftness = d.cornerSoftness * k,
        )
    }

    fun polaroid(intensity: Float = 0.5f): PolaroidParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = PolaroidParams()
        return d.copy(
            saturation = 1f + (d.saturation - 1f) * k,
            contrast = 1f + (d.contrast - 1f) * k,
            fade = d.fade * k,
            softness = d.softness * k,
            bloom = d.bloom * k,
            chemistry = d.chemistry * k,
            vignette = d.vignette * k,
        )
    }
}
