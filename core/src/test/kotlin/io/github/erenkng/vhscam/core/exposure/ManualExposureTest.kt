package io.github.erenkng.vhscam.core.exposure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ln

class ManualExposureTest {

    private val isoRange = 50..3200
    private val timeRange = 100_000L..1_000_000_000L
    private val metered = Exposure(iso = 400, timeNanos = ManualExposure.NANOS_PER_SECOND / 60)

    private fun assertNear(expected: Long, actual: Long) = assertTrue("$actual != $expected", abs(actual - expected) <= 2L)

    @Test
    fun isoPriorityKeepsTheBrightness() {
        val e = ManualExposure.complete(iso = 100, timeNanos = null, metered = metered, isoRange = isoRange, timeRange = timeRange)
        assertEquals(100, e.iso)
        // Two stops less sensitive: four times as long.
        assertNear(ManualExposure.NANOS_PER_SECOND / 15, e.timeNanos)
    }

    @Test
    fun shutterPriorityKeepsTheBrightness() {
        val e = ManualExposure.complete(iso = null, timeNanos = ManualExposure.NANOS_PER_SECOND / 240, metered = metered, isoRange = isoRange, timeRange = timeRange)
        assertEquals(1600, e.iso)
    }

    @Test
    fun valuesStayInsideWhatTheSensorCanDo() {
        val dark = ManualExposure.complete(iso = 50, timeNanos = null, metered = Exposure(3200, 100_000_000L), isoRange = isoRange, timeRange = timeRange)
        assertEquals(timeRange.last, dark.timeNanos)
        val bright = ManualExposure.complete(iso = null, timeNanos = 1_000_000_000L, metered = metered, isoRange = isoRange, timeRange = timeRange)
        assertEquals(isoRange.first, bright.iso)
    }

    @Test
    fun withoutAMeterReadingItStartsFromIso100At60th() {
        val e = ManualExposure.complete(iso = 200, timeNanos = null, metered = null, isoRange = isoRange, timeRange = timeRange)
        assertNear(ManualExposure.NANOS_PER_SECOND / 120, e.timeNanos)
    }

    @Test
    fun stopsAreFilteredAndEvenlySpaced() {
        assertEquals(listOf(50, 64, 80, 100), ManualExposure.isoStops(50..100))
        val stops = ManualExposure.SHUTTER_STOPS
        assertTrue(stops.zipWithNext().all { (a, b) -> b > a })
        // Neighbouring stops are about half a stop apart.
        stops.zipWithNext().forEach { (a, b) ->
            val halfStops = ln(b.toDouble() / a) / ln(2.0) * 2
            assertTrue("$a -> $b", abs(halfStops - 1) < 0.45)
        }
    }

    @Test
    fun shutterTimesReadLikeACamera() {
        assertEquals("1/250", ManualExposure.formatShutter(ManualExposure.NANOS_PER_SECOND / 250))
        assertEquals("1/60", ManualExposure.formatShutter(16_666_666L))
        assertEquals("0.3\"", ManualExposure.formatShutter(333_333_333L))
        assertEquals("0.5\"", ManualExposure.formatShutter(500_000_000L))
        assertEquals("2\"", ManualExposure.formatShutter(2_000_000_000L))
    }
}
