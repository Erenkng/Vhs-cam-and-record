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
    fun fileNamesUseCaptureTime() {
        val t = LocalDateTime.of(2026, 10, 26, 9, 5, 3)
        assertEquals("VHS_20261026_090503.mp4", FileNames.video(t))
        assertEquals("FILM_20261026_090503.jpg", FileNames.photo(t, PhotoLook.FILM))
    }
}
