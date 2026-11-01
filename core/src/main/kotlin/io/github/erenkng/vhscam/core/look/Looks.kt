package io.github.erenkng.vhscam.core.look

/**
 * Capture modes offered by the camera screen. [DUAL] films with two cameras at once; [PRO]
 * takes photos with manual ISO, shutter speed, focus and white balance.
 */
enum class CaptureMode {
    PHOTO,
    VIDEO,
    DUAL,
    PRO,
    ;

    /** Takes photos, with the photo look. */
    val takesPhotos: Boolean get() = this == PHOTO || this == PRO

    /** Records video, with the video look. */
    val films: Boolean get() = this == VIDEO || this == DUAL
}

/** What one press of the shutter does in photo mode. */
enum class ShotKind {
    SINGLE,

    /** Four shots with a countdown, printed as a photobooth strip. */
    BOOTH,

    /** Two shots on the same frame, like film wound back. */
    DOUBLE,

    /** Every press adds a frame to an animation. */
    STOP_MOTION,
}

/** How looks are grouped when browsing them. */
enum class LookCategory { HOME_VIDEO, FILM, DIGITAL }

/**
 * Retro looks, in the order they are offered: home video, then film, then digital cameras.
 * Every look takes photos; the ones with [video] can also record.
 *
 * @param cropAspect fixed width / height of the look's photos (landscape orientation), or
 *   `null` when the look follows the user's aspect-ratio setting.
 * @param stillLongEdge long edge, in pixels, of the processed photo.
 * @param workLines height of the working frame the look is rendered at, for the looks that get
 *   their character from a low resolution; `null` renders at the output resolution.
 * @param holdFrames the look shows a new picture every this many camera frames (low frame rate).
 * @param pictureRate the look shows this many pictures a second, whatever the camera's frame
 *   rate; takes the place of [holdFrames] when set.
 */
enum class Look(
    val category: LookCategory,
    val cropAspect: Float?,
    val stillLongEdge: Int,
    val filePrefix: String,
    val video: Boolean,
    val workLines: Int?,
    val holdFrames: Int = 1,
    val pictureRate: Int? = null,
) {
    VHS(LookCategory.HOME_VIDEO, cropAspect = null, stillLongEdge = 1440, filePrefix = "VHS", video = true, workLines = 480),

    /** Super 8 home movie: 18 pictures a second, with the film edge and sprocket hole in view. */
    SUPER8(
        LookCategory.HOME_VIDEO, cropAspect = 4f / 3f, stillLongEdge = 1440, filePrefix = "SUPER8", video = true,
        workLines = 540, pictureRate = 18,
    ),

    /** 35 mm compact camera with light leaks. */
    FILM(LookCategory.FILM, cropAspect = 3f / 2f, stillLongEdge = 2400, filePrefix = "FILM", video = false, workLines = null),

    // Colour film stocks, all drawn by the film stock shader (see FilmStocks).
    GOLD(LookCategory.FILM, cropAspect = 3f / 2f, stillLongEdge = 2400, filePrefix = "GOLD", video = false, workLines = null),
    PORTRAIT(LookCategory.FILM, cropAspect = 3f / 2f, stillLongEdge = 2400, filePrefix = "PORTRAIT", video = false, workLines = null),
    CINE(LookCategory.FILM, cropAspect = 3f / 2f, stillLongEdge = 2400, filePrefix = "CINE800", video = false, workLines = null),
    RED_GRAIN(LookCategory.FILM, cropAspect = 3f / 2f, stillLongEdge = 2400, filePrefix = "REDGRAIN", video = false, workLines = null),
    REDSCALE(LookCategory.FILM, cropAspect = 3f / 2f, stillLongEdge = 2400, filePrefix = "REDSCALE", video = false, workLines = null),

    /** Single-use camera with a flash: shots go on a 27-exposure roll that has to be developed. */
    DISPOSABLE(LookCategory.FILM, cropAspect = 3f / 2f, stillLongEdge = 1800, filePrefix = "DISPO", video = false, workLines = null),
    POLAROID(LookCategory.FILM, cropAspect = 1f, stillLongEdge = 1600, filePrefix = "POLA", video = false, workLines = null),

    CCD(LookCategory.DIGITAL, cropAspect = 4f / 3f, stillLongEdge = 1600, filePrefix = "CCD", video = false, workLines = null),

    /** 8-bit handheld camera: 128 x 112 pixels in four shades of green, 8x magnified photos. */
    GAMEBOY(
        LookCategory.DIGITAL, cropAspect = 8f / 7f, stillLongEdge = 1024, filePrefix = "8BIT", video = true, workLines = 112,
        holdFrames = 2,
    ),

    /** Mid-2000s USB webcam: QVGA, 15 fps. */
    WEBCAM(
        LookCategory.DIGITAL, cropAspect = 4f / 3f, stillLongEdge = 960, filePrefix = "WEBCAM", video = true, workLines = 240,
        holdFrames = 2,
    ),

    /** Security camera: black and white, 5 fps, time stamp. */
    CCTV(
        LookCategory.DIGITAL, cropAspect = 4f / 3f, stillLongEdge = 1152, filePrefix = "CCTV", video = true, workLines = 288,
        holdFrames = 6,
    ),
    ;

    companion object {
        val videoLooks: List<Look> = entries.filter { it.video }
    }
}

/** Aspect ratios offered for VHS photos and videos. */
enum class AspectSetting(val ratio: Float) {
    RATIO_4_3(4f / 3f),
    RATIO_16_9(16f / 9f),
}

/** VHS recording speed. EP squeezes the most tape out of a cassette and looks the worst. */
enum class TapeSpeed { SP, LP, EP }

/** The digital effects button of a 90s camcorder. Used with the VHS look. */
enum class CamcorderEffect(val holdFrames: Int = 1) {
    NONE,
    NIGHT_VISION,
    SEPIA,
    MONO,
    MOSAIC,

    /** Stepped motion, about 7 pictures a second. */
    STROBE(holdFrames = 4),

    /** Moving bright things leave a fading trail. */
    TRAIL,
}

/** Colour the camcorder fader fades to and from. */
enum class FadeColor { BLACK, WHITE }

/** Interval recording: one short snippet every [seconds]. */
enum class IntervalSetting(val seconds: Int) {
    OFF(0),
    S10(10),
    S30(30),
    M1(60),
}
