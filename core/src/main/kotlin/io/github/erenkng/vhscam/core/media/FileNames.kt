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
