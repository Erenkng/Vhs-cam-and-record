package io.github.erenkng.vhscam.core.geometry

/**
 * How the "upright" processing space maps onto one camera output.
 *
 * VHS artefacts are horizontal relative to the viewer (scanlines, tracking bands, chroma smear),
 * so every look is rendered in a frame that is upright for the device's physical orientation.
 * CameraX may hand the effect an output that is still in sensor orientation (stream sharing) or
 * already rotated/mirrored (single preview target); both cases are absorbed here.
 */
data class UprightMapping(
    /** Upright uv -> camera texture coordinates. Used by the input pass. */
    val uprightToTexture: Affine2,
    /** Output-surface uv -> upright uv. Used by the final pass to sample the processed frame. */
    val outputToUpright: Affine2,
    /** Width / height of the upright frame. */
    val uprightAspect: Float,
    /** Whether the upright frame is a mirror image (front camera selfie view). */
    val mirrored: Boolean,
)

object OrientationMath {

    /**
     * The work happens in the camera buffer as the sensor wrote it ("raw" uv, y up), which the
     * texture stores upside down: texture = [FLIP_Y] of raw uv. The SurfaceTexture transform is
     * not used for this on purpose: on top of that flip it usually carries the camera's own
     * rotation to the phone's natural orientation (and the selfie mirror), which CameraX
     * predicts from the sensor orientation. Treating that rotated space as the sensor's turned
     * the processing frame a second time, so date stamps and tape lines came out sideways.
     *
     * @param output the transform from `SurfaceOutput.updateTransformMatrix`: output uv ->
     *   texture coordinates (CameraX's crop, rotation and mirroring included).
     * @param bufferWidth width of the camera buffer, in sensor orientation.
     * @param bufferHeight height of the camera buffer, in sensor orientation.
     * @param sensorRotationDegrees clockwise rotation that makes the buffer image upright for the
     *   device's physical orientation (`CameraInfo.getSensorRotationDegrees(physicalRotation)`).
     */
    fun fit(
        output: Affine2,
        bufferWidth: Int,
        bufferHeight: Int,
        sensorRotationDegrees: Int,
    ): UprightMapping {
        // Output uv -> raw buffer uv.
        val additional = FLIP_Y.inverse() * output

        // Region of the buffer that this output shows (CameraX crop / view port).
        val xs = floatArrayOf(
            additional.mapX(0f, 0f), additional.mapX(1f, 0f),
            additional.mapX(0f, 1f), additional.mapX(1f, 1f),
        )
        val ys = floatArrayOf(
            additional.mapY(0f, 0f), additional.mapY(1f, 0f),
            additional.mapY(0f, 1f), additional.mapY(1f, 1f),
        )
        val s0 = xs.min()
        val s1 = xs.max()
        val t0 = ys.min()
        val t1 = ys.max()
        val crop = Affine2(a = s1 - s0, d = t1 - t0, e = s0, f = t0)

        val rotation = normalizeDegrees(sensorRotationDegrees)
        val mirrored = additional.determinant < 0f
        val mirror = if (mirrored) MIRROR_U else Affine2.IDENTITY

        val uprightToBuffer = crop * uprightToRotated(rotation) * mirror
        val cropWidthPx = (s1 - s0) * bufferWidth
        val cropHeightPx = (t1 - t0) * bufferHeight
        val aspect = if (rotation % 180 == 0) {
            cropWidthPx / cropHeightPx
        } else {
            cropHeightPx / cropWidthPx
        }
        return UprightMapping(
            uprightToTexture = FLIP_Y * uprightToBuffer,
            outputToUpright = uprightToBuffer.inverse() * additional,
            uprightAspect = aspect,
            mirrored = mirrored,
        )
    }

    /**
     * Maps upright uv to the unit square of an image that has to be rotated clockwise by
     * [rotation] degrees to become upright. Coordinates use the GL convention (y up).
     */
    fun uprightToRotated(rotation: Int): Affine2 = when (normalizeDegrees(rotation)) {
        0 -> Affine2.IDENTITY
        // Clockwise 90: buffer (s, t) -> upright (t, 1 - s); inverse s = 1 - v, t = u.
        90 -> Affine2(a = 0f, b = 1f, c = -1f, d = 0f, e = 1f, f = 0f)
        180 -> Affine2(a = -1f, b = 0f, c = 0f, d = -1f, e = 1f, f = 1f)
        // Clockwise 270: buffer (s, t) -> upright (1 - t, s); inverse s = v, t = 1 - u.
        270 -> Affine2(a = 0f, b = -1f, c = 1f, d = 0f, e = 0f, f = 1f)
        else -> error("unreachable")
    }

    fun normalizeDegrees(degrees: Int): Int {
        val d = ((degrees % 360) + 360) % 360
        require(d % 90 == 0) { "Rotation must be a multiple of 90, was $degrees" }
        return d
    }

    /** Horizontal flip of the unit square. */
    val MIRROR_U = Affine2(a = -1f, e = 1f)

    /** Vertical flip of the unit square: raw buffer uv (y up) <-> texture coordinates. */
    val FLIP_Y = Affine2(d = -1f, f = 1f)
}
