package io.github.erenkng.vhscam.camera

import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.PhotoLook
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
    fun videoIsAlwaysVhs() {
        val settings = AppSettings(mode = CaptureMode.VIDEO, photoLook = PhotoLook.FILM)
        assertEquals(PhotoLook.VHS, LookFactory.activeLook(settings))
        assertTrue(LookFactory.spec(settings, LookFactory.activeLook(settings), 0.5f) is LookSpec.Vhs)
        assertNull(LookFactory.lookAspect(settings))
    }

    @Test
    fun photoLooksBringTheirOwnAspect() {
        assertEquals(1.5f, LookFactory.lookAspect(AppSettings(mode = CaptureMode.PHOTO, photoLook = PhotoLook.FILM)))
        assertEquals(1f, LookFactory.lookAspect(AppSettings(mode = CaptureMode.PHOTO, photoLook = PhotoLook.POLAROID)))
        val vhsWide = AppSettings(mode = CaptureMode.PHOTO, photoLook = PhotoLook.VHS, aspect = AspectSetting.RATIO_16_9)
        assertEquals(16f / 9f, LookFactory.lookAspect(vhsWide)!!, 1e-4f)
    }

    @Test
    fun osdFollowsTheToggles() {
        val time = LocalDateTime.of(1997, 1, 1, 0, 5)
        val dateOnly = LookFactory.osd(AppSettings(timeStamp = false), PhotoLook.VHS, time, 0L)
        assertNull(dateOnly.camcorderTime)
        assertEquals("JAN.01 1997", dateOnly.camcorderDate)

        val off = LookFactory.osd(AppSettings(dateStamp = false), PhotoLook.FILM, time, 0L)
        assertNull(off.filmStamp)

        val counter = LookFactory.osd(AppSettings(counterOsd = true), PhotoLook.VHS, time, 65_000L)
        assertEquals("0:01:05", counter.counter)
    }

    @Test
    fun centreCropKeepsTheMiddle() {
        // 3:2 out of 4:3: full height, 8/9 of the width.
        val crop = VhsSurfaceProcessor.centreCrop(4f / 3f, 3f / 2f)!!
        assertEquals(1f, crop.d, 1e-4f)
        assertEquals(8f / 9f, crop.a, 1e-4f)
        assertEquals((1f - 8f / 9f) / 2f, crop.e, 1e-4f)
        assertNull(VhsSurfaceProcessor.centreCrop(1f, 1f))
    }
}
