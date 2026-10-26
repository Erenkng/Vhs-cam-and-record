package io.github.erenkng.vhscam.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.Rational
import android.util.Size
import android.view.Display
import androidx.annotation.MainThread
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.TorchState
import androidx.camera.core.UseCase
import androidx.camera.core.ViewPort
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.common.util.concurrent.ListenableFuture
import io.github.erenkng.vhscam.core.look.AspectSetting
import io.github.erenkng.vhscam.core.look.CaptureMode
import io.github.erenkng.vhscam.gl.VhsCameraEffect
import io.github.erenkng.vhscam.gl.VhsSurfaceProcessor
import io.github.erenkng.vhscam.settings.FlashSetting
import io.github.erenkng.vhscam.settings.VideoQuality
import io.github.erenkng.vhscam.settings.WhiteBalance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ZoomInfo(val ratio: Float = 1f, val min: Float = 1f, val max: Float = 1f)

data class ExposureInfo(val index: Int = 0, val min: Int = 0, val max: Int = 0, val step: Float = 1f) {
    val supported: Boolean get() = max > min
}

data class CameraCapabilities(
    val hasFlash: Boolean = false,
    val hasFrontCamera: Boolean = true,
    val isFront: Boolean = false,
    /** Closest focus distance in diopters; 0 means the lens cannot be focused manually. */
    val minFocusDiopters: Float = 0f,
) {
    val manualFocus: Boolean get() = minFocusDiopters > 0f
}

/** What to bind. A change of any field means a new camera session. */
data class BindRequest(
    val mode: CaptureMode,
    val front: Boolean,
    val videoAspect: AspectSetting,
    val videoQuality: VideoQuality,
    val flash: FlashSetting,
    val targetRotation: Int,
    val displayRotation: Int,
)

/** A captured photo: JPEG bytes (or a bitmap if the device delivered another format). */
class CapturedPhoto(
    val jpeg: ByteArray?,
    val bitmap: Bitmap?,
    val rotationDegrees: Int,
    val front: Boolean,
)

sealed interface RecordTarget {
    data class ToFile(val file: File) : RecordTarget
    data class ToMediaStore(val options: MediaStoreOutputOptions) : RecordTarget
}

/**
 * Owns the CameraX session: binds preview + photo or video capture with the VHS effect,
 * and exposes zoom, exposure, focus, flash, torch and manual Camera2 controls.
 */
class CameraEngine(private val context: Context, private val processor: VhsSurfaceProcessor) {

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    private val captureExecutor = Executors.newSingleThreadExecutor()

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var observedInfo: CameraInfo? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    private val _zoom = MutableStateFlow(ZoomInfo())
    val zoom: StateFlow<ZoomInfo> = _zoom.asStateFlow()

    private val _exposure = MutableStateFlow(ExposureInfo())
    val exposure: StateFlow<ExposureInfo> = _exposure.asStateFlow()

    private val _capabilities = MutableStateFlow(CameraCapabilities())
    val capabilities: StateFlow<CameraCapabilities> = _capabilities.asStateFlow()

    private val _torchOn = MutableStateFlow(false)
    val torchOn: StateFlow<Boolean> = _torchOn.asStateFlow()

    private val zoomObserver = Observer<ZoomState> { state ->
        _zoom.value = ZoomInfo(state.zoomRatio, state.minZoomRatio, state.maxZoomRatio)
    }
    private val torchObserver = Observer<Int> { _torchOn.value = it == TorchState.ON }

    private val preview: Preview = Preview.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build(),
        )
        .build()
        .also { preview -> preview.setSurfaceProvider { request -> _surfaceRequest.value = request } }

    val isRecording: Boolean get() = recording != null

    /** Binds a new session. Must be called on the main thread. */
    @MainThread
    suspend fun bind(owner: LifecycleOwner, request: BindRequest, onEffectError: (Throwable) -> Unit): CameraCapabilities {
        val cameraProvider = provider ?: ProcessCameraProvider.awaitInstance(context).also { provider = it }
        val hasFront = hasCamera(cameraProvider, CameraSelector.DEFAULT_FRONT_CAMERA)
        val hasBack = hasCamera(cameraProvider, CameraSelector.DEFAULT_BACK_CAMERA)
        val front = if (request.front) hasFront else !hasBack
        val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        preview.targetRotation = request.displayRotation
        val useCases = mutableListOf<UseCase>(preview)
        val effect: CameraEffect
        when (request.mode) {
            CaptureMode.PHOTO -> {
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(request.flash.toCameraX())
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                            .setResolutionStrategy(
                                ResolutionStrategy(PHOTO_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                            )
                            .build(),
                    )
                    .setTargetRotation(request.targetRotation)
                    .build()
                imageCapture = capture
                videoCapture = null
                useCases += capture
                effect = VhsCameraEffect(CameraEffect.PREVIEW, processor, onEffectError)
            }
            CaptureMode.VIDEO -> {
                val quality = request.videoQuality.toCameraX()
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(quality)))
                    .setAspectRatio(
                        if (request.videoAspect == AspectSetting.RATIO_16_9) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_4_3,
                    )
                    .build()
                val capture = VideoCapture.Builder(recorder)
                    .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
                    .setTargetRotation(request.targetRotation)
                    .build()
                videoCapture = capture
                imageCapture = null
                useCases += capture
                effect = VhsCameraEffect(CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE, processor, onEffectError)
            }
        }

        // Portrait view port so every output shares one crop (what you see is what you get).
        val portraitAspect = if (request.mode == CaptureMode.VIDEO && request.videoAspect == AspectSetting.RATIO_16_9) {
            Rational(9, 16)
        } else {
            Rational(3, 4)
        }
        val viewPort = ViewPort.Builder(portraitAspect, request.displayRotation)
            .setScaleType(ViewPort.FILL_CENTER)
            .build()

        cameraProvider.unbindAll()
        val bound = cameraProvider.bindToLifecycle(
            owner,
            selector,
            SessionConfig(useCases = useCases, viewPort = viewPort, effects = listOf(effect)),
        )
        attach(bound, owner, front, hasFront)
        return _capabilities.value
    }

    private fun hasCamera(provider: ProcessCameraProvider, selector: CameraSelector): Boolean =
        try {
            provider.hasCamera(selector)
        } catch (e: Exception) {
            false
        }

    private fun attach(bound: Camera, owner: LifecycleOwner, front: Boolean, hasFront: Boolean) {
        observedInfo?.let {
            it.zoomState.removeObserver(zoomObserver)
            it.torchState.removeObserver(torchObserver)
        }
        camera = bound
        val info = bound.cameraInfo
        info.zoomState.observe(owner, zoomObserver)
        info.torchState.observe(owner, torchObserver)
        observedInfo = info
        _exposure.value = info.exposureState.toInfo()
        _capabilities.value = CameraCapabilities(
            hasFlash = info.hasFlashUnit(),
            hasFrontCamera = hasFront,
            isFront = front,
            minFocusDiopters = minFocusDiopters(info),
        )
    }

    @MainThread
    fun unbind() {
        recording?.stop()
        recording = null
        observedInfo?.let {
            it.zoomState.removeObserver(zoomObserver)
            it.torchState.removeObserver(torchObserver)
        }
        observedInfo = null
        provider?.unbindAll()
        camera = null
    }

    /** Clockwise rotation that makes camera frames upright for a device rotation. */
    fun sensorRotationDegrees(rotation: Int): Int =
        camera?.cameraInfo?.getSensorRotationDegrees(rotation) ?: 90

    /** Keeps photos and videos upright for the way the phone is held. */
    fun setTargetRotation(rotation: Int) {
        imageCapture?.targetRotation = rotation
        if (recording == null) videoCapture?.targetRotation = rotation
    }

    fun setFlash(flash: FlashSetting) {
        imageCapture?.flashMode = flash.toCameraX()
    }

    fun setTorch(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
    }

    fun setZoom(ratio: Float) {
        val z = _zoom.value
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(z.min, z.max))
    }

    fun setExposure(index: Int) {
        val cam = camera ?: return
        val state = cam.cameraInfo.exposureState
        if (!state.isExposureCompensationSupported) return
        val range = state.exposureCompensationRange
        val clamped = index.coerceIn(range.lower, range.upper)
        cam.cameraControl.setExposureCompensationIndex(clamped)
        _exposure.value = _exposure.value.copy(index = clamped)
    }

    fun meteringPointFactory(display: Display, width: Float, height: Float): DisplayOrientedMeteringPointFactory? =
        camera?.cameraInfo?.let { DisplayOrientedMeteringPointFactory(display, it, width, height) }

    /**
     * Focuses and meters at [point]. With [lock] the result is held until the next tap
     * (AE/AF lock); without [autoFocus] only exposure and white balance are metered.
     * Returns whether focus was achieved.
     */
    suspend fun focus(point: MeteringPoint, lock: Boolean, autoFocus: Boolean): Boolean {
        val control = camera?.cameraControl ?: return false
        val flags = if (autoFocus) {
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        } else {
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        }
        val action = FocusMeteringAction.Builder(point, flags).apply {
            if (lock) disableAutoCancel() else setAutoCancelDuration(5, TimeUnit.SECONDS)
        }.build()
        return try {
            control.startFocusAndMetering(action).await(mainExecutor).isFocusSuccessful
        } catch (e: Exception) {
            false
        }
    }

    fun cancelFocus() {
        camera?.cameraControl?.cancelFocusAndMetering()
    }

    /** Manual focus distance in diopters (null = autofocus) and a white-balance preset. */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    fun setManualControls(focusDiopters: Float?, whiteBalance: WhiteBalance) {
        val control = camera?.cameraControl ?: return
        val options = CaptureRequestOptions.Builder().apply {
            if (focusDiopters != null) {
                setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focusDiopters)
            }
            whiteBalance.awbMode()?.let { setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, it) }
        }.build()
        Camera2CameraControl.from(control).setCaptureRequestOptions(options)
    }

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun minFocusDiopters(info: CameraInfo): Float = try {
        Camera2CameraInfo.from(info).getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
    } catch (e: IllegalArgumentException) {
        0f
    }

    suspend fun takePhoto(): CapturedPhoto {
        val capture = imageCapture ?: error("Not in photo mode")
        val front = _capabilities.value.isFront
        return suspendCancellableCoroutine { continuation ->
            capture.takePicture(captureExecutor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val result = try {
                        val rotation = image.imageInfo.rotationDegrees
                        if (image.format == ImageFormat.JPEG) {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                            CapturedPhoto(bytes, null, rotation, front)
                        } else {
                            CapturedPhoto(null, image.toBitmap(), rotation, front)
                        }
                    } finally {
                        image.close()
                    }
                    continuation.resume(result)
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    /** Starts recording. Audio is only recorded when asked for and permitted. */
    @SuppressLint("MissingPermission")
    fun startRecording(target: RecordTarget, withAudio: Boolean, onEvent: (VideoRecordEvent) -> Unit): Boolean {
        val capture = videoCapture ?: return false
        if (recording != null) return false
        val pending = when (target) {
            is RecordTarget.ToFile -> capture.output.prepareRecording(context, FileOutputOptions.Builder(target.file).build())
            is RecordTarget.ToMediaStore -> capture.output.prepareRecording(context, target.options)
        }
        val micAllowed = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val withMic = if (withAudio && micAllowed) pending.withAudioEnabled() else pending
        recording = withMic.start(mainExecutor) { event ->
            if (event is VideoRecordEvent.Finalize) recording = null
            onEvent(event)
        }
        return true
    }

    fun pauseRecording() {
        recording?.pause()
    }

    fun resumeRecording() {
        recording?.resume()
    }

    fun stopRecording() {
        recording?.stop()
    }

    fun release() {
        captureExecutor.shutdown()
    }

    companion object {
        private val PHOTO_SIZE = Size(2560, 1920)

        private fun FlashSetting.toCameraX(): Int = when (this) {
            FlashSetting.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashSetting.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashSetting.ON -> ImageCapture.FLASH_MODE_ON
        }

        private fun VideoQuality.toCameraX(): Quality = when (this) {
            VideoQuality.SD -> Quality.SD
            VideoQuality.HD -> Quality.HD
            VideoQuality.FHD -> Quality.FHD
        }

        private fun WhiteBalance.awbMode(): Int? = when (this) {
            WhiteBalance.AUTO -> null
            WhiteBalance.DAYLIGHT -> CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT
            WhiteBalance.CLOUDY -> CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
            WhiteBalance.INCANDESCENT -> CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT
            WhiteBalance.FLUORESCENT -> CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT
        }

        private fun ExposureState.toInfo(): ExposureInfo {
            val range = exposureCompensationRange
            return ExposureInfo(
                index = exposureCompensationIndex,
                min = range.lower,
                max = range.upper,
                step = exposureCompensationStep.toFloat(),
            )
        }
    }
}

/** Suspends until the future completes, delivering the result on [executor]. */
suspend fun <T> ListenableFuture<T>.await(executor: Executor): T = suspendCancellableCoroutine { continuation ->
    addListener({
        try {
            continuation.resume(get())
        } catch (e: ExecutionException) {
            continuation.resumeWithException(e.cause ?: e)
        } catch (e: java.util.concurrent.CancellationException) {
            continuation.cancel(e)
        }
    }, executor)
    continuation.invokeOnCancellation { cancel(false) }
}
