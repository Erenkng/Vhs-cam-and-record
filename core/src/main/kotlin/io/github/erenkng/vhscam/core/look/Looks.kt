package io.github.erenkng.vhscam.core.look

/** Capture modes offered by the camera screen. */
enum class CaptureMode { PHOTO, VIDEO }

/**
 * Retro looks for photos. Video always uses [VHS].
 *
 * @param cropAspect fixed width / height of the look (landscape orientation), or `null` when the
 *   look follows the user's aspect-ratio setting.
 * @param stillLongEdge long edge, in pixels, of the processed photo.
 */
enum class PhotoLook(val cropAspect: Float?, val stillLongEdge: Int, val filePrefix: String) {
    VHS(cropAspect = null, stillLongEdge = 1440, filePrefix = "VHS"),
    CCD(cropAspect = 4f / 3f, stillLongEdge = 1600, filePrefix = "CCD"),
    FILM(cropAspect = 3f / 2f, stillLongEdge = 2400, filePrefix = "FILM"),
    POLAROID(cropAspect = 1f, stillLongEdge = 1600, filePrefix = "POLA"),
}

/** Aspect ratios offered for VHS photos and videos. */
enum class AspectSetting(val ratio: Float) {
    RATIO_4_3(4f / 3f),
    RATIO_16_9(16f / 9f),
}

/** VHS recording speed. EP squeezes the most tape out of a cassette and looks the worst. */
enum class TapeSpeed { SP, LP, EP }
