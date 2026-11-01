package io.github.erenkng.vhscam.core.media

import io.github.erenkng.vhscam.core.look.Look
import java.time.LocalDateTime
import java.time.LocalTime

/** The photo lab for the single-use camera: rolls go in, prints come out the next morning. */
object FilmLab {

    /** Shots on a roll, like the classic single-use cameras. */
    const val ROLL_SIZE = 27

    /** When the lab hands back prints. */
    val PICKUP: LocalTime = LocalTime.of(9, 0)

    /** A roll spends at least this long at the lab, so one dropped off at 8 isn't back at 9. */
    const val MIN_HOURS = 6L

    /** When a roll dropped off at [sentAt] is ready: the first pick-up time after the minimum wait. */
    fun readyAt(sentAt: LocalDateTime): LocalDateTime {
        val earliest = sentAt.plusHours(MIN_HOURS)
        var ready = sentAt.toLocalDate().atTime(PICKUP)
        while (ready.isBefore(earliest)) ready = ready.plusDays(1)
        return ready
    }

    /** Display name of a print, numbered on its roll like the frame numbers on the negative. */
    fun printName(capturedAt: LocalDateTime, frame: Int): String =
        FileNames.photo(capturedAt, Look.DISPOSABLE)
            .removeSuffix(".jpg") + "_" + frame.toString().padStart(2, '0') + ".jpg"
}
