package io.github.erenkng.vhscam.ui.camera

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.net.Uri
import android.util.Log
import android.view.Display
import android.view.Surface
import androidx.camera.core.CameraInfo
import androidx.camera.core.SurfaceRequest
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.erenkng.vhscam.AppContainer
import io.github.erenkng.vhscam.VhsCamApp
import io.github.erenkng.vhscam.camera.BindRequest
import io.github.erenkng.vhscam.camera.CameraCapabilities
import io.github.erenkng.vhscam.camera.CameraEngine
import io.github.erenkng.vhscam.camera.CapturedPhoto
import io.github.erenkng.vhscam.camera.DeviceOrientation
import io.github.erenkng.vhscam.camera.ExposureInfo
import io.github.erenkng.vhscam.camera.LookFactory
import io.github.erenkng.vhscam.camera.RecordTarget
import io.github.erenkng.vhscam.camera.ZoomInfo
import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CamcorderEffect
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.core.look.FadeColor
import io.github.erenkng.vhscam.core.look.IntervalSetting
import io.github.erenkng.vhscam.core.look.Look
import io.github.erenkng.vhscam.core.look.TapeSpeed
import io.github.erenkng.vhscam.core.media.FileNames
import io.github.erenkng.vhscam.gl.LiveRenderState
import io.github.erenkng.vhscam.gl.VhsSurfaceProcessor
import io.github.erenkng.vhscam.media.CapturedMedia
import io.github.erenkng.vhscam.settings.AppSettings
import io.github.erenkng.vhscam.settings.FlashSetting
import io.github.erenkng.vhscam.settings.TimerSetting
import io.github.erenkng.vhscam.settings.VideoQuality
import io.github.erenkng.vhscam.settings.WhiteBalance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import kotlin.random.Random

enum class FocusStatus { FOCUSING, FOCUSED, FAILED }

/** The focus indicator: where the user tapped (viewfinder pixels) and how it went. */
data class FocusUi(val x: Float, val y: Float, val status: FocusStatus, val locked: Boolean, val id: Int)

sealed interface RecordingUi {
    data object Idle : RecordingUi
    data class Active(val elapsedMillis: Long, val paused: Boolean) : RecordingUi
}

enum class BusyKind { DEVELOPING, TAPE_AUDIO }

data class BusyUi(val kind: BusyKind, val progress: Float? = null)

/** Photobooth progress: [shot] of [total] is being taken next. */
data class BoothUi(val shot: Int, val total: Int)

/** Interval recording: seconds until the next snippet, or null while one is being recorded. */
data class IntervalUi(val nextInSeconds: Int?)

/** Snackbar messages. A saved photo needs none: the gallery thumbnail pops in instead. */
enum class CameraMessage { VIDEO_SAVED, CAPTURE_FAILED, RECORDING_FAILED, CAMERA_ERROR, DUAL_UNAVAILABLE }

/** Session-defining settings; a change requires binding the camera again. */
data class BindKey(val mode: CaptureMode, val front: Boolean, val aspect: AspectSetting?, val quality: VideoQuality?)

data class CameraUiState(
    val settings: AppSettings = AppSettings(),
    val settingsLoaded: Boolean = false,
    val zoom: ZoomInfo = ZoomInfo(),
    /** Camcorder digital zoom on top of the camera's zoom (VHS look only); 1 is off. */
    val digitalZoom: Float = 1f,
    val exposure: ExposureInfo = ExposureInfo(),
    val capabilities: CameraCapabilities = CameraCapabilities(),
    val torchOn: Boolean = false,
    val rotation: Int = Surface.ROTATION_0,
    val recording: RecordingUi = RecordingUi.Idle,
    val busy: BusyUi? = null,
    val countdown: Int? = null,
    val focus: FocusUi? = null,
    /** Manual focus distance in diopters, or null for autofocus. */
    val manualFocus: Float? = null,
    val whiteBalance: WhiteBalance = WhiteBalance.AUTO,
    val latest: CapturedMedia? = null,
    /** Incremented for every photo, drives the shutter blink. */
    val shutterCount: Int = 0,
    val message: CameraMessage? = null,
    /** Camera frames are arriving; until then the viewfinder shows the VCR's blue screen. */
    val signal: Boolean = false,
    val booth: BoothUi? = null,
    val interval: IntervalUi? = null,
) {
    val isRecording: Boolean get() = recording is RecordingUi.Active
    val isFilming: Boolean get() = settings.mode != CaptureMode.PHOTO
    val activeLook: Look get() = LookFactory.activeLook(settings)

    /** Zoom as the user sees it: the camera's zoom times the digital zoom. */
    val effectiveZoom: Float get() = zoom.ratio * digitalZoom

    /** Like 90s camcorders, the VHS look keeps zooming digitally past the lens. */
    val digitalZoomAvailable: Boolean get() = activeLook == Look.VHS

    val bindKey: BindKey
        get() = BindKey(
            mode = settings.mode,
            front = settings.frontCamera,
            aspect = settings.aspect.takeIf { isFilming },
            quality = settings.videoQuality.takeIf { isFilming },
        )
}

class CameraViewModel(
    application: Application,
    private val container: AppContainer,
) : AndroidViewModel(application) {

    private val processor = VhsSurfaceProcessor(application)
    private val engine = CameraEngine(application, processor)
    private val orientation = DeviceOrientation(application)
    private val sounds = MediaActionSound()
    private val sessionSeed = Random.nextFloat()

    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    val surfaceRequest: StateFlow<SurfaceRequest?> = engine.surfaceRequest

    private var countdownJob: Job? = null
    private var boothJob: Job? = null
    private var intervalJob: Job? = null
    private var focusCounter = 0
    private var recordingRotation: Int? = null
    private var stopping = false
    private var developing = 0
    private var tapeProgress: Float? = null

    init {
        sounds.load(MediaActionSound.SHUTTER_CLICK)
        sounds.load(MediaActionSound.START_VIDEO_RECORDING)
        sounds.load(MediaActionSound.STOP_VIDEO_RECORDING)
        orientation.start()
        processor.onSignal = { flowing -> _state.update { it.copy(signal = flowing) } }

        viewModelScope.launch {
            container.settings.settings.collect { s ->
                _state.update {
                    val next = it.copy(settings = s, settingsLoaded = true)
                    if (next.digitalZoomAvailable) next else next.copy(digitalZoom = 1f)
                }
            }
        }
        viewModelScope.launch { engine.zoom.collect { z -> _state.update { it.copy(zoom = z) } } }
        viewModelScope.launch { engine.exposure.collect { e -> _state.update { it.copy(exposure = e) } } }
        viewModelScope.launch { engine.capabilities.collect { c -> _state.update { it.copy(capabilities = c) } } }
        viewModelScope.launch { engine.torchOn.collect { t -> _state.update { it.copy(torchOn = t) } } }
        viewModelScope.launch {
            orientation.rotation.collect { r ->
                _state.update { it.copy(rotation = r) }
                if (!engine.isRecording) engine.setTargetRotation(r)
            }
        }
        viewModelScope.launch { container.media.latest.collect { m -> _state.update { it.copy(latest = m) } } }
        viewModelScope.launch { container.media.loadAll() }
        viewModelScope.launch { container.hardwareShutter.collect { onShutter() } }
        viewModelScope.launch {
            val ticker = flow {
                while (true) {
                    emit(Unit)
                    delay(1_000)
                }
            }
            combine(_state, ticker) { s, _ -> s }.collect { pushRenderState(it) }
        }
    }

    private fun pushRenderState(s: CameraUiState) {
        if (!s.settingsLoaded) return
        val settings = s.settings
        val look = s.activeLook
        val rotation = recordingRotation ?: s.rotation
        val counter = (s.recording as? RecordingUi.Active)?.elapsedMillis ?: 0L
        processor.state = LiveRenderState(
            spec = LookFactory.spec(settings, look, sessionSeed),
            osd = LookFactory.osd(settings, look, LookFactory.clockTime(settings), counter),
            sensorRotationDegrees = engine.sensorRotationDegrees(rotation),
            lookAspect = LookFactory.lookAspect(settings),
            // While filming, CameraX mirrors the selfie output after the effect; pre-mirror the text.
            osdMirror = s.capabilities.isFront && s.isFilming,
            peaking = if (s.manualFocus != null && !s.isRecording) 1f else 0f,
            digitalZoom = s.digitalZoom,
        )
    }

    /** Binds the camera for the current settings. Call again whenever [CameraUiState.bindKey] changes. */
    suspend fun bindCamera(owner: LifecycleOwner, displayRotation: Int) {
        val s = _state.value
        val settings = s.settings
        val request = BindRequest(
            mode = settings.mode,
            front = settings.frontCamera,
            videoAspect = settings.aspect,
            videoQuality = settings.videoQuality,
            flash = settings.flash,
            targetRotation = s.rotation,
            displayRotation = displayRotation,
        )
        try {
            val capabilities = engine.bind(owner, request) { error ->
                Log.e(TAG, "Effect failed", error)
                viewModelScope.launch { message(CameraMessage.CAMERA_ERROR) }
            }
            _state.update { it.copy(capabilities = capabilities, manualFocus = null, focus = null, digitalZoom = 1f) }
            engine.setManualControls(null, _state.value.whiteBalance)
            if (settings.mode == CaptureMode.DUAL && !capabilities.dualCamera) {
                // Not supported after all: CameraX bound a single camera, so say so and switch.
                updateSettings { it.copy(mode = CaptureMode.VIDEO) }
                message(CameraMessage.DUAL_UNAVAILABLE)
            }
            pushRenderState(_state.value)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Binding failed", e)
            if (settings.mode == CaptureMode.DUAL) {
                updateSettings { it.copy(mode = CaptureMode.VIDEO) }
                message(CameraMessage.DUAL_UNAVAILABLE)
            } else {
                message(CameraMessage.CAMERA_ERROR)
            }
        }
    }

    fun onShutter() {
        val s = _state.value
        if (boothJob?.isActive == true) {
            boothJob?.cancel()
            return
        }
        if (countdownJob?.isActive == true) {
            countdownJob?.cancel()
            _state.update { it.copy(countdown = null) }
            return
        }
        when (s.settings.mode) {
            CaptureMode.PHOTO -> if (s.settings.booth) captureBooth() else withTimer(s.settings.timer) { capturePhoto() }
            CaptureMode.VIDEO, CaptureMode.DUAL -> if (s.isRecording) {
                stopRecording()
            } else {
                withTimer(s.settings.timer) { startRecording() }
            }
        }
    }

    private fun withTimer(timer: TimerSetting, action: () -> Unit) {
        if (timer.seconds == 0) {
            action()
            return
        }
        countdownJob = viewModelScope.launch {
            for (i in timer.seconds downTo 1) {
                _state.update { it.copy(countdown = i) }
                delay(1_000)
            }
            _state.update { it.copy(countdown = null) }
            action()
        }
    }

    private fun capturePhoto() {
        val settings = _state.value.settings
        playSound(MediaActionSound.SHUTTER_CLICK, settings)
        _state.update { it.copy(shutterCount = it.shutterCount + 1) }
        viewModelScope.launch {
            val photo = try {
                engine.takePhoto()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Capture failed", e)
                message(CameraMessage.CAPTURE_FAILED)
                return@launch
            }
            val nowMillis = System.currentTimeMillis()
            val capturedAt = LocalDateTime.now()
            val look = settings.photoLook
            val spec = LookFactory.spec(settings, look, Random.nextFloat())
            val osd = LookFactory.osd(settings, look, LookFactory.clockTime(settings, nowMillis), 0L)
            val lookAspect = LookFactory.photoAspect(settings, look)
            val mirror = photo.front && settings.mirrorSelfies
            // Developing continues if the user leaves the camera meanwhile.
            container.appScope.launch {
                developing++
                refreshBusy()
                val result = runCatching {
                    container.photoPipeline.develop(
                        photo, look, spec, osd, mirror, lookAspect, capturedAt, nowMillis,
                        keepOriginal = settings.keepOriginals,
                    )
                }
                developing--
                refreshBusy()
                result.exceptionOrNull()?.let {
                    Log.e(TAG, "Developing failed", it)
                    message(CameraMessage.CAPTURE_FAILED)
                }
            }
        }
    }

    /** Photobooth: four shots with a countdown before each, printed as one strip. */
    private fun captureBooth() {
        if (boothJob?.isActive == true) return
        val settings = _state.value.settings
        boothJob = viewModelScope.launch {
            val shots = mutableListOf<CapturedPhoto>()
            try {
                for (i in 0 until BOOTH_SHOTS) {
                    _state.update { it.copy(booth = BoothUi(shot = i + 1, total = BOOTH_SHOTS)) }
                    for (left in (if (i == 0) 3 else 2) downTo 1) {
                        _state.update { it.copy(countdown = left) }
                        delay(1_000)
                    }
                    _state.update { it.copy(countdown = null) }
                    playSound(MediaActionSound.SHUTTER_CLICK, settings)
                    _state.update { it.copy(shutterCount = it.shutterCount + 1) }
                    shots += engine.takePhoto()
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(booth = null, countdown = null) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photobooth capture failed", e)
                _state.update { it.copy(booth = null, countdown = null) }
                message(CameraMessage.CAPTURE_FAILED)
                return@launch
            }
            _state.update { it.copy(booth = null) }
            val nowMillis = System.currentTimeMillis()
            val capturedAt = LocalDateTime.now()
            val look = settings.photoLook
            val osd = LookFactory.osd(settings, look, LookFactory.clockTime(settings, nowMillis), 0L)
            container.appScope.launch {
                developing++
                refreshBusy()
                val result = runCatching {
                    container.photoPipeline.developStrip(
                        photos = shots,
                        look = look,
                        specFor = { LookFactory.spec(settings, look, Random.nextFloat()) },
                        osd = osd,
                        mirrorSelfies = settings.mirrorSelfies,
                        lookAspect = LookFactory.photoAspect(settings, look),
                        capturedAt = capturedAt,
                        capturedAtMillis = nowMillis,
                    )
                }
                developing--
                refreshBusy()
                result.exceptionOrNull()?.let {
                    Log.e(TAG, "Developing the strip failed", it)
                    message(CameraMessage.CAPTURE_FAILED)
                }
            }
        }
    }

    private fun startRecording() {
        val s = _state.value
        val settings = s.settings
        val look = s.activeLook
        val nowMillis = System.currentTimeMillis()
        val capturedAt = LocalDateTime.now()
        val micGranted = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val withAudio = settings.microphone && micGranted
        // Tape audio belongs to the VHS look only.
        val file = if (settings.tapeAudio && withAudio && look == Look.VHS) container.videoPipeline.newRecordingFile() else null
        val target = if (file != null) {
            RecordTarget.ToFile(file)
        } else {
            RecordTarget.ToMediaStore(container.media.videoOutputOptions(FileNames.video(capturedAt, look), nowMillis))
        }
        recordingRotation = s.rotation
        val started = engine.startRecording(target, withAudio) { event ->
            onRecordingEvent(event, file, withAudio, look, settings.tapeSpeed, capturedAt, nowMillis)
        }
        if (!started) {
            recordingRotation = null
            message(CameraMessage.RECORDING_FAILED)
            return
        }
        playSound(MediaActionSound.START_VIDEO_RECORDING, settings)
        _state.update { it.copy(recording = RecordingUi.Active(elapsedMillis = 0L, paused = false)) }

        val white = settings.fadeColor == FadeColor.WHITE
        val blueIntro = look == Look.VHS && settings.blueIntro
        if (blueIntro) processor.showBlueScreen(BLUE_INTRO_MILLIS) else processor.triggerGlitch()
        if (settings.fade) {
            viewModelScope.launch {
                // The tape cuts from the deck's blue screen to black, then the picture fades in.
                if (blueIntro) delay(BLUE_INTRO_MILLIS)
                processor.fadeTo(1f, white, durationMillis = 0L)
                processor.fadeTo(0f, white, FADE_MILLIS)
            }
        }
        if (settings.interval != IntervalSetting.OFF) startInterval(settings.interval)
    }

    /** Interval REC: keep a one-second snippet, pause until the next one, repeat. */
    private fun startInterval(interval: IntervalSetting) {
        intervalJob?.cancel()
        intervalJob = viewModelScope.launch {
            while (true) {
                _state.update { it.copy(interval = IntervalUi(nextInSeconds = null)) }
                delay(SNIPPET_MILLIS)
                engine.pauseRecording()
                for (left in (interval.seconds - (SNIPPET_MILLIS / 1000).toInt()) downTo 1) {
                    _state.update { it.copy(interval = IntervalUi(nextInSeconds = left)) }
                    delay(1_000)
                }
                engine.resumeRecording()
            }
        }
    }

    private fun stopRecording() {
        val s = _state.value
        val active = s.recording as? RecordingUi.Active ?: return
        intervalJob?.cancel()
        if (s.settings.fade && !active.paused && !stopping) {
            // Like the camcorder's fader button: fade out first, then stop.
            stopping = true
            processor.fadeTo(1f, s.settings.fadeColor == FadeColor.WHITE, FADE_MILLIS)
            viewModelScope.launch {
                delay(FADE_MILLIS)
                engine.stopRecording()
            }
        } else {
            engine.stopRecording()
        }
    }

    private fun onRecordingEvent(
        event: VideoRecordEvent,
        file: File?,
        withAudio: Boolean,
        look: Look,
        speed: TapeSpeed,
        capturedAt: LocalDateTime,
        capturedAtMillis: Long,
    ) {
        when (event) {
            is VideoRecordEvent.Status -> _state.update { s ->
                val active = s.recording as? RecordingUi.Active ?: return@update s
                s.copy(recording = active.copy(elapsedMillis = event.recordingStats.recordedDurationNanos / 1_000_000))
            }
            is VideoRecordEvent.Pause -> _state.update { s ->
                val active = s.recording as? RecordingUi.Active ?: return@update s
                s.copy(recording = active.copy(paused = true))
            }
            is VideoRecordEvent.Resume -> {
                processor.triggerGlitch()
                _state.update { s ->
                    val active = s.recording as? RecordingUi.Active ?: return@update s
                    s.copy(recording = active.copy(paused = false))
                }
            }
            is VideoRecordEvent.Finalize -> {
                intervalJob?.cancel()
                stopping = false
                recordingRotation = null
                engine.setTargetRotation(_state.value.rotation)
                // Bring the viewfinder back if the recording faded out.
                processor.fadeTo(0f, _state.value.settings.fadeColor == FadeColor.WHITE, 350L)
                _state.update { it.copy(recording = RecordingUi.Idle, interval = null) }
                playSound(MediaActionSound.STOP_VIDEO_RECORDING, _state.value.settings)
                if (file != null) {
                    if (file.exists() && file.length() > 0) {
                        finishTape(file, withAudio, look, speed, capturedAt, capturedAtMillis)
                    } else {
                        Log.e(TAG, "Recording failed: ${event.error}", event.cause)
                        file.delete()
                        message(CameraMessage.RECORDING_FAILED)
                    }
                } else {
                    val uri = event.outputResults.outputUri
                    if (uri != Uri.EMPTY) {
                        viewModelScope.launch {
                            container.media.onVideoRecorded(uri)
                            message(CameraMessage.VIDEO_SAVED)
                        }
                    } else {
                        Log.e(TAG, "Recording failed: ${event.error}", event.cause)
                        message(CameraMessage.RECORDING_FAILED)
                    }
                }
            }
            else -> Unit
        }
    }

    private fun finishTape(file: File, withAudio: Boolean, look: Look, speed: TapeSpeed, capturedAt: LocalDateTime, millis: Long) {
        container.appScope.launch {
            tapeProgress = 0f
            refreshBusy()
            val result = runCatching {
                container.videoPipeline.finish(file, withAudio, look, speed, capturedAt, millis) { p ->
                    tapeProgress = p
                    refreshBusy()
                }
            }
            tapeProgress = null
            refreshBusy()
            result.exceptionOrNull()?.let { Log.e(TAG, "Saving the tape failed", it) }
            message(if (result.isSuccess) CameraMessage.VIDEO_SAVED else CameraMessage.RECORDING_FAILED)
        }
    }

    private fun refreshBusy() {
        val tape = tapeProgress
        val busy = when {
            tape != null -> BusyUi(BusyKind.TAPE_AUDIO, tape)
            developing > 0 -> BusyUi(BusyKind.DEVELOPING)
            else -> null
        }
        _state.update { it.copy(busy = busy) }
    }

    fun togglePause() {
        val s = _state.value
        val active = s.recording as? RecordingUi.Active ?: return
        // Interval recording pauses and resumes by itself.
        if (s.interval != null || stopping) return
        val white = s.settings.fadeColor == FadeColor.WHITE
        when {
            active.paused -> {
                if (s.settings.fade) processor.fadeTo(1f, white, durationMillis = 0L)
                engine.resumeRecording()
                if (s.settings.fade) processor.fadeTo(0f, white, FADE_MILLIS)
            }
            s.settings.fade -> {
                processor.fadeTo(1f, white, FADE_MILLIS)
                viewModelScope.launch {
                    delay(FADE_MILLIS)
                    engine.pauseRecording()
                    // Paused, nothing is recorded: show the picture again for framing the next shot.
                    processor.fadeTo(0f, white, 300L)
                }
            }
            else -> engine.pauseRecording()
        }
    }

    fun onTapFocus(x: Float, y: Float, width: Float, height: Float, display: Display, lock: Boolean) {
        val factory = engine.meteringPointFactory(display, width, height) ?: return
        val point = factory.createPoint(x, y)
        val id = ++focusCounter
        _state.update { it.copy(focus = FocusUi(x, y, FocusStatus.FOCUSING, lock, id)) }
        viewModelScope.launch {
            val ok = engine.focus(point, lock, autoFocus = _state.value.manualFocus == null)
            _state.update { s ->
                val f = s.focus
                if (f != null && f.id == id) {
                    s.copy(focus = f.copy(status = if (ok) FocusStatus.FOCUSED else FocusStatus.FAILED))
                } else {
                    s
                }
            }
            if (!lock) {
                delay(4_000)
                _state.update { s -> if (s.focus?.id == id) s.copy(focus = null) else s }
            }
        }
    }

    fun clearFocusLock() {
        engine.cancelFocus()
        _state.update { it.copy(focus = null) }
    }

    fun setExposure(index: Int) = engine.setExposure(index)

    /** Zooms the camera; past the lens the VHS look carries on with blocky digital zoom. */
    fun setZoom(ratio: Float) {
        val s = _state.value
        val lensMax = s.zoom.max
        if (s.digitalZoomAvailable && ratio > lensMax) {
            engine.setZoom(lensMax)
            _state.update { it.copy(digitalZoom = (ratio / lensMax).coerceIn(1f, DIGITAL_ZOOM_MAX)) }
        } else {
            engine.setZoom(ratio)
            _state.update { it.copy(digitalZoom = 1f) }
        }
    }

    fun setManualFocus(diopters: Float?) {
        _state.update { it.copy(manualFocus = diopters) }
        engine.setManualControls(diopters, _state.value.whiteBalance)
    }

    fun setWhiteBalance(whiteBalance: WhiteBalance) {
        _state.update { it.copy(whiteBalance = whiteBalance) }
        engine.setManualControls(_state.value.manualFocus, whiteBalance)
    }

    fun toggleTorch() = engine.setTorch(!_state.value.torchOn)

    fun setFlash(flash: FlashSetting) {
        engine.setFlash(flash)
        updateSettings { it.copy(flash = flash) }
    }

    fun setEffect(effect: CamcorderEffect) = updateSettings { it.copy(effect = effect) }

    fun setMode(mode: CaptureMode) {
        val s = _state.value
        if (s.isRecording || mode == s.settings.mode) return
        if (mode == CaptureMode.DUAL && !s.capabilities.dualCamera) return
        engine.setTorch(false)
        updateSettings { it.copy(mode = mode) }
    }

    fun flipCamera() {
        if (_state.value.isRecording) return
        updateSettings { it.copy(frontCamera = !it.frontCamera) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settings.update(transform) }
    }

    fun setCameraVisible(visible: Boolean) {
        container.cameraVisible = visible
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun message(message: CameraMessage) = _state.update { it.copy(message = message) }

    private fun playSound(sound: Int, settings: AppSettings) {
        if (settings.shutterSound || CameraInfo.mustPlayShutterSound()) sounds.play(sound)
    }

    override fun onCleared() {
        countdownJob?.cancel()
        boothJob?.cancel()
        intervalJob?.cancel()
        processor.onSignal = null
        orientation.stop()
        engine.unbind()
        engine.release()
        processor.release()
        sounds.release()
        container.cameraVisible = false
    }

    companion object {
        private const val TAG = "CameraViewModel"
        const val FADE_MILLIS = 1_000L
        const val BLUE_INTRO_MILLIS = 1_200L
        const val SNIPPET_MILLIS = 1_000L
        const val BOOTH_SHOTS = 4

        /** How far the digital zoom goes past the lens. */
        const val DIGITAL_ZOOM_MAX = 10f

        fun factory(app: VhsCamApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { CameraViewModel(app, app.container) }
        }
    }
}
