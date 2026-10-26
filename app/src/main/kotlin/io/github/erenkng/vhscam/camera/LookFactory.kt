package io.github.erenkng.vhscam.camera

import io.github.erenkng.vhscam.core.look.CaptureMode
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

    /** The look in use: the video look when filming, the photo look otherwise. */
    fun activeLook(settings: AppSettings): Look =
        if (settings.mode == CaptureMode.PHOTO) settings.photoLook else settings.videoLook.takeIf { it.video } ?: Look.VHS

    fun spec(settings: AppSettings, look: Look, seed: Float): LookSpec = when (look) {
        Look.VHS -> LookSpec.Vhs(
            params = VhsPresets.params(settings.tapeSpeed, settings.vhs),
            speed = settings.tapeSpeed,
            effect = settings.effect,
        )
        Look.CCD -> LookSpec.Ccd(PhotoLookPresets.ccd(settings.ccdIntensity))
        Look.FILM -> LookSpec.Film(PhotoLookPresets.film(settings.filmIntensity), seed)
        Look.POLAROID -> LookSpec.Polaroid(PhotoLookPresets.polaroid(settings.polaroidIntensity), seed)
        Look.GAMEBOY -> LookSpec.GameBoy(PhotoLookPresets.gameBoy(settings.gameBoyIntensity))
        Look.WEBCAM -> LookSpec.Webcam(PhotoLookPresets.webcam(settings.webcamIntensity))
        Look.CCTV -> LookSpec.Cctv(PhotoLookPresets.cctv(settings.cctvIntensity))
    }

    /** Intensity slider value of a look; VHS uses tape wear instead. */
    fun intensity(settings: AppSettings, look: Look): Float = when (look) {
        Look.VHS -> settings.vhs.wear
        Look.CCD -> settings.ccdIntensity
        Look.FILM -> settings.filmIntensity
        Look.POLAROID -> settings.polaroidIntensity
        Look.GAMEBOY -> settings.gameBoyIntensity
        Look.WEBCAM -> settings.webcamIntensity
        Look.CCTV -> settings.cctvIntensity
    }

    fun withIntensity(settings: AppSettings, look: Look, value: Float): AppSettings = when (look) {
        Look.VHS -> settings.copy(vhs = settings.vhs.copy(wear = value))
        Look.CCD -> settings.copy(ccdIntensity = value)
        Look.FILM -> settings.copy(filmIntensity = value)
        Look.POLAROID -> settings.copy(polaroidIntensity = value)
        Look.GAMEBOY -> settings.copy(gameBoyIntensity = value)
        Look.WEBCAM -> settings.copy(webcamIntensity = value)
        Look.CCTV -> settings.copy(cctvIntensity = value)
    }

    /** Landscape aspect of the photo look's crop. Video crops through the camera view port. */
    fun lookAspect(settings: AppSettings): Float? =
        if (settings.mode == CaptureMode.PHOTO) photoAspect(settings, settings.photoLook) else null

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
            Look.FILM -> OsdContent(
                filmStamp = if (settings.dateStamp) DateStamp.filmDate(time, settings.dateOrder) else null,
            )
            // A security camera always stamps the time; the toggle only hides it.
            Look.CCTV -> OsdContent(
                cctvStamp = if (settings.dateStamp) DateStamp.cctvStamp(time, settings.dateOrder) else null,
            )
            Look.POLAROID, Look.GAMEBOY, Look.WEBCAM -> OsdContent()
        }
}
