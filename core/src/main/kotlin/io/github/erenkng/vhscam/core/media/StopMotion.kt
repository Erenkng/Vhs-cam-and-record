package io.github.erenkng.vhscam.core.media

/** Stop-motion timing: how many pictures a second, and how they play as video. */
object StopMotion {

    /** The video's frame rate; every picture shows for a whole number of its frames. */
    const val VIDEO_RATE = 24

    /** Pictures a second to choose from: each shows for 4, 3 or 2 frames of the video. */
    val RATES = listOf(6, 8, 12)

    const val DEFAULT_RATE = 12

    /** The long edge of a frame, small enough for every phone's video encoder. */
    const val FRAME_LONG_EDGE = 1280

    /** Video frames each picture shows for at [rate] pictures a second. */
    fun repeat(rate: Int): Int = ((VIDEO_RATE + rate - 1) / rate).coerceAtLeast(1)

    /** How long [frames] pictures play at [rate] pictures a second, in milliseconds. */
    fun durationMillis(frames: Int, rate: Int): Long = frames * 1_000L / rate

    /** The rate after [rate], going round; an unknown rate goes back to the first. */
    fun next(rate: Int): Int = RATES[(RATES.indexOf(rate) + 1) % RATES.size]

    /** [rate] if it is one of [RATES], or the default. */
    fun valid(rate: Int?): Int = rate?.takeIf { it in RATES } ?: DEFAULT_RATE
}
