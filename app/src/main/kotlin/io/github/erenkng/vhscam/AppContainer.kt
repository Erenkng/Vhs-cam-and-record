package io.github.erenkng.vhscam

import android.content.Context
import android.content.Intent
import io.github.erenkng.vhscam.audio.TapeAudioExporter
import io.github.erenkng.vhscam.camera.PhotoPipeline
import io.github.erenkng.vhscam.camera.VideoPipeline
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.gl.StillRenderer
import io.github.erenkng.vhscam.launch.LaunchDestination
import io.github.erenkng.vhscam.launch.LaunchIntents
import io.github.erenkng.vhscam.media.FilmRollStore
import io.github.erenkng.vhscam.media.FrameSequenceEncoder
import io.github.erenkng.vhscam.media.GifMaker
import io.github.erenkng.vhscam.media.MediaRepository
import io.github.erenkng.vhscam.media.OriginalsStore
import io.github.erenkng.vhscam.media.StopMotionReel
import io.github.erenkng.vhscam.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** App-wide singletons, created once in [VhsCamApp]. */
class AppContainer(context: Context) {

    /** Outlives screens: finishing a photo or a tape keeps going when the user leaves. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settings = SettingsRepository(context)
    val media = MediaRepository(context)
    val originals = OriginalsStore(context)
    val stillRenderer = StillRenderer(context)
    val photoPipeline = PhotoPipeline(context, stillRenderer, media, originals)
    val videoPipeline = VideoPipeline(context, TapeAudioExporter(context), media)
    val gifMaker = GifMaker(context, media)
    val rolls = FilmRollStore(context, media, originals)
    val reel = StopMotionReel(context, media, FrameSequenceEncoder(context))

    private val _hardwareShutter = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Volume-key presses while the camera is open. */
    val hardwareShutter: SharedFlow<Unit> = _hardwareShutter.asSharedFlow()

    /** Whether the camera screen is showing, so volume keys only act as a shutter there. */
    @Volatile
    var cameraVisible: Boolean = false

    /** The mode a launch from outside the app asked for, until the camera takes it up. */
    val launchMode = MutableStateFlow<CaptureMode?>(null)

    /** The screen a launch from outside the app asked for, until the app goes there. */
    val launchDestination = MutableStateFlow<LaunchDestination?>(null)

    /** Notes what [intent] (from the widget, the tile, a shortcut or another app) asks for. */
    fun onLaunch(intent: Intent?) {
        LaunchIntents.mode(intent)?.let { launchMode.value = it }
        LaunchIntents.destination(intent)?.let { launchDestination.value = it }
    }

    fun onHardwareShutter(): Boolean {
        if (!cameraVisible) return false
        _hardwareShutter.tryEmit(Unit)
        return true
    }
}
