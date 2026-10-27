package io.github.erenkng.vhscam.core.geometry

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrientationMathTest {

    /** Raw buffer uv -> texture coordinates. An output in sensor orientation has exactly this transform. */
    private val flipY = Affine2(d = -1f, f = 1f)

    @Test
    fun affineInverseRoundTrips() {
        val m = Affine2(a = 0.3f, b = 1.2f, c = -0.7f, d = 0.4f, e = 0.25f, f = -1.5f)
        assertAffineEquals(Affine2.IDENTITY, m * m.inverse())
        assertAffineEquals(Affine2.IDENTITY, m.inverse() * m)
    }

    @Test
    fun glMatrixRoundTrips() {
        val m = Affine2(a = 0f, b = 1f, c = -1f, d = 0f, e = 1f, f = 0f)
        assertAffineEquals(m, Affine2.fromGlMatrix(m.toGlMatrix()))
    }

    @Test
    fun rotationsMapCornersClockwise() {
        // For a 90 degree clockwise rotation the buffer's top-left lands on the upright top-right,
        // its top-right on the upright bottom-right and its bottom-right on the bottom-left.
        val toBuffer = OrientationMath.uprightToRotated(90)
        assertPoint(1f, 1f, toBuffer, 0f, 1f)
        assertPoint(1f, 0f, toBuffer, 1f, 1f)
        assertPoint(0f, 0f, toBuffer, 1f, 0f)
        // 270 is the inverse of 90.
        assertAffineEquals(Affine2.IDENTITY, OrientationMath.uprightToRotated(90) * OrientationMath.uprightToRotated(270))
        // 180 twice is identity.
        assertAffineEquals(Affine2.IDENTITY, OrientationMath.uprightToRotated(180) * OrientationMath.uprightToRotated(180))
    }

    @Test
    fun streamSharingOutputIsRotatedIntoUprightSpace() {
        // Stream sharing: the effect output stays in sensor orientation (no additional transform).
        val mapping = OrientationMath.fit(
            output = flipY,
            bufferWidth = 1440,
            bufferHeight = 1080,
            sensorRotationDegrees = 90,
        )
        assertFalse(mapping.mirrored)
        assertEquals(0.75f, mapping.uprightAspect, 1e-4f)
        // Output top-left (sensor orientation) is the upright top-right.
        assertPoint(0f, 1f, mapping.outputToUpright, 1f, 1f)
        // Texture lookup of the upright centre stays in the centre.
        assertPoint(0.5f, 0.5f, mapping.uprightToTexture, 0.5f, 0.5f)
    }

    @Test
    fun portraitVideoKeepsTheStampUprightOnScreen() {
        // Video with the phone held upright (back camera, sensor at 90 degrees): the effect writes
        // in sensor orientation and the preview turns that a quarter turn clockwise.
        val mapping = OrientationMath.fit(
            output = flipY,
            bufferWidth = 1440,
            bufferHeight = 1080,
            sensorRotationDegrees = 90,
        )
        val screenToOutput = OrientationMath.uprightToRotated(90)
        // Each point on screen shows the upright frame at that same point, so text reads upright.
        assertAffineEquals(Affine2.IDENTITY, mapping.outputToUpright * screenToOutput)
        assertEquals(0.75f, mapping.uprightAspect, 1e-4f)
        // The upright frame is the buffer turned clockwise, whatever SurfaceTexture's own
        // transform holds (it usually includes this same rotation already).
        assertAffineEquals(flipY * OrientationMath.uprightToRotated(90), mapping.uprightToTexture)
    }

    @Test
    fun landscapeVideoUsesTheSensorFrameAsItIs() {
        // Held on its side with the top to the left, the back sensor is already the right way up.
        val mapping = OrientationMath.fit(
            output = flipY,
            bufferWidth = 1440,
            bufferHeight = 1080,
            sensorRotationDegrees = 0,
        )
        assertEquals(1440f / 1080f, mapping.uprightAspect, 1e-4f)
        assertAffineEquals(flipY, mapping.uprightToTexture)
        assertAffineEquals(Affine2.IDENTITY, mapping.outputToUpright)
    }

    @Test
    fun alreadyRotatedOutputNeedsNoExtraRotation() {
        // A preview-only effect receives an output CameraX already rotated for portrait.
        val additional = OrientationMath.uprightToRotated(90)
        val mapping = OrientationMath.fit(
            output = flipY * additional,
            bufferWidth = 1440,
            bufferHeight = 1080,
            sensorRotationDegrees = 90,
        )
        assertAffineEquals(Affine2.IDENTITY, mapping.outputToUpright)
    }

    @Test
    fun deviceHeldSidewaysRotatesTheProcessingSpace() {
        // Output already rotated for a portrait display, but the phone is held in landscape.
        val additional = OrientationMath.uprightToRotated(90)
        val mapping = OrientationMath.fit(
            output = flipY * additional,
            bufferWidth = 1440,
            bufferHeight = 1080,
            sensorRotationDegrees = 0,
        )
        assertEquals(1440f / 1080f, mapping.uprightAspect, 1e-4f)
        assertTrue(abs(mapping.outputToUpright.determinant - 1f) < 1e-4f)
        assertAffineEquals(OrientationMath.uprightToRotated(90), mapping.outputToUpright)
    }

    @Test
    fun mirroredOutputIsDetectedAndCancelledForTheFinalPass() {
        val additional = OrientationMath.uprightToRotated(90) * OrientationMath.MIRROR_U
        val mapping = OrientationMath.fit(
            output = flipY * additional,
            bufferWidth = 1440,
            bufferHeight = 1080,
            sensorRotationDegrees = 90,
        )
        assertTrue(mapping.mirrored)
        // Text drawn in upright space must not come out mirrored.
        assertAffineEquals(Affine2.IDENTITY, mapping.outputToUpright)
    }

    @Test
    fun cropIsKeptAndAspectFollowsIt() {
        // 16:9 band out of a 4:3 buffer.
        val crop = Affine2(a = 1f, d = 0.75f, e = 0f, f = 0.125f)
        val mapping = OrientationMath.fit(
            output = flipY * crop,
            bufferWidth = 1440,
            bufferHeight = 1080,
            sensorRotationDegrees = 0,
        )
        assertEquals(16f / 9f, mapping.uprightAspect, 1e-3f)
        assertAffineEquals(Affine2.IDENTITY, mapping.outputToUpright)
        // Upright bottom-left is the crop's bottom-left in the (flipped) texture.
        assertPoint(0f, 0f, mapping.uprightToTexture, 0f, 1f - 0.125f)
    }

    private fun assertPoint(x: Float, y: Float, m: Affine2, ex: Float, ey: Float) {
        assertEquals(ex, m.mapX(x, y), 1e-4f, "x of ($x, $y)")
        assertEquals(ey, m.mapY(x, y), 1e-4f, "y of ($x, $y)")
    }

    private fun assertAffineEquals(expected: Affine2, actual: Affine2) {
        val e = floatArrayOf(expected.a, expected.b, expected.c, expected.d, expected.e, expected.f)
        val a = floatArrayOf(actual.a, actual.b, actual.c, actual.d, actual.e, actual.f)
        for (i in e.indices) assertEquals(e[i], a[i], 1e-4f, "component $i of $actual")
    }
}
