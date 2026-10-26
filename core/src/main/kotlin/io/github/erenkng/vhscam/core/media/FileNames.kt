package io.github.erenkng.vhscam.core.media

import io.github.erenkng.vhscam.core.look.PhotoLook
import java.time.LocalDateTime

/** Display names for saved media. Always based on the real capture time. */
object FileNames {

    fun video(capturedAt: LocalDateTime): String = "VHS_${stamp(capturedAt)}.mp4"

    fun photo(capturedAt: LocalDateTime, look: PhotoLook): String =
        "${look.filePrefix}_${stamp(capturedAt)}.jpg"

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
