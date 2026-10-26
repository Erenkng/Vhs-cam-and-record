package io.github.erenkng.vhscam.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.view.Surface

/**
 * An EGL display and GLES 3 context. Use it only from the thread that created it.
 *
 * The context is kept current on a 1x1 pbuffer whenever no output surface is bound, so
 * textures and framebuffers can be touched at any time.
 */
class EglCore {
    private val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val config: EGLConfig
    private val context: EGLContext
    private val placeholder: EGLSurface

    init {
        check(display != EGL14.EGL_NO_DISPLAY) { "No EGL display" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
        // Encoder surfaces need EGL_RECORDABLE_ANDROID; fall back if a device lacks such a config.
        config = chooseConfig(recordable = true) ?: chooseConfig(recordable = false)
            ?: error("No suitable EGL config")
        context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
            0,
        )
        check(context != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed: ${EGL14.eglGetError()}" }
        placeholder = createPbufferSurface(1, 1)
        makeDefaultCurrent()
    }

    private fun chooseConfig(recordable: Boolean): EGLConfig? {
        val attributes = mutableListOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT,
        )
        if (recordable) {
            attributes += EGLExt.EGL_RECORDABLE_ANDROID
            attributes += 1
        }
        attributes += EGL14.EGL_NONE
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        val ok = EGL14.eglChooseConfig(display, attributes.toIntArray(), 0, configs, 0, 1, count, 0)
        return if (ok && count[0] > 0) configs[0] else null
    }

    fun createWindowSurface(surface: Surface): EGLSurface {
        val eglSurface = EGL14.eglCreateWindowSurface(
            display, config, surface, intArrayOf(EGL14.EGL_NONE), 0,
        )
        check(eglSurface != EGL14.EGL_NO_SURFACE) {
            "eglCreateWindowSurface failed: ${EGL14.eglGetError()}"
        }
        return eglSurface
    }

    fun createPbufferSurface(width: Int, height: Int): EGLSurface {
        val eglSurface = EGL14.eglCreatePbufferSurface(
            display,
            config,
            intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE),
            0,
        )
        check(eglSurface != EGL14.EGL_NO_SURFACE) {
            "eglCreatePbufferSurface failed: ${EGL14.eglGetError()}"
        }
        return eglSurface
    }

    fun makeCurrent(surface: EGLSurface) {
        check(EGL14.eglMakeCurrent(display, surface, surface, context)) {
            "eglMakeCurrent failed: ${EGL14.eglGetError()}"
        }
    }

    fun makeDefaultCurrent() = makeCurrent(placeholder)

    fun swapBuffers(surface: EGLSurface): Boolean = EGL14.eglSwapBuffers(display, surface)

    fun setPresentationTime(surface: EGLSurface, nanos: Long) {
        EGLExt.eglPresentationTimeANDROID(display, surface, nanos)
    }

    fun releaseSurface(surface: EGLSurface) {
        EGL14.eglDestroySurface(display, surface)
    }

    /**
     * Releases the context. The display is left initialised on purpose: other EglCore
     * instances in the process share it.
     */
    fun release() {
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, placeholder)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglReleaseThread()
    }
}
