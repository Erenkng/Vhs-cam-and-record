package io.github.erenkng.vhscam.gl

import android.content.res.AssetManager
import android.opengl.GLES30

/** A linked shader program with cached uniform locations. Missing uniforms are ignored. */
class GlProgram(vertexSource: String, fragmentSource: String, private val name: String) {

    val id: Int
    private val locations = HashMap<String, Int>()

    init {
        val vs = compile(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        id = GLES30.glCreateProgram()
        GLES30.glAttachShader(id, vs)
        GLES30.glAttachShader(id, fs)
        GLES30.glBindAttribLocation(id, 0, "aPosition")
        GLES30.glLinkProgram(id)
        val status = IntArray(1)
        GLES30.glGetProgramiv(id, GLES30.GL_LINK_STATUS, status, 0)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(id)
            GLES30.glDeleteProgram(id)
            error("Linking $name failed: $log")
        }
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            error("Compiling $name failed: $log")
        }
        return shader
    }

    fun use() = GLES30.glUseProgram(id)

    private fun location(uniform: String): Int =
        locations.getOrPut(uniform) { GLES30.glGetUniformLocation(id, uniform) }

    fun set(uniform: String, value: Float) {
        val l = location(uniform)
        if (l >= 0) GLES30.glUniform1f(l, value)
    }

    fun set(uniform: String, x: Float, y: Float) {
        val l = location(uniform)
        if (l >= 0) GLES30.glUniform2f(l, x, y)
    }

    fun set(uniform: String, x: Float, y: Float, z: Float) {
        val l = location(uniform)
        if (l >= 0) GLES30.glUniform3f(l, x, y, z)
    }

    fun setUnsigned(uniform: String, value: Int) {
        val l = location(uniform)
        if (l >= 0) GLES30.glUniform1ui(l, value)
    }

    fun setMatrix(uniform: String, matrix: FloatArray) {
        val l = location(uniform)
        if (l >= 0) GLES30.glUniformMatrix4fv(l, 1, false, matrix, 0)
    }

    /** A 3 x 3 matrix given row-major, the way it reads in Kotlin. */
    fun setMatrix3RowMajor(uniform: String, rows: List<Float>) {
        val l = location(uniform)
        if (l < 0) return
        val columns = FloatArray(9) { i -> rows[(i % 3) * 3 + i / 3] }
        GLES30.glUniformMatrix3fv(l, 1, false, columns, 0)
    }

    fun setTexture(uniform: String, unit: Int, target: Int, texture: Int) {
        val l = location(uniform)
        if (l < 0) return
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(target, texture)
        GLES30.glUniform1i(l, unit)
    }

    fun release() = GLES30.glDeleteProgram(id)
}

/**
 * Loads the GLSL passes from assets/shaders. Every fragment shader gets common.glsl prepended;
 * the shader-lab (tools/shader-lab/lab.js) assembles sources the same way.
 */
class ShaderLibrary(private val assets: AssetManager) {

    private val sources = HashMap<String, String>()

    private fun source(name: String): String = sources.getOrPut(name) {
        assets.open("shaders/$name").bufferedReader().use { it.readText() }
    }

    fun program(fragment: String, defines: Set<String> = emptySet()): GlProgram {
        val header = buildString {
            append("#version 300 es\n")
            if ("INPUT_OES" in defines) append("#extension GL_OES_EGL_image_external_essl3 : require\n")
            defines.forEach { append("#define ").append(it).append(" 1\n") }
        }
        return GlProgram(
            vertexSource = header + source("quad.vert"),
            fragmentSource = header + source("common.glsl") + "\n" + source(fragment),
            name = fragment,
        )
    }
}
