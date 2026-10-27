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

/** 8-bit handheld camera. Shades are RGB, darkest first (Game Boy LCD greens). */
data class GameBoyParams(
    val contrast: Float = 1.35f,
    val brightness: Float = 0.02f,
    /** The camera's built-in edge enhancement. */
    val edge: Float = 0.55f,
    /** Ordered dithering between the four shades. */
    val dither: Float = 1f,
    val shades: List<Float> = listOf(
        0.059f, 0.220f, 0.059f,
        0.188f, 0.384f, 0.188f,
        0.545f, 0.675f, 0.059f,
        0.608f, 0.737f, 0.059f,
    ),
)

/** Mid-2000s USB webcam. */
data class WebcamParams(
    /** Gain of an auto-exposure that never quite copes. */
    val exposure: Float = 1.3f,
    val contrast: Float = 1.2f,
    val saturation: Float = 0.6f,
    val tintR: Float = 0.88f,
    val tintG: Float = 1.02f,
    val tintB: Float = 1.16f,
    val noise: Float = 0.11f,
    /** Motion-JPEG blocks in flat areas. */
    val blockiness: Float = 0.95f,
    val vignette: Float = 0.4f,
)

/** Security camera. */
data class CctvParams(
    val contrast: Float = 1.45f,
    val noise: Float = 0.1f,
    /** Wide-angle barrel distortion. */
    val barrel: Float = 0.22f,
    /** Mains hum bar rolling through the picture. */
    val hum: Float = 0.12f,
    val tintR: Float = 0.92f,
    val tintG: Float = 1.0f,
    val tintB: Float = 0.94f,
    val vignette: Float = 0.5f,
    val scanlines: Float = 0.18f,
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

    fun gameBoy(intensity: Float = 0.5f): GameBoyParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = GameBoyParams()
        return d.copy(
            contrast = 1f + (d.contrast - 1f) * k,
            edge = d.edge * k,
            dither = (0.5f + 0.5f * k).coerceAtMost(1f),
        )
    }

    fun webcam(intensity: Float = 0.5f): WebcamParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = WebcamParams()
        return d.copy(
            exposure = 1f + (d.exposure - 1f) * k,
            contrast = 1f + (d.contrast - 1f) * k,
            saturation = 1f + (d.saturation - 1f) * k,
            tintR = 1f + (d.tintR - 1f) * k,
            tintG = 1f + (d.tintG - 1f) * k,
            tintB = 1f + (d.tintB - 1f) * k,
            noise = d.noise * k,
            blockiness = (d.blockiness * k).coerceAtMost(1f),
            vignette = d.vignette * k,
        )
    }

    fun cctv(intensity: Float = 0.5f): CctvParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = CctvParams()
        return d.copy(
            contrast = 1f + (d.contrast - 1f) * k,
            noise = d.noise * k,
            barrel = d.barrel * k,
            hum = d.hum * k,
            vignette = d.vignette * k,
            scanlines = d.scanlines * k,
        )
    }
}
