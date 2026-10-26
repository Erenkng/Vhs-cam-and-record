package io.github.erenkng.vhscam.settings

import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.PhotoLook
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.core.look.VhsAdjustments
import io.github.erenkng.vhscam.core.osd.ClockStyle
import io.github.erenkng.vhscam.core.osd.DateOrder

enum class FlashSetting { OFF, AUTO, ON }

enum class TimerSetting(val seconds: Int) { OFF(0), THREE(3), TEN(10) }

enum class VideoQuality { SD, HD, FHD }

enum class WhiteBalance { AUTO, DAYLIGHT, CLOUDY, INCANDESCENT, FLUORESCENT }

/** Everything the user can change. Defaults are the out-of-the-box experience. */
data class AppSettings(
    val mode: CaptureMode = CaptureMode.VIDEO,
    val frontCamera: Boolean = false,
    val photoLook: PhotoLook = PhotoLook.VHS,
    val tapeSpeed: TapeSpeed = TapeSpeed.SP,
    val vhs: VhsAdjustments = VhsAdjustments(),
    val ccdIntensity: Float = 0.5f,
    val filmIntensity: Float = 0.5f,
    val polaroidIntensity: Float = 0.5f,
    val aspect: AspectSetting = AspectSetting.RATIO_4_3,
    val videoQuality: VideoQuality = VideoQuality.HD,
    val flash: FlashSetting = FlashSetting.OFF,
    val timer: TimerSetting = TimerSetting.OFF,
    val grid: Boolean = false,
    val dateStamp: Boolean = true,
    val timeStamp: Boolean = true,
    val dateOrder: DateOrder = DateOrder.MONTH_DAY_YEAR,
    val clockStyle: ClockStyle = ClockStyle.H12,
    /** Offset of the camcorder clock from real time, or null to show the real date. */
    val clockOffsetMillis: Long? = null,
    val playOsd: Boolean = false,
    val counterOsd: Boolean = false,
    val microphone: Boolean = true,
    val tapeAudio: Boolean = true,
    val mirrorSelfies: Boolean = true,
    val shutterSound: Boolean = true,
    val haptics: Boolean = true,
    val dynamicColor: Boolean = true,
)
