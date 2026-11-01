package io.github.erenkng.vhscam.core.media

import io.github.erenkng.vhscam.core.look.Look
import java.time.LocalDateTime

/** Display names for saved media. Always based on the real capture time. */
object FileNames {

    fun video(capturedAt: LocalDateTime, look: Look = Look.VHS): String =
        "${look.filePrefix}_${stamp(capturedAt)}.mp4"

    fun photo(capturedAt: LocalDateTime, look: Look): String =
        "${look.filePrefix}_${stamp(capturedAt)}.jpg"

    /** A photobooth strip of several shots. */
    fun strip(capturedAt: LocalDateTime): String = "BOOTH_${stamp(capturedAt)}.jpg"

    fun gif(capturedAt: LocalDateTime): String = "GIF_${stamp(capturedAt)}.gif"

    /** A stop-motion animation. */
    fun animation(capturedAt: LocalDateTime): String = "ANIM_${stamp(capturedAt)}.mp4"

    /** A tape put on an old TV, as a 9:16 video. */
    fun tv(madeAt: LocalDateTime): String = "TV_${stamp(madeAt)}.mp4"

    /** Prefixes of what is made from shots rather than shot with a look. */
    val madePrefixes: Set<String> = setOf("BOOTH", "GIF", "ANIM", "TV")

    private fun stamp(t: LocalDateTime): String = buildString {
        append(t.year.toString().padStart(4, '0'))
        append(t.monthValue.toString().padStart(2, '0'))
        append(t.dayOfMonth.toString().padStart(2, '0'))
        append('_')
        append(t.hour.toString().padStart(2, '0'))
        append(t.minute.toString().padStart(2, '0'))
        append(t.second.toString().padStart(2, '0'))
    }
}
