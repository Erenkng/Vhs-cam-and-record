package io.github.erenkng.vhscam.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.core.util.Consumer
import io.github.erenkng.vhscam.core.geometry.Affine2
import io.github.erenkng.vhscam.core.geometry.OrientationMath
import io.github.erenkng.vhscam.core.geometry.UprightMapping
import java.util.concurrent.Executor
import kotlin.math.max
import kotlin.math.roundToInt

/** What the live pipeline should render. Swapped atomically from the main thread. */
data class LiveRenderState(
    val spec: LookSpec,
    val osd: OsdContent = OsdContent(),
    /** Clockwise rotation that makes the camera buffer upright for the physical orientation. */
    val sensorRotationDegrees: Int = 90,
    /** Width / height of the look's own crop (landscape), or null to use the full frame. */
    val lookAspect: Float? = null,
    /** Draw the OSD mirrored because CameraX mirrors the output afterwards (selfie video). */
    val osdMirror: Boolean = false,
    val peaking: Float = 0f,
)

/**
 * The live effect: receives camera frames from CameraX, renders the selected look with
 * [LookRenderer] and draws the result to every output surface (preview, video encoder).
 *
 * All GL work happens on a private thread, which is also the executor CameraX uses to call us.
 */
class VhsSurfaceProcessor(private val context: Context) : SurfaceProcessor {

    private val thread = HandlerThread("VhsGl").apply { start() }
    private val handler = Handler(thread.looper)
    val executor: Executor = Executor { handler.post(it) }

    @Volatile
    var state: LiveRenderState? = null

    @Volatile
    private var glitchFrames = 0

    private var egl: EglCore? = null
    private var renderer: LookRenderer? = null
    private var osd: OsdLayers? = null

    private var inputTexture = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var inputSize = Size(1, 1)
    private val outputs = LinkedHashMap<SurfaceOutput, EGLSurface>()

    private val textureMatrix = FloatArray(16)
    private val outputMatrix = FloatArray(16)
    private var firstTimestamp = -1L
    private var frameCount = 0
    private var released = false

    /** Adds a few frames of tape tearing, like a camcorder starting or resuming a recording. */
    fun triggerGlitch(frames: Int = 9) {
        glitchFrames = frames
    }

    private fun ensureGl(): Boolean {
        if (egl != null) return true
        return try {
            val core = EglCore()
            egl = core
            renderer = LookRenderer(ShaderLibrary(context.assets), oesInput = true)
            osd = OsdLayers(OsdPainter(context))
            true
        } catch (e: RuntimeException) {
            Log.e(TAG, "Could not set up GL", e)
            false
        }
    }

    override fun onInputSurface(request: SurfaceRequest) {
        if (released || !ensureGl()) {
            request.willNotProvideSurface()
            return
        }
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        val texture = ids[0]
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        RenderTarget.setLinearClamp(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)

        val st = SurfaceTexture(texture)
        st.setDefaultBufferSize(request.resolution.width, request.resolution.height)
        val surface = Surface(st)
        request.provideSurface(surface, executor) {
            st.setOnFrameAvailableListener(null)
            st.release()
            surface.release()
            GLES30.glDeleteTextures(1, intArrayOf(texture), 0)
            if (surfaceTexture === st) {
                surfaceTexture = null
                inputTexture = 0
            }
        }
        st.setOnFrameAvailableListener({ onFrameAvailable(it) }, handler)
        surfaceTexture = st
        inputTexture = texture
        inputSize = request.resolution
        firstTimestamp = -1L
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        if (released || !ensureGl()) {
            surfaceOutput.close()
            return
        }
        val surface = surfaceOutput.getSurface(executor, Consumer { event ->
            if (event.eventCode == SurfaceOutput.Event.EVENT_REQUEST_CLOSE) {
                outputs.remove(surfaceOutput)?.let { eglSurface ->
                    egl?.makeDefaultCurrent()
                    egl?.releaseSurface(eglSurface)
                }
                surfaceOutput.close()
            }
        })
        val eglSurface = try {
            egl!!.createWindowSurface(surface)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Could not wrap output surface", e)
            surfaceOutput.close()
            return
        }
        outputs[surfaceOutput] = eglSurface
    }

    private fun onFrameAvailable(st: SurfaceTexture) {
        if (released || st !== surfaceTexture) return
        val core = egl ?: return
        val renderer = renderer ?: return
        val osd = osd ?: return
        core.makeDefaultCurrent()
        st.updateTexImage()
        val current = state
        if (outputs.isEmpty() || current == null) return
        st.getTransformMatrix(textureMatrix)
        val surfaceTransform = Affine2.fromGlMatrix(textureMatrix)

        val timestamp = st.timestamp
        if (firstTimestamp < 0) firstTimestamp = timestamp
        val seconds = (timestamp - firstTimestamp) / 1_000_000_000f
        val glitch = glitchFrames
        if (glitch > 0) glitchFrames = glitch - 1
        val spec = (current.spec as? LookSpec.Vhs)?.let { it.copy(glitch = glitch / 9f) } ?: current.spec

        // Process once, in the upright space of the first output.
        val (first, _) = outputs.entries.first()
        first.updateTransformMatrix(outputMatrix, textureMatrix)
        val mapping = withLookCrop(
            OrientationMath.fit(
                surfaceTexture = surfaceTransform,
                output = Affine2.fromGlMatrix(outputMatrix),
                bufferWidth = inputSize.width,
                bufferHeight = inputSize.height,
                sensorRotationDegrees = current.sensorRotationDegrees,
            ),
            current.lookAspect,
        )
        val workHeight = if (spec is LookSpec.Vhs) VHS_LINES else PHOTO_PREVIEW_LINES
        val workWidth = max(4, (workHeight * mapping.uprightAspect).roundToInt())
        osd.update(
            look = spec.kind,
            content = current.osd,
            burnedWidth = workWidth,
            burnedHeight = workHeight,
            overlayWidth = max(4, (OVERLAY_LINES * mapping.uprightAspect).roundToInt()),
            overlayHeight = OVERLAY_LINES,
        )
        renderer.process(
            inputTexture = inputTexture,
            uprightToTexture = mapping.uprightToTexture,
            workWidth = workWidth,
            workHeight = workHeight,
            spec = spec,
            time = seconds,
            frame = frameCount,
            osd = osd,
            osdMirror = current.osdMirror,
        )

        for ((output, eglSurface) in outputs) {
            output.updateTransformMatrix(outputMatrix, textureMatrix)
            val outputMapping = withLookCrop(
                OrientationMath.fit(
                    surfaceTexture = surfaceTransform,
                    output = Affine2.fromGlMatrix(outputMatrix),
                    bufferWidth = inputSize.width,
                    bufferHeight = inputSize.height,
                    sensorRotationDegrees = current.sensorRotationDegrees,
                ),
                current.lookAspect,
            )
            try {
                core.makeCurrent(eglSurface)
                val size = output.size
                renderer.present(
                    outputToUpright = outputMapping.outputToUpright,
                    width = size.width,
                    height = size.height,
                    spec = spec,
                    osd = osd,
                    osdMirror = current.osdMirror,
                    // The camera screen turns peaking off while recording, so it never ends up in a video.
                    peaking = current.peaking,
                )
                core.setPresentationTime(eglSurface, timestamp)
                core.swapBuffers(eglSurface)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Dropping a frame for an output that went away", e)
            }
        }
        core.makeDefaultCurrent()
        frameCount++
    }

    /** Restricts the upright frame to a centred crop with the look's aspect ratio. */
    private fun withLookCrop(mapping: UprightMapping, lookAspect: Float?): UprightMapping {
        if (lookAspect == null) return mapping
        val target = if (mapping.uprightAspect < 1f) 1f / lookAspect else lookAspect
        val crop = centreCrop(mapping.uprightAspect, target) ?: return mapping
        return mapping.copy(
            uprightToTexture = mapping.uprightToTexture * crop,
            outputToUpright = crop.inverse() * mapping.outputToUpright,
            uprightAspect = target,
        )
    }

    fun release() {
        handler.post {
            if (released) return@post
            released = true
            surfaceTexture?.setOnFrameAvailableListener(null)
            egl?.let { core ->
                core.makeDefaultCurrent()
                outputs.forEach { (output, eglSurface) ->
                    core.releaseSurface(eglSurface)
                    output.close()
                }
                outputs.clear()
                renderer?.release()
                osd?.release()
                core.release()
            }
            egl = null
            thread.quitSafely()
        }
    }

    companion object {
        private const val TAG = "VhsSurfaceProcessor"
        const val VHS_LINES = 480
        const val PHOTO_PREVIEW_LINES = 720
        const val OVERLAY_LINES = 720

        /**
         * Maps uv of a centred crop with aspect [target] to uv of a frame with aspect [source],
         * or null when they already match.
         */
        fun centreCrop(source: Float, target: Float): Affine2? {
            if (kotlin.math.abs(source - target) < 1e-3f) return null
            return if (target < source) {
                val sx = target / source
                Affine2(a = sx, d = 1f, e = (1f - sx) / 2f, f = 0f)
            } else {
                val sy = source / target
                Affine2(a = 1f, d = sy, e = 0f, f = (1f - sy) / 2f)
            }
        }
    }
}
