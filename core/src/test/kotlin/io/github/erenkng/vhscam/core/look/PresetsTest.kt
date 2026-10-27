package io.github.erenkng.vhscam.core.look

import io.github.erenkng.vhscam.core.media.FileNames
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetsTest {

    @Test
    fun slowerTapeSpeedsLookWorse() {
        val sp = VhsPresets.params(TapeSpeed.SP)
        val ep = VhsPresets.params(TapeSpeed.EP)
        assertTrue(ep.lumaBlur > sp.lumaBlur)
        assertTrue(ep.chromaBlur > sp.chromaBlur)
        assertTrue(ep.lumaNoise > sp.lumaNoise)
        assertTrue(ep.tracking > sp.tracking)
    }

    @Test
    fun zeroWearRemovesDamageButKeepsTheSoftLook() {
        val clean = VhsPresets.params(TapeSpeed.LP, VhsAdjustments(wear = 0f))
        assertEquals(0f, clean.lumaNoise)
        assertEquals(0f, clean.tracking)
        assertEquals(0f, clean.dropouts)
        assertTrue(clean.lumaBlur > 0f && clean.chromaBlur > 0f)
    }

    @Test
    fun defaultWearIsThePresetAsDesigned() {
        assertEquals(VhsPresets.base(TapeSpeed.SP), VhsPresets.params(TapeSpeed.SP))
    }

    @Test
    fun photoLooksScaleWithIntensity() {
        assertEquals(0f, PhotoLookPresets.film(0f).grain)
        assertTrue(PhotoLookPresets.film(1f).grain > PhotoLookPresets.film(0.5f).grain)
        assertEquals(1f, PhotoLookPresets.polaroid(0f).saturation)
        assertTrue(PhotoLookPresets.ccd(1f).jpegQuality < PhotoLookPresets.ccd(0.2f).jpegQuality)
    }

    @Test
    fun newLooksScaleWithIntensity() {
        assertEquals(0f, PhotoLookPresets.gameBoy(0f).edge)
        assertEquals(1f, PhotoLookPresets.gameBoy(1f).dither)
        assertEquals(GameBoyParams().contrast, PhotoLookPresets.gameBoy(0.5f).contrast)
        assertEquals(0f, PhotoLookPresets.webcam(0f).noise)
        assertTrue(PhotoLookPresets.webcam(1f).blockiness <= 1f)
        assertEquals(1f, PhotoLookPresets.cctv(0f).contrast)
        assertEquals(CctvParams(), PhotoLookPresets.cctv(0.5f))
    }

    @Test
    fun videoLooksAreTheOnesThatRecord() {
        assertEquals(listOf(Look.VHS, Look.GAMEBOY, Look.WEBCAM, Look.CCTV), Look.videoLooks)
        assertTrue(Look.entries.filter { it.workLines == null }.none { it.video })
        assertTrue(Look.CCTV.holdFrames > Look.WEBCAM.holdFrames)
    }

    @Test
    fun fileNamesUseCaptureTime() {
        val t = LocalDateTime.of(2026, 10, 26, 9, 5, 3)
        assertEquals("VHS_20261026_090503.mp4", FileNames.video(t))
        assertEquals("CCTV_20261026_090503.mp4", FileNames.video(t, Look.CCTV))
        assertEquals("FILM_20261026_090503.jpg", FileNames.photo(t, Look.FILM))
        assertEquals("BOOTH_20261026_090503.jpg", FileNames.strip(t))
        assertEquals("GIF_20261026_090503.gif", FileNames.gif(t))
    }
}
