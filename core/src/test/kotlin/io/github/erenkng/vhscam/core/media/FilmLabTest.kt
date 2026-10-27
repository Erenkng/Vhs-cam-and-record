package io.github.erenkng.vhscam.core.media

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class FilmLabTest {

    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 10, day, hour, minute)

    @Test
    fun afternoonRollsAreReadyNextMorning() {
        assertEquals(at(28, 9), FilmLab.readyAt(at(27, 15)))
        assertEquals(at(28, 9), FilmLab.readyAt(at(27, 23, 30)))
    }

    @Test
    fun earlyMorningRollsWaitAtLeastSixHours() {
        // Dropped off an hour before pick-up: back the day after, not an hour later.
        assertEquals(at(28, 9), FilmLab.readyAt(at(27, 8)))
        // In the middle of the night: the same morning, exactly six hours later.
        assertEquals(at(27, 9), FilmLab.readyAt(at(27, 3)))
    }

    @Test
    fun printsAreNumberedOnTheirRoll() {
        assertEquals("DISPO_20261027_153000_07.jpg", FilmLab.printName(at(27, 15, 30), 7))
        assertEquals(27, FilmLab.ROLL_SIZE)
    }
}
