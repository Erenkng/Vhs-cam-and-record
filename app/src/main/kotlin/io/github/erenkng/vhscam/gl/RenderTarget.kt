package io.github.erenkng.vhscam.gl

import android.opengl.GLES30
import java.nio.Buffer

/** An RGBA8 texture with a framebuffer attached, used for intermediate passes. */
class RenderTarget(val width: Int, val height: Int) {

    val texture: Int
    private val framebuffer: Int

    init {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        texture = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null as Buffer?,
        )
        setLinearClamp(GLES30.GL_TEXTURE_2D)
        GLES30.glGenFramebuffers(1, ids, 0)
        framebuffer = ids[0]
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, texture, 0,
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        check(status == GLES30.GL_FRAMEBUFFER_COMPLETE) { "Framebuffer incomplete: $status" }
    }

    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)
        GLES30.glViewport(0, 0, width, height)
    }

    fun release() {
        GLES30.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        GLES30.glDeleteTextures(1, intArrayOf(texture), 0)
    }

    companion object {
        fun setLinearClamp(target: Int) {
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        }
    }
}

/** Keeps a target of the requested size, recreating it only when the size changes. */
class TargetSlot {
    private var target: RenderTarget? = null

    fun get(width: Int, height: Int): RenderTarget {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val current = target
        if (current != null && current.width == w && current.height == h) return current
        current?.release()
        return RenderTarget(w, h).also { target = it }
    }

    fun peek(): RenderTarget? = target

    fun release() {
        target?.release()
        target = null
    }
}
