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

/** Super 8 home movie. Distances are in pixels of a 540 line frame. */
data class Super8Params(
    val saturation: Float = 1.18f,
    val contrast: Float = 1.14f,
    /** Warm reversal film: reds and yellows glow, blues stay deep. */
    val tintR: Float = 1.07f,
    val tintG: Float = 0.99f,
    val tintB: Float = 0.86f,
    /** Brownish blacks of an old projection print. */
    val fade: Float = 0.35f,
    val grain: Float = 0.11f,
    val grainSize: Float = 1.7f,
    /** Exposure changing from frame to frame. */
    val flicker: Float = 0.6f,
    /** The picture drifting and jumping in the gate. */
    val weave: Float = 0.6f,
    val dust: Float = 0.6f,
    val scratches: Float = 0.5f,
    val softness: Float = 0.8f,
    val vignette: Float = 0.5f,
    /** How much of the projector gate's dark rounded edge shows, 0..1. */
    val gate: Float = 1f,
)

/** Single-use camera with a flash. Distances are in pixels of a 1200 px high frame. */
data class DisposableParams(
    /** Flash fall-off: a bright middle and darker surroundings. */
    val flash: Float = 0.6f,
    val saturation: Float = 1.3f,
    val contrast: Float = 1.1f,
    val shadowTintR: Float = 0.9f,
    val shadowTintG: Float = 1.03f,
    val shadowTintB: Float = 1.0f,
    val highlightTintR: Float = 1.06f,
    val highlightTintG: Float = 1.0f,
    val highlightTintB: Float = 0.9f,
    val grain: Float = 0.1f,
    val grainSize: Float = 1.8f,
    /** Plastic lens softness towards the corners. */
    val softness: Float = 0.7f,
    /** Colour fringes towards the corners. */
    val fringe: Float = 1.2f,
    val vignette: Float = 0.5f,
)

/** Late-90s MiniDV camcorder with a fisheye. Distances are in pixels of a 480 line frame. */
data class MiniDvParams(
    /** How much the fisheye bulges the picture. */
    val fisheye: Float = 0.6f,
    /** The lens hood rounding off the corners. */
    val hood: Float = 0.6f,
    /** How far interlaced fields are out where they comb. */
    val comb: Float = 1.2f,
    val sharpen: Float = 0.6f,
    val saturation: Float = 1.15f,
    val contrast: Float = 1.1f,
    val noise: Float = 0.03f,
    val vignette: Float = 0.25f,
)

/** A camcorder's infrared night mode. Distances are in pixels of a 480 line frame. */
data class NightShotParams(
    val gain: Float = 1.5f,
    /** Bloom around lights. */
    val glow: Float = 0.6f,
    /** The picture's faint echo beside it, and trails in video. */
    val ghost: Float = 0.35f,
    val noise: Float = 0.14f,
    /** How much the infrared lamp lights the middle more than the edges. */
    val spot: Float = 0.5f,
    val scanlines: Float = 0.15f,
)

/** A picture on a curved CRT television. */
data class CrtParams(
    val curvature: Float = 0.6f,
    /** The red, green and blue phosphor stripes. */
    val mask: Float = 0.6f,
    val scanlines: Float = 0.5f,
    val flicker: Float = 0.4f,
    val glow: Float = 0.5f,
    val saturation: Float = 1.1f,
    /** Light from the room on the glass. */
    val glare: Float = 0.35f,
    val vignette: Float = 0.35f,
)

/** 2004 camera phone filming 3GP. */
data class FlipPhoneParams(
    /** 16 x 16 macroblocks of a codec working hard. */
    val blockiness: Float = 0.7f,
    /** Purple and green sensor noise. */
    val noise: Float = 0.14f,
    val saturation: Float = 0.85f,
    val contrast: Float = 1.25f,
    val tintR: Float = 1.04f,
    val tintG: Float = 1.02f,
    val tintB: Float = 0.86f,
    val sharpen: Float = 0.5f,
)

/** Camera that prints on till roll. */
data class ThermalParams(
    val contrast: Float = 1.35f,
    val brightness: Float = 0.06f,
    /** 0 a hard threshold .. 1 a full ordered dither. */
    val dither: Float = 1f,
    /** Paper fibres and streaks from the print head. */
    val paper: Float = 0.6f,
    /** Grey rather than black print. */
    val fade: Float = 0.12f,
)

/** Late-90s floppy-disk camera with a 640 x 480 CCD. */
data class MavicaParams(
    val saturation: Float = 0.95f,
    val contrast: Float = 1.2f,
    /** Where highlights start shooting up to white: a narrow dynamic range. */
    val highlightKnee: Float = 0.74f,
    /** Vertical smear and bloom from bright lights on the CCD. */
    val smear: Float = 0.6f,
    val sharpen: Float = 0.5f,
    val noise: Float = 0.035f,
    val tintR: Float = 0.96f,
    val tintG: Float = 1.0f,
    val tintB: Float = 1.06f,
    /** JPEG quality used to bake in block artefacts when saving. */
    val jpegQuality: Int = 55,
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

    fun super8(intensity: Float = 0.5f): Super8Params {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = Super8Params()
        return d.copy(
            saturation = 1f + (d.saturation - 1f) * k,
            contrast = 1f + (d.contrast - 1f) * k,
            tintR = 1f + (d.tintR - 1f) * k,
            tintG = 1f + (d.tintG - 1f) * k,
            tintB = 1f + (d.tintB - 1f) * k,
            fade = d.fade * k,
            grain = d.grain * k,
            flicker = d.flicker * k,
            weave = d.weave * k,
            dust = (d.dust * k).coerceAtMost(1f),
            scratches = (d.scratches * k).coerceAtMost(1f),
            softness = d.softness * k,
            vignette = d.vignette * k,
            gate = (d.gate * k).coerceAtMost(1f),
        )
    }

    fun disposable(intensity: Float = 0.5f): DisposableParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = DisposableParams()
        return d.copy(
            flash = d.flash * k,
            saturation = 1f + (d.saturation - 1f) * k,
            contrast = 1f + (d.contrast - 1f) * k,
            shadowTintR = 1f + (d.shadowTintR - 1f) * k,
            shadowTintG = 1f + (d.shadowTintG - 1f) * k,
            shadowTintB = 1f + (d.shadowTintB - 1f) * k,
            highlightTintR = 1f + (d.highlightTintR - 1f) * k,
            highlightTintG = 1f + (d.highlightTintG - 1f) * k,
            highlightTintB = 1f + (d.highlightTintB - 1f) * k,
            grain = d.grain * k,
            softness = d.softness * k,
            fringe = d.fringe * k,
            vignette = d.vignette * k,
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

    fun miniDv(intensity: Float = 0.5f): MiniDvParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = MiniDvParams()
        return d.copy(
            fisheye = d.fisheye * k,
            hood = (d.hood * k).coerceAtMost(1f),
            comb = d.comb * k,
            sharpen = d.sharpen * k,
            saturation = 1f + (d.saturation - 1f) * k,
            contrast = 1f + (d.contrast - 1f) * k,
            noise = d.noise * k,
            vignette = d.vignette * k,
        )
    }

    /** Night mode stays green at any strength; the strength is how much the camera struggles. */
    fun nightShot(intensity: Float = 0.5f): NightShotParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = NightShotParams()
        return d.copy(
            glow = d.glow * k,
            ghost = d.ghost * k,
            noise = d.noise * k,
            spot = (d.spot * k).coerceAtMost(1f),
            scanlines = d.scanlines * k,
        )
    }

    fun crt(intensity: Float = 0.5f): CrtParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = CrtParams()
        return d.copy(
            curvature = d.curvature * k,
            mask = (d.mask * k).coerceAtMost(1f),
            scanlines = (d.scanlines * k).coerceAtMost(1f),
            flicker = d.flicker * k,
            glow = d.glow * k,
            saturation = 1f + (d.saturation - 1f) * k,
            glare = d.glare * k,
            vignette = d.vignette * k,
        )
    }

    fun flipPhone(intensity: Float = 0.5f): FlipPhoneParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = FlipPhoneParams()
        return d.copy(
            blockiness = (d.blockiness * k).coerceAtMost(1f),
            noise = d.noise * k,
            saturation = 1f + (d.saturation - 1f) * k,
            contrast = 1f + (d.contrast - 1f) * k,
            tintR = 1f + (d.tintR - 1f) * k,
            tintG = 1f + (d.tintG - 1f) * k,
            tintB = 1f + (d.tintB - 1f) * k,
            sharpen = d.sharpen * k,
        )
    }

    /** Always printed in dots; the strength is contrast, paper and how faded the print is. */
    fun thermal(intensity: Float = 0.5f): ThermalParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = ThermalParams()
        return d.copy(
            contrast = 1f + (d.contrast - 1f) * k,
            brightness = d.brightness * k,
            paper = d.paper * k,
            fade = d.fade * k,
        )
    }

    fun mavica(intensity: Float = 0.5f): MavicaParams {
        val k = 2f * intensity.coerceIn(0f, 1f)
        val d = MavicaParams()
        return d.copy(
            saturation = 1f + (d.saturation - 1f) * k,
            contrast = 1f + (d.contrast - 1f) * k,
            highlightKnee = 1f - (1f - d.highlightKnee) * k,
            smear = d.smear * k,
            sharpen = d.sharpen * k,
            noise = d.noise * k,
            tintR = 1f + (d.tintR - 1f) * k,
            tintG = 1f + (d.tintG - 1f) * k,
            tintB = 1f + (d.tintB - 1f) * k,
            jpegQuality = (90 - (90 - d.jpegQuality) * k).toInt().coerceIn(20, 95),
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
