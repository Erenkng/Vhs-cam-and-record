package io.github.erenkng.vhscam.settings

import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CamcorderEffect
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.FadeColor
import io.github.erenkng.vhscam.core.look.IntervalSetting
import io.github.erenkng.vhscam.core.look.Look
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
    val photoLook: Look = Look.VHS,
    /** Look of videos; only looks with [Look.video]. */
    val videoLook: Look = Look.VHS,
    val tapeSpeed: TapeSpeed = TapeSpeed.SP,
    val vhs: VhsAdjustments = VhsAdjustments(),
    /** Strength of each look, 0..1, where 0.5 is the look as designed. VHS uses [vhs] wear. */
    val intensities: Map<Look, Float> = emptyMap(),
    /** Camcorder digital effect, used with the VHS look. */
    val effect: CamcorderEffect = CamcorderEffect.NONE,
    /** Fade in when recording starts or resumes, fade out before it stops or pauses. */
    val fade: Boolean = false,
    val fadeColor: FadeColor = FadeColor.BLACK,
    /** Tapes start with a second of the VCR's blue screen. */
    val blueIntro: Boolean = true,
    val interval: IntervalSetting = IntervalSetting.OFF,
    /** Photo mode takes a four-shot photobooth strip. */
    val booth: Boolean = false,
    /** Keep the untouched photo so its look can be changed later. */
    val keepOriginals: Boolean = true,
    val onboardingDone: Boolean = false,
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
) {
    fun intensity(look: Look): Float = intensities[look] ?: DEFAULT_INTENSITY

    companion object {
        const val DEFAULT_INTENSITY = 0.5f
    }
}
