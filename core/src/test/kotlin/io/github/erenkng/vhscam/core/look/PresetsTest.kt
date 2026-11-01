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
    fun super8AndDisposableScaleWithIntensity() {
        assertEquals(Super8Params(), PhotoLookPresets.super8(0.5f))
        val clean8 = PhotoLookPresets.super8(0f)
        assertEquals(0f, clean8.grain)
        assertEquals(0f, clean8.gate)
        assertEquals(0f, clean8.weave)
        assertEquals(1f, PhotoLookPresets.super8(1f).gate)
        assertEquals(DisposableParams(), PhotoLookPresets.disposable(0.5f))
        assertEquals(0f, PhotoLookPresets.disposable(0f).flash)
        assertEquals(1f, PhotoLookPresets.disposable(0f).highlightTintR)
        assertTrue(PhotoLookPresets.disposable(1f).flash > DisposableParams().flash)
    }

    @Test
    fun filmStocksScaleFromNeutral() {
        for (look in FilmStocks.looks) {
            val neutral = FilmStocks.params(look, 0f)
            assertEquals(FilmStockParams.IDENTITY, neutral.matrix, "$look matrix")
            assertEquals(1f, neutral.contrast, "$look contrast")
            assertEquals(0f, neutral.grain, "$look grain")
            assertEquals(0f, neutral.halation, "$look halation")
            assertEquals(LookCategory.FILM, look.category)
            assertTrue(FilmStocks.params(look, 1f).grain > FilmStocks.params(look, 0.5f).grain, "$look grain grows")
        }
        assertEquals(
            setOf(
                Look.GOLD, Look.PORTRAIT, Look.CINE, Look.RED_GRAIN, Look.REDSCALE,
                Look.SILVER, Look.AEROCHROME, Look.CROSS, Look.SLIDE,
            ),
            FilmStocks.looks,
        )
        // Among the colour negatives, Red Grain is the grainiest and the reddest; Cine 800 glows the most.
        val negatives = listOf(Look.GOLD, Look.PORTRAIT, Look.CINE, Look.RED_GRAIN, Look.REDSCALE)
        val red = FilmStocks.params(Look.RED_GRAIN)
        assertTrue(negatives.all { FilmStocks.params(it).grain <= red.grain && FilmStocks.params(it).redGrain <= red.redGrain })
        assertTrue(negatives.all { FilmStocks.params(it).halation <= FilmStocks.params(Look.CINE).halation })
    }

    @Test
    fun specialFilmsKeepTheirCharacter() {
        // Black and white at any strength, with grey grain.
        for (intensity in listOf(0f, 0.3f, 1f)) assertEquals(0f, FilmStocks.params(Look.SILVER, intensity).saturation)
        assertEquals(0f, FilmStocks.params(Look.SILVER).redGrain)
        // The slide sits in its mount, which goes with the effect.
        assertEquals(1f, FilmStocks.params(Look.SLIDE).mount)
        assertEquals(1f, FilmStocks.params(Look.SLIDE, 1f).mount)
        assertEquals(0f, FilmStocks.params(Look.SLIDE, 0f).mount)
        // Only the toy camera's lens blurs; infrared turns green into red.
        assertTrue(FilmStocks.params(Look.CROSS).softness > 0f)
        assertTrue(FilmStocks.looks.minus(Look.CROSS).all { FilmStocks.params(it).softness == 0f })
        val ir = FilmStocks.params(Look.AEROCHROME).matrix
        assertTrue(ir[1] > ir[0], "green drives the red record")
    }

    @Test
    fun v12LooksScaleWithIntensity() {
        assertEquals(MiniDvParams(), PhotoLookPresets.miniDv(0.5f))
        assertEquals(0f, PhotoLookPresets.miniDv(0f).fisheye)
        assertEquals(1f, PhotoLookPresets.miniDv(1f).hood.coerceAtMost(1f))
        // Night mode is green at any strength; only the struggling goes.
        assertEquals(NightShotParams().gain, PhotoLookPresets.nightShot(0f).gain)
        assertEquals(0f, PhotoLookPresets.nightShot(0f).ghost)
        assertEquals(CrtParams(), PhotoLookPresets.crt(0.5f))
        assertEquals(0f, PhotoLookPresets.crt(0f).curvature)
        assertEquals(FlipPhoneParams(), PhotoLookPresets.flipPhone(0.5f))
        assertTrue(PhotoLookPresets.flipPhone(1f).blockiness <= 1f)
        // Still printed in dots when turned right down.
        assertEquals(1f, PhotoLookPresets.thermal(0f).dither)
        assertEquals(0f, PhotoLookPresets.thermal(0f).paper)
        assertEquals(MavicaParams(), PhotoLookPresets.mavica(0.5f))
        assertEquals(1f, PhotoLookPresets.mavica(0f).highlightKnee)
        assertTrue(PhotoLookPresets.mavica(1f).jpegQuality < PhotoLookPresets.mavica(0f).jpegQuality)
    }

    @Test
    fun looksAreOrderedByCategory() {
        val categories = Look.entries.map { it.category }
        assertEquals(categories.sortedBy { it.ordinal }, categories)
    }

    @Test
    fun super8RunsAtEighteenPicturesASecond() {
        assertEquals(18, Look.SUPER8.pictureRate)
        assertTrue(Look.SUPER8.video)
        assertEquals(null, Look.VHS.pictureRate)
    }

    @Test
    fun videoLooksAreTheOnesThatRecord() {
        assertEquals(
            listOf(
                Look.VHS, Look.SUPER8, Look.MINIDV, Look.NIGHTSHOT, Look.CRT,
                Look.GAMEBOY, Look.WEBCAM, Look.FLIP, Look.CCTV,
            ),
            Look.videoLooks,
        )
        assertTrue(Look.entries.filter { it.workLines == null }.none { it.video })
        assertTrue(Look.CCTV.holdFrames > Look.WEBCAM.holdFrames)
    }

    @Test
    fun fileNamesUseCaptureTime() {
        val t = LocalDateTime.of(2026, 10, 26, 9, 5, 3)
        assertEquals("VHS_20261026_090503.mp4", FileNames.video(t))
        assertEquals("CCTV_20261026_090503.mp4", FileNames.video(t, Look.CCTV))
        assertEquals("FILM_20261026_090503.jpg", FileNames.photo(t, Look.FILM))
        assertEquals("DISPO_20261026_090503.jpg", FileNames.photo(t, Look.DISPOSABLE))
        assertEquals("BOOTH_20261026_090503.jpg", FileNames.strip(t))
        assertEquals("GIF_20261026_090503.gif", FileNames.gif(t))
        assertEquals("MINIDV_20261026_090503.mp4", FileNames.video(t, Look.MINIDV))
        assertEquals("3GP_20261026_090503.mp4", FileNames.video(t, Look.FLIP))
        assertEquals("MVC_20261026_090503.jpg", FileNames.photo(t, Look.MAVICA))
        assertEquals("SILVER400_20261026_090503.jpg", FileNames.photo(t, Look.SILVER))
        assertEquals("TV_20261026_090503.mp4", FileNames.tv(t))
    }

    @Test
    fun lookFilePrefixesAreDistinct() {
        assertEquals(Look.entries.size, Look.entries.map { it.filePrefix }.toSet().size)
        // Strips, GIFs, animations and TV clips keep names of their own.
        assertTrue(Look.entries.none { it.filePrefix in FileNames.madePrefixes })
    }
}
