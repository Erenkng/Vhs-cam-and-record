package io.github.erenkng.vhscam.core.geometry

import kotlin.math.abs

/**
 * A 2D affine transform `(x, y) -> (a*x + c*y + e, b*x + d*y + f)`.
 *
 * GL texture transforms (SurfaceTexture / CameraX `SurfaceOutput`) are 4x4 column-major matrices
 * that only ever act on `(s, t, 0, 1)`, so their useful part is exactly this affine map.
 */
data class Affine2(
    val a: Float = 1f,
    val b: Float = 0f,
    val c: Float = 0f,
    val d: Float = 1f,
    val e: Float = 0f,
    val f: Float = 0f,
) {
    val determinant: Float get() = a * d - b * c

    fun mapX(x: Float, y: Float): Float = a * x + c * y + e

    fun mapY(x: Float, y: Float): Float = b * x + d * y + f

    /** Returns `this ∘ other`: applies [other] first, then this. */
    operator fun times(other: Affine2): Affine2 = Affine2(
        a = a * other.a + c * other.b,
        b = b * other.a + d * other.b,
        c = a * other.c + c * other.d,
        d = b * other.c + d * other.d,
        e = a * other.e + c * other.f + e,
        f = b * other.e + d * other.f + f,
    )

    fun inverse(): Affine2 {
        val det = determinant
        require(abs(det) > 1e-12f) { "Affine2 is not invertible: $this" }
        val ia = d / det
        val ib = -b / det
        val ic = -c / det
        val id = a / det
        return Affine2(
            a = ia,
            b = ib,
            c = ic,
            d = id,
            e = -(ia * e + ic * f),
            f = -(ib * e + id * f),
        )
    }

    /** Writes this transform as a column-major 4x4 GL matrix. */
    fun toGlMatrix(out: FloatArray = FloatArray(16)): FloatArray {
        require(out.size >= 16)
        out.fill(0f)
        out[0] = a
        out[1] = b
        out[4] = c
        out[5] = d
        out[10] = 1f
        out[12] = e
        out[13] = f
        out[15] = 1f
        return out
    }

    companion object {
        val IDENTITY = Affine2()

        /** Reads the affine part of a column-major 4x4 GL texture matrix. */
        fun fromGlMatrix(m: FloatArray): Affine2 {
            require(m.size >= 16)
            return Affine2(a = m[0], b = m[1], c = m[4], d = m[5], e = m[12], f = m[13])
        }

        fun translate(x: Float, y: Float) = Affine2(e = x, f = y)

        fun scale(x: Float, y: Float) = Affine2(a = x, d = y)
    }
}
