package io.github.erenkng.vhscam.core.look

/**
 * Uniform values for the VHS shader passes. Distances are in "tape pixels": the VHS frame is
 * processed at 480 lines, i.e. 640 px wide for 4:3.
 */
data class VhsParams(
    /** Sigma of the horizontal luma low-pass (VHS resolves roughly 240 TV lines). */
    val lumaBlur: Float,
    /** Edge enhancement of the playback deck; produces the bright halos next to edges. */
    val sharpen: Float,
    /** Sigma of the horizontal chroma low-pass (colour-under resolves roughly 40 lines). */
    val chromaBlur: Float,
    /** Chroma delay relative to luma, to the right. */
    val chromaShift: Float,
    /** Amplitude of the streaky colour noise. */
    val chromaNoise: Float,
    /** Amplitude of the luma "snow". */
    val lumaNoise: Float,
    /** Per-line horizontal time-base jitter. */
    val jitter: Float,
    /** 0..1 probability and strength of rolling tracking-error bands. */
    val tracking: Float,
    /** Strength of the head-switching distortion at the bottom of the frame. */
    val headSwitch: Float,
    /** Rate of white dropout streaks. */
    val dropouts: Float,
    val saturation: Float,
    val contrast: Float,
    /** Milky blacks. */
    val blackLift: Float,
    val tintR: Float,
    val tintG: Float,
    val tintB: Float,
    val vignette: Float,
    val scanlines: Float,
)

/** User controls on top of a [TapeSpeed] preset. Multipliers default to 1. */
data class VhsAdjustments(
    /** Master "tape wear", 0..1. 0.5 is the preset as designed. */
    val wear: Float = 0.5f,
    val colorBleed: Float = 1f,
    val noise: Float = 1f,
    val tracking: Float = 1f,
    val jitter: Float = 1f,
    val sharpen: Float = 1f,
    val headSwitch: Float = 1f,
    val scanlines: Float = 1f,
    val vignette: Float = 1f,
)

object VhsPresets {

    private val SP = VhsParams(
        lumaBlur = 1.1f, sharpen = 0.55f, chromaBlur = 5.0f, chromaShift = 2.0f,
        chromaNoise = 0.035f, lumaNoise = 0.035f, jitter = 0.5f, tracking = 0.12f,
        headSwitch = 0.8f, dropouts = 0.25f, saturation = 0.92f, contrast = 0.92f,
        blackLift = 0.045f, tintR = 1.0f, tintG = 0.985f, tintB = 0.97f,
        vignette = 0.25f, scanlines = 0.10f,
    )

    private val LP = VhsParams(
        lumaBlur = 1.45f, sharpen = 0.65f, chromaBlur = 6.5f, chromaShift = 2.8f,
        chromaNoise = 0.06f, lumaNoise = 0.06f, jitter = 0.85f, tracking = 0.25f,
        headSwitch = 1.0f, dropouts = 0.5f, saturation = 0.86f, contrast = 0.9f,
        blackLift = 0.06f, tintR = 1.0f, tintG = 0.98f, tintB = 0.95f,
        vignette = 0.28f, scanlines = 0.12f,
    )

    private val EP = VhsParams(
        lumaBlur = 1.9f, sharpen = 0.75f, chromaBlur = 8.5f, chromaShift = 3.8f,
        chromaNoise = 0.09f, lumaNoise = 0.09f, jitter = 1.3f, tracking = 0.45f,
        headSwitch = 1.2f, dropouts = 0.9f, saturation = 0.8f, contrast = 0.88f,
        blackLift = 0.075f, tintR = 1.0f, tintG = 0.975f, tintB = 0.93f,
        vignette = 0.3f, scanlines = 0.14f,
    )

    fun base(speed: TapeSpeed): VhsParams = when (speed) {
        TapeSpeed.SP -> SP
        TapeSpeed.LP -> LP
        TapeSpeed.EP -> EP
    }

    fun params(speed: TapeSpeed, adjustments: VhsAdjustments = VhsAdjustments()): VhsParams {
        val p = base(speed)
        val wear = adjustments.wear.coerceIn(0f, 1f)
        // Damage (noise, tracking, jitter) vanishes at zero wear; softness never fully does.
        val damage = 2f * wear
        val soft = 0.6f + 0.8f * wear
        val bleed = adjustments.colorBleed.coerceAtLeast(0f)
        val noise = adjustments.noise.coerceAtLeast(0f)
        return p.copy(
            lumaBlur = p.lumaBlur * soft,
            sharpen = p.sharpen * adjustments.sharpen.coerceAtLeast(0f),
            chromaBlur = p.chromaBlur * soft * bleed,
            chromaShift = p.chromaShift * soft * bleed,
            chromaNoise = p.chromaNoise * damage * noise,
            lumaNoise = p.lumaNoise * damage * noise,
            jitter = p.jitter * damage * adjustments.jitter.coerceAtLeast(0f),
            tracking = (p.tracking * damage * adjustments.tracking.coerceAtLeast(0f)).coerceIn(0f, 1f),
            headSwitch = p.headSwitch * adjustments.headSwitch.coerceAtLeast(0f),
            dropouts = p.dropouts * damage * noise,
            vignette = p.vignette * adjustments.vignette.coerceAtLeast(0f),
            scanlines = p.scanlines * adjustments.scanlines.coerceAtLeast(0f),
        )
    }
}
