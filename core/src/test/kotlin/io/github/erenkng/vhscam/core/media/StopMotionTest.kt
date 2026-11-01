package io.github.erenkng.vhscam.core.media

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class StopMotionTest {

    @Test
    fun everyRateFillsWholeVideoFrames() {
        assertEquals(listOf(4, 3, 2), StopMotion.RATES.map { StopMotion.repeat(it) })
        StopMotion.RATES.forEach { rate -> assertEquals(StopMotion.VIDEO_RATE, rate * StopMotion.repeat(rate)) }
    }

    @Test
    fun lengthFollowsTheRate() {
        assertEquals(1_000L, StopMotion.durationMillis(12, 12))
        assertEquals(1_500L, StopMotion.durationMillis(12, 8))
        assertEquals(0L, StopMotion.durationMillis(0, 6))
    }

    @Test
    fun ratesGoRound() {
        assertEquals(8, StopMotion.next(6))
        assertEquals(12, StopMotion.next(8))
        assertEquals(6, StopMotion.next(12))
        // A rate saved by some other version starts the round again.
        assertEquals(6, StopMotion.next(30))
        assertEquals(StopMotion.DEFAULT_RATE, StopMotion.valid(30))
        assertEquals(8, StopMotion.valid(8))
        assertEquals(StopMotion.DEFAULT_RATE, StopMotion.valid(null))
    }

    @Test
    fun animationsAreNamedLikeTheOtherCaptures() {
        assertEquals("ANIM_20261027_090503.mp4", FileNames.animation(LocalDateTime.of(2026, 10, 27, 9, 5, 3)))
    }
}
