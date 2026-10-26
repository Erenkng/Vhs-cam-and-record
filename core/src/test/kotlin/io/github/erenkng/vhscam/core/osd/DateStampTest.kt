package io.github.erenkng.vhscam.core.osd

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class DateStampTest {

    private val newYear = LocalDateTime.of(1997, 1, 1, 0, 5)
    private val summer = LocalDateTime.of(1998, 6, 14, 15, 42)

    @Test
    fun camcorderDates() {
        assertEquals("JAN.01 1997", DateStamp.vhsDate(newYear, DateOrder.MONTH_DAY_YEAR))
        assertEquals("14.06.1998", DateStamp.vhsDate(summer, DateOrder.DAY_MONTH_YEAR))
        assertEquals("1998.06.14", DateStamp.vhsDate(summer, DateOrder.YEAR_MONTH_DAY))
    }

    @Test
    fun camcorderTimes() {
        assertEquals("12:05 AM", DateStamp.vhsTime(newYear, ClockStyle.H12))
        assertEquals("3:42 PM", DateStamp.vhsTime(summer, ClockStyle.H12))
        assertEquals("15:42", DateStamp.vhsTime(summer, ClockStyle.H24))
    }

    @Test
    fun filmAndDigitalImprints() {
        assertEquals("'98 6 14", DateStamp.filmDate(summer, DateOrder.YEAR_MONTH_DAY))
        assertEquals("6 14 '98", DateStamp.filmDate(summer, DateOrder.MONTH_DAY_YEAR))
        assertEquals("14/06/1998 15:42", DateStamp.ccdDate(summer, DateOrder.DAY_MONTH_YEAR))
    }

    @Test
    fun cctvStampHasSecondsAndA24HourClock() {
        val t = LocalDateTime.of(1998, 6, 14, 15, 42, 7)
        assertEquals("14-06-1998 15:42:07", DateStamp.cctvStamp(t, DateOrder.DAY_MONTH_YEAR))
        assertEquals("06-14-1998 15:42:07", DateStamp.cctvStamp(t, DateOrder.MONTH_DAY_YEAR))
        assertEquals("1998-06-14 15:42:07", DateStamp.cctvStamp(t, DateOrder.YEAR_MONTH_DAY))
    }

    @Test
    fun tapeCounter() {
        assertEquals("0:00:00", DateStamp.tapeCounter(0))
        assertEquals("0:01:05", DateStamp.tapeCounter(65_400))
        assertEquals("1:00:00", DateStamp.tapeCounter(3_600_000))
    }

    @Test
    fun camcorderClockKeepsRunningFromTheSetDate() {
        val realNow = 1_700_000_000_000L
        val clock = CamcorderClock.settingTo(newYear, realNow, ZoneOffset.UTC)
        assertEquals(newYear, clock.now(realNow, ZoneOffset.UTC))
        assertEquals(newYear.plusMinutes(10), clock.now(realNow + 600_000L, ZoneOffset.UTC))
    }
}
