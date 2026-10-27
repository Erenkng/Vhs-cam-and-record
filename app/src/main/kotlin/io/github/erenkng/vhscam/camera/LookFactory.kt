package io.github.erenkng.vhscam.camera

import io.github.erenkng.vhscam.core.look.FilmStocks
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.core.look.PhotoLookPresets
import io.github.erenkng.vhscam.core.look.VhsPresets
import io.github.erenkng.vhscam.core.osd.CamcorderClock
import io.github.erenkng.vhscam.core.osd.DateStamp
import io.github.erenkng.vhscam.gl.LookSpec
import io.github.erenkng.vhscam.gl.OsdContent
import io.github.erenkng.vhscam.settings.AppSettings
import java.time.LocalDateTime
import java.time.ZoneId

/** Builds what the renderer needs from the user's settings. */
object LookFactory {

    /** The look in use: the photo look for photos (Pro too), the video look when filming. */
    fun activeLook(settings: AppSettings): Look =
        if (settings.mode.takesPhotos) settings.photoLook else settings.videoLook.takeIf { it.video } ?: Look.VHS

    fun spec(settings: AppSettings, look: Look, seed: Float): LookSpec {
        val intensity = settings.intensity(look)
        return when (look) {
            Look.VHS -> LookSpec.Vhs(
                params = VhsPresets.params(settings.tapeSpeed, settings.vhs),
                speed = settings.tapeSpeed,
                effect = settings.effect,
            )
            Look.SUPER8 -> LookSpec.Super8(PhotoLookPresets.super8(intensity))
            Look.FILM -> LookSpec.Film(PhotoLookPresets.film(intensity), seed)
            Look.GOLD, Look.PORTRAIT, Look.CINE, Look.RED_GRAIN, Look.REDSCALE ->
                LookSpec.FilmStock(look, FilmStocks.params(look, intensity))
            Look.DISPOSABLE -> LookSpec.Disposable(PhotoLookPresets.disposable(intensity))
            Look.POLAROID -> LookSpec.Polaroid(PhotoLookPresets.polaroid(intensity), seed)
            Look.CCD -> LookSpec.Ccd(PhotoLookPresets.ccd(intensity))
            Look.GAMEBOY -> LookSpec.GameBoy(PhotoLookPresets.gameBoy(intensity))
            Look.WEBCAM -> LookSpec.Webcam(PhotoLookPresets.webcam(intensity))
            Look.CCTV -> LookSpec.Cctv(PhotoLookPresets.cctv(intensity))
        }
    }

    /** Intensity slider value of a look; VHS uses tape wear instead. */
    fun intensity(settings: AppSettings, look: Look): Float =
        if (look == Look.VHS) settings.vhs.wear else settings.intensity(look)

    fun withIntensity(settings: AppSettings, look: Look, value: Float): AppSettings =
        if (look == Look.VHS) {
            settings.copy(vhs = settings.vhs.copy(wear = value))
        } else {
            settings.copy(intensities = settings.intensities + (look to value))
        }

    /** Landscape aspect of the photo look's crop. Video crops through the camera view port. */
    fun lookAspect(settings: AppSettings): Float? =
        if (settings.mode.takesPhotos) photoAspect(settings, settings.photoLook) else null

    fun photoAspect(settings: AppSettings, look: Look): Float = look.cropAspect ?: settings.aspect.ratio

    /** The camcorder clock: the real time or the date the user set it to. */
    fun clockTime(settings: AppSettings, nowMillis: Long = System.currentTimeMillis()): LocalDateTime =
        CamcorderClock(settings.clockOffsetMillis ?: 0L).now(nowMillis, ZoneId.systemDefault())

    fun osd(settings: AppSettings, look: Look, time: LocalDateTime, counterMillis: Long): OsdContent =
        when (look) {
            Look.VHS -> OsdContent(
                camcorderTime = if (settings.dateStamp && settings.timeStamp) {
                    DateStamp.vhsTime(time, settings.clockStyle)
                } else {
                    null
                },
                camcorderDate = if (settings.dateStamp) DateStamp.vhsDate(time, settings.dateOrder) else null,
                playIndicator = settings.playOsd,
                speedLabel = settings.tapeSpeed.name,
                counter = if (settings.counterOsd) DateStamp.tapeCounter(counterMillis) else null,
            )
            Look.CCD -> OsdContent(
                digitalStamp = if (settings.dateStamp) DateStamp.ccdDate(time, settings.dateOrder) else null,
            )
            // Film cameras of the time printed the date with an orange data back.
            Look.FILM, Look.GOLD, Look.PORTRAIT, Look.CINE, Look.RED_GRAIN, Look.REDSCALE, Look.DISPOSABLE -> OsdContent(
                filmStamp = if (settings.dateStamp) DateStamp.filmDate(time, settings.dateOrder) else null,
            )
            // A security camera always stamps the time; the toggle only hides it.
            Look.CCTV -> OsdContent(
                cctvStamp = if (settings.dateStamp) DateStamp.cctvStamp(time, settings.dateOrder) else null,
            )
            Look.SUPER8, Look.POLAROID, Look.GAMEBOY, Look.WEBCAM -> OsdContent()
        }
}
