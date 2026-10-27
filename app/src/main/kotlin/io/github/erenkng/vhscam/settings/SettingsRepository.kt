package io.github.erenkng.vhscam.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.core.look.VhsAdjustments
import io.github.erenkng.vhscam.core.osd.ClockStyle
import io.github.erenkng.vhscam.core.osd.DateOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private object Keys {
        val mode = stringPreferencesKey("mode")
        val frontCamera = booleanPreferencesKey("front_camera")
        val photoLook = stringPreferencesKey("photo_look")
        val videoLook = stringPreferencesKey("video_look")
        val gameBoyIntensity = floatPreferencesKey("gameboy_intensity")
        val webcamIntensity = floatPreferencesKey("webcam_intensity")
        val cctvIntensity = floatPreferencesKey("cctv_intensity")
        val super8Intensity = floatPreferencesKey("super8_intensity")
        val disposableIntensity = floatPreferencesKey("disposable_intensity")
        val effect = stringPreferencesKey("effect")
        val fade = booleanPreferencesKey("fade")
        val fadeColor = stringPreferencesKey("fade_color")
        val blueIntro = booleanPreferencesKey("blue_intro")
        val interval = stringPreferencesKey("interval")
        val booth = booleanPreferencesKey("booth")
        val keepOriginals = booleanPreferencesKey("keep_originals")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val tapeSpeed = stringPreferencesKey("tape_speed")
        val wear = floatPreferencesKey("vhs_wear")
        val colorBleed = floatPreferencesKey("vhs_color_bleed")
        val noise = floatPreferencesKey("vhs_noise")
        val tracking = floatPreferencesKey("vhs_tracking")
        val jitter = floatPreferencesKey("vhs_jitter")
        val sharpen = floatPreferencesKey("vhs_sharpen")
        val headSwitch = floatPreferencesKey("vhs_head_switch")
        val scanlines = floatPreferencesKey("vhs_scanlines")
        val vignette = floatPreferencesKey("vhs_vignette")
        val ccdIntensity = floatPreferencesKey("ccd_intensity")
        val filmIntensity = floatPreferencesKey("film_intensity")
        val polaroidIntensity = floatPreferencesKey("polaroid_intensity")
        val aspect = stringPreferencesKey("aspect")
        val videoQuality = stringPreferencesKey("video_quality")
        val flash = stringPreferencesKey("flash")
        val timer = stringPreferencesKey("timer")
        val grid = booleanPreferencesKey("grid")
        val dateStamp = booleanPreferencesKey("date_stamp")
        val timeStamp = booleanPreferencesKey("time_stamp")
        val dateOrder = stringPreferencesKey("date_order")
        val clockStyle = stringPreferencesKey("clock_style")
        val clockOffset = longPreferencesKey("clock_offset")
        val playOsd = booleanPreferencesKey("play_osd")
        val counterOsd = booleanPreferencesKey("counter_osd")
        val microphone = booleanPreferencesKey("microphone")
        val tapeAudio = booleanPreferencesKey("tape_audio")
        val mirrorSelfies = booleanPreferencesKey("mirror_selfies")
        val shutterSound = booleanPreferencesKey("shutter_sound")
        val haptics = booleanPreferencesKey("haptics")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        val dv = d.vhs
        return AppSettings(
            mode = enum(Keys.mode, d.mode),
            frontCamera = this[Keys.frontCamera] ?: d.frontCamera,
            photoLook = enum(Keys.photoLook, d.photoLook),
            videoLook = enum(Keys.videoLook, d.videoLook).takeIf { it.video } ?: d.videoLook,
            gameBoyIntensity = this[Keys.gameBoyIntensity] ?: d.gameBoyIntensity,
            webcamIntensity = this[Keys.webcamIntensity] ?: d.webcamIntensity,
            cctvIntensity = this[Keys.cctvIntensity] ?: d.cctvIntensity,
            super8Intensity = this[Keys.super8Intensity] ?: d.super8Intensity,
            disposableIntensity = this[Keys.disposableIntensity] ?: d.disposableIntensity,
            effect = enum(Keys.effect, d.effect),
            fade = this[Keys.fade] ?: d.fade,
            fadeColor = enum(Keys.fadeColor, d.fadeColor),
            blueIntro = this[Keys.blueIntro] ?: d.blueIntro,
            interval = enum(Keys.interval, d.interval),
            booth = this[Keys.booth] ?: d.booth,
            keepOriginals = this[Keys.keepOriginals] ?: d.keepOriginals,
            onboardingDone = this[Keys.onboardingDone] ?: d.onboardingDone,
            tapeSpeed = enum(Keys.tapeSpeed, d.tapeSpeed),
            vhs = VhsAdjustments(
                wear = this[Keys.wear] ?: dv.wear,
                colorBleed = this[Keys.colorBleed] ?: dv.colorBleed,
                noise = this[Keys.noise] ?: dv.noise,
                tracking = this[Keys.tracking] ?: dv.tracking,
                jitter = this[Keys.jitter] ?: dv.jitter,
                sharpen = this[Keys.sharpen] ?: dv.sharpen,
                headSwitch = this[Keys.headSwitch] ?: dv.headSwitch,
                scanlines = this[Keys.scanlines] ?: dv.scanlines,
                vignette = this[Keys.vignette] ?: dv.vignette,
            ),
            ccdIntensity = this[Keys.ccdIntensity] ?: d.ccdIntensity,
            filmIntensity = this[Keys.filmIntensity] ?: d.filmIntensity,
            polaroidIntensity = this[Keys.polaroidIntensity] ?: d.polaroidIntensity,
            aspect = enum(Keys.aspect, d.aspect),
            videoQuality = enum(Keys.videoQuality, d.videoQuality),
            flash = enum(Keys.flash, d.flash),
            timer = enum(Keys.timer, d.timer),
            grid = this[Keys.grid] ?: d.grid,
            dateStamp = this[Keys.dateStamp] ?: d.dateStamp,
            timeStamp = this[Keys.timeStamp] ?: d.timeStamp,
            dateOrder = enum(Keys.dateOrder, d.dateOrder),
            clockStyle = enum(Keys.clockStyle, d.clockStyle),
            clockOffsetMillis = this[Keys.clockOffset],
            playOsd = this[Keys.playOsd] ?: d.playOsd,
            counterOsd = this[Keys.counterOsd] ?: d.counterOsd,
            microphone = this[Keys.microphone] ?: d.microphone,
            tapeAudio = this[Keys.tapeAudio] ?: d.tapeAudio,
            mirrorSelfies = this[Keys.mirrorSelfies] ?: d.mirrorSelfies,
            shutterSound = this[Keys.shutterSound] ?: d.shutterSound,
            haptics = this[Keys.haptics] ?: d.haptics,
            dynamicColor = this[Keys.dynamicColor] ?: d.dynamicColor,
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[Keys.mode] = s.mode.name
        this[Keys.frontCamera] = s.frontCamera
        this[Keys.photoLook] = s.photoLook.name
        this[Keys.videoLook] = s.videoLook.name
        this[Keys.gameBoyIntensity] = s.gameBoyIntensity
        this[Keys.webcamIntensity] = s.webcamIntensity
        this[Keys.cctvIntensity] = s.cctvIntensity
        this[Keys.super8Intensity] = s.super8Intensity
        this[Keys.disposableIntensity] = s.disposableIntensity
        this[Keys.effect] = s.effect.name
        this[Keys.fade] = s.fade
        this[Keys.fadeColor] = s.fadeColor.name
        this[Keys.blueIntro] = s.blueIntro
        this[Keys.interval] = s.interval.name
        this[Keys.booth] = s.booth
        this[Keys.keepOriginals] = s.keepOriginals
        this[Keys.onboardingDone] = s.onboardingDone
        this[Keys.tapeSpeed] = s.tapeSpeed.name
        this[Keys.wear] = s.vhs.wear
        this[Keys.colorBleed] = s.vhs.colorBleed
        this[Keys.noise] = s.vhs.noise
        this[Keys.tracking] = s.vhs.tracking
        this[Keys.jitter] = s.vhs.jitter
        this[Keys.sharpen] = s.vhs.sharpen
        this[Keys.headSwitch] = s.vhs.headSwitch
        this[Keys.scanlines] = s.vhs.scanlines
        this[Keys.vignette] = s.vhs.vignette
        this[Keys.ccdIntensity] = s.ccdIntensity
        this[Keys.filmIntensity] = s.filmIntensity
        this[Keys.polaroidIntensity] = s.polaroidIntensity
        this[Keys.aspect] = s.aspect.name
        this[Keys.videoQuality] = s.videoQuality.name
        this[Keys.flash] = s.flash.name
        this[Keys.timer] = s.timer.name
        this[Keys.grid] = s.grid
        this[Keys.dateStamp] = s.dateStamp
        this[Keys.timeStamp] = s.timeStamp
        this[Keys.dateOrder] = s.dateOrder.name
        this[Keys.clockStyle] = s.clockStyle.name
        val offset = s.clockOffsetMillis
        if (offset != null) this[Keys.clockOffset] = offset else remove(Keys.clockOffset)
        this[Keys.playOsd] = s.playOsd
        this[Keys.counterOsd] = s.counterOsd
        this[Keys.microphone] = s.microphone
        this[Keys.tapeAudio] = s.tapeAudio
        this[Keys.mirrorSelfies] = s.mirrorSelfies
        this[Keys.shutterSound] = s.shutterSound
        this[Keys.haptics] = s.haptics
        this[Keys.dynamicColor] = s.dynamicColor
    }
}
