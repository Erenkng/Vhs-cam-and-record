package io.github.erenkng.vhscam.camera

import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.PhotoLook
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

    /** The look in use: video is always VHS, photos use the chosen look. */
    fun activeLook(settings: AppSettings): PhotoLook =
        if (settings.mode == CaptureMode.VIDEO) PhotoLook.VHS else settings.photoLook

    fun spec(settings: AppSettings, look: PhotoLook, seed: Float): LookSpec = when (look) {
        PhotoLook.VHS -> LookSpec.Vhs(VhsPresets.params(settings.tapeSpeed, settings.vhs), settings.tapeSpeed)
        PhotoLook.CCD -> LookSpec.Ccd(PhotoLookPresets.ccd(settings.ccdIntensity))
        PhotoLook.FILM -> LookSpec.Film(PhotoLookPresets.film(settings.filmIntensity), seed)
        PhotoLook.POLAROID -> LookSpec.Polaroid(PhotoLookPresets.polaroid(settings.polaroidIntensity), seed)
    }

    /** Landscape aspect of the photo look's crop. Video crops through the camera view port. */
    fun lookAspect(settings: AppSettings): Float? =
        if (settings.mode == CaptureMode.VIDEO) null else settings.photoLook.cropAspect ?: settings.aspect.ratio

    /** The camcorder clock: the real time or the date the user set it to. */
    fun clockTime(settings: AppSettings, nowMillis: Long = System.currentTimeMillis()): LocalDateTime =
        CamcorderClock(settings.clockOffsetMillis ?: 0L).now(nowMillis, ZoneId.systemDefault())

    fun osd(settings: AppSettings, look: PhotoLook, time: LocalDateTime, counterMillis: Long): OsdContent =
        when (look) {
            PhotoLook.VHS -> OsdContent(
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
            PhotoLook.CCD -> OsdContent(
                digitalStamp = if (settings.dateStamp) DateStamp.ccdDate(time, settings.dateOrder) else null,
            )
            PhotoLook.FILM -> OsdContent(
                filmStamp = if (settings.dateStamp) DateStamp.filmDate(time, settings.dateOrder) else null,
            )
            PhotoLook.POLAROID -> OsdContent()
        }
}
