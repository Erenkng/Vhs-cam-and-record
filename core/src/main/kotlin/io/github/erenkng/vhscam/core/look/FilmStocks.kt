package io.github.erenkng.vhscam.core.look

/**
 * A colour film stock for the film stock shader. Colours are r, g, b; [matrix] is row-major.
 * Distances are in pixels of a 1600 px high frame.
 */
data class FilmStockParams(
    /** Exposure in stops. */
    val exposure: Float = 0f,
    /** Strength of the print's S-curve; 1 leaves contrast alone. */
    val contrast: Float = 1f,
    /** Lifted blacks of a matte print. */
    val fade: Float = 0f,
    /** The stock's colour response in linear light. */
    val matrix: List<Float> = IDENTITY,
    val saturation: Float = 1f,
    val shadowTint: List<Float> = NEUTRAL,
    val highlightTint: List<Float> = NEUTRAL,
    /** Red glow around highlights, from light reflected off the film base. */
    val halation: Float = 0f,
    val halationTint: List<Float> = listOf(1f, 0.3f, 0.1f),
    val grain: Float = 0f,
    val grainSize: Float = 1.6f,
    /** 0 grey grain .. 1 grain in coloured, mostly red, dye clouds. */
    val redGrain: Float = 0f,
    val vignette: Float = 0f,
) {
    companion object {
        val IDENTITY = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val NEUTRAL = listOf(1f, 1f, 1f)
    }
}

/** The colour film looks that share the film stock shader, with their character. */
object FilmStocks {

    private val stocks: Map<Look, FilmStockParams> = mapOf(
        // Warm consumer film: golden light, rich reds, a little brown in the shadows.
        Look.GOLD to FilmStockParams(
            exposure = 0.1f, contrast = 1.12f, fade = 0.02f,
            matrix = listOf(1.06f, 0.02f, -0.02f, 0.02f, 1f, -0.02f, -0.04f, 0f, 0.86f),
            saturation = 1.18f,
            shadowTint = listOf(1f, 0.97f, 0.92f), highlightTint = listOf(1.06f, 1f, 0.86f),
            halation = 0.3f, halationTint = listOf(1f, 0.35f, 0.12f),
            grain = 0.07f, grainSize = 1.7f, redGrain = 0.55f, vignette = 0.22f,
        ),
        // Professional portrait film: soft contrast, pastel colour, kind to skin.
        Look.PORTRAIT to FilmStockParams(
            exposure = 0.15f, contrast = 0.9f, fade = 0.03f,
            matrix = listOf(1.03f, 0f, -0.01f, 0.02f, 0.97f, 0.02f, 0f, 0.04f, 0.95f),
            saturation = 0.92f,
            shadowTint = listOf(0.97f, 1f, 1.03f), highlightTint = listOf(1.04f, 1f, 0.95f),
            halation = 0.15f,
            grain = 0.045f, grainSize = 1.4f, redGrain = 0.35f, vignette = 0.12f,
        ),
        // Tungsten cinema film: teal shadows, warm light and a strong red glow around it.
        Look.CINE to FilmStockParams(
            contrast = 1.08f, fade = 0.025f,
            matrix = listOf(0.92f, 0.04f, 0f, 0f, 1f, 0.02f, 0.02f, 0.06f, 1.1f),
            saturation = 1.05f,
            shadowTint = listOf(0.88f, 1f, 1.06f), highlightTint = listOf(1.06f, 1f, 0.92f),
            halation = 0.95f, halationTint = listOf(1f, 0.18f, 0.06f),
            grain = 0.085f, grainSize = 1.9f, redGrain = 0.6f, vignette = 0.25f,
        ),
        // Faded, warm and red, with heavy grain in red dye clouds.
        Look.RED_GRAIN to FilmStockParams(
            exposure = 0.05f, contrast = 1.05f, fade = 0.07f,
            matrix = listOf(1.08f, 0.04f, 0f, 0.02f, 0.97f, 0f, 0f, 0.04f, 0.86f),
            saturation = 1.08f,
            shadowTint = listOf(1.08f, 0.94f, 0.92f), highlightTint = listOf(1.04f, 0.99f, 0.9f),
            halation = 0.4f,
            grain = 0.14f, grainSize = 2f, redGrain = 1f, vignette = 0.3f,
        ),
        // Film exposed through its base: everything in reds, oranges and yellows.
        Look.REDSCALE to FilmStockParams(
            exposure = 0.15f, contrast = 1.15f, fade = 0.02f,
            matrix = listOf(0.85f, 0.55f, 0.25f, 0.25f, 0.45f, 0.12f, 0.05f, 0.07f, 0.04f),
            shadowTint = listOf(1.05f, 0.82f, 0.75f), highlightTint = listOf(1f, 0.97f, 0.82f),
            halation = 0.3f,
            grain = 0.11f, grainSize = 1.9f, redGrain = 0.8f, vignette = 0.35f,
        ),
    )

    /** Looks drawn by the film stock shader. */
    val looks: Set<Look> get() = stocks.keys

    /** [look]'s stock at [intensity]: 0 is neutral, 0.5 the stock as designed, 1 twice as strong. */
    fun params(look: Look, intensity: Float = 0.5f): FilmStockParams {
        val d = stocks.getValue(look)
        val k = 2f * intensity.coerceIn(0f, 1f)
        fun scaled(neutral: Float, value: Float) = neutral + (value - neutral) * k
        fun scaled(neutral: List<Float>, value: List<Float>) = neutral.zip(value) { n, v -> scaled(n, v) }
        return d.copy(
            exposure = scaled(0f, d.exposure),
            contrast = scaled(1f, d.contrast),
            fade = scaled(0f, d.fade),
            matrix = scaled(FilmStockParams.IDENTITY, d.matrix),
            saturation = scaled(1f, d.saturation),
            shadowTint = scaled(FilmStockParams.NEUTRAL, d.shadowTint),
            highlightTint = scaled(FilmStockParams.NEUTRAL, d.highlightTint),
            halation = scaled(0f, d.halation),
            grain = scaled(0f, d.grain),
            vignette = scaled(0f, d.vignette),
        )
    }
}
