package io.github.erenkng.vhscam.camera

import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.gl.LookSpec
import io.github.erenkng.vhscam.gl.VhsSurfaceProcessor
import io.github.erenkng.vhscam.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class LookFactoryTest {

    @Test
    fun filmingUsesTheVideoLook() {
        val settings = AppSettings(mode = CaptureMode.VIDEO, photoLook = Look.FILM)
        assertEquals(Look.VHS, LookFactory.activeLook(settings))
        assertTrue(LookFactory.spec(settings, LookFactory.activeLook(settings), 0.5f) is LookSpec.Vhs)
        assertNull(LookFactory.lookAspect(settings))

        val cctv = settings.copy(mode = CaptureMode.DUAL, videoLook = Look.CCTV)
        assertEquals(Look.CCTV, LookFactory.activeLook(cctv))
        // A look that can't record falls back to VHS.
        assertEquals(Look.VHS, LookFactory.activeLook(settings.copy(videoLook = Look.POLAROID)))
    }

    @Test
    fun intensityRoundTrips() {
        Look.entries.forEach { look ->
            val changed = LookFactory.withIntensity(AppSettings(), look, 0.8f)
            assertEquals(0.8f, LookFactory.intensity(changed, look), 1e-6f)
        }
    }

    @Test
    fun photoLooksBringTheirOwnAspect() {
        assertEquals(1.5f, LookFactory.lookAspect(AppSettings(mode = CaptureMode.PHOTO, photoLook = Look.FILM)))
        assertEquals(1f, LookFactory.lookAspect(AppSettings(mode = CaptureMode.PHOTO, photoLook = Look.POLAROID)))
        val vhsWide = AppSettings(mode = CaptureMode.PHOTO, photoLook = Look.VHS, aspect = AspectSetting.RATIO_16_9)
        assertEquals(16f / 9f, LookFactory.lookAspect(vhsWide)!!, 1e-4f)
    }

    @Test
    fun osdFollowsTheToggles() {
        val time = LocalDateTime.of(1997, 1, 1, 0, 5)
        val dateOnly = LookFactory.osd(AppSettings(timeStamp = false), Look.VHS, time, 0L)
        assertNull(dateOnly.camcorderTime)
        assertEquals("JAN.01 1997", dateOnly.camcorderDate)

        val off = LookFactory.osd(AppSettings(dateStamp = false), Look.FILM, time, 0L)
        assertNull(off.filmStamp)

        val counter = LookFactory.osd(AppSettings(counterOsd = true), Look.VHS, time, 65_000L)
        assertEquals("0:01:05", counter.counter)

        val cctv = LookFactory.osd(AppSettings(), Look.CCTV, time, 0L)
        assertEquals("01-01-1997 00:05:00", cctv.cctvStamp)
    }

    @Test
    fun centreCropKeepsTheMiddle() {
        // 3:2 is wider than 4:3: full width, 8/9 of the height.
        val wide = VhsSurfaceProcessor.centreCrop(4f / 3f, 3f / 2f)!!
        assertEquals(1f, wide.a, 1e-4f)
        assertEquals(0f, wide.e, 1e-4f)
        assertEquals(8f / 9f, wide.d, 1e-4f)
        assertEquals((1f - 8f / 9f) / 2f, wide.f, 1e-4f)

        // A square out of 4:3: full height, the middle 3/4 of the width.
        val square = VhsSurfaceProcessor.centreCrop(4f / 3f, 1f)!!
        assertEquals(0.75f, square.a, 1e-4f)
        assertEquals(0.125f, square.e, 1e-4f)
        assertEquals(1f, square.d, 1e-4f)
        assertEquals(0f, square.f, 1e-4f)

        assertNull(VhsSurfaceProcessor.centreCrop(1f, 1f))
    }
}
