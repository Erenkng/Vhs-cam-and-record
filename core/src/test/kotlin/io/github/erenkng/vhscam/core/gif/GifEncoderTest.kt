package io.github.erenkng.vhscam.core.gif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import kotlin.random.Random

class GifEncoderTest {

    private fun encode(width: Int, height: Int, vararg frames: IntArray): ByteArray =
        ByteArrayOutputStream().use { out ->
            val encoder = GifEncoder(out, width, height)
            frames.forEach { encoder.addFrame(it, delayCentis = 10) }
            encoder.finish()
            out.toByteArray()
        }

    private fun reader(bytes: ByteArray): ImageReader =
        ImageIO.getImageReadersByFormatName("gif").next().apply {
            input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        }

    private fun BufferedImage.rgb(x: Int, y: Int) = getRGB(x, y) and 0xFFFFFF

    @Test
    fun paletteColoursSurviveUnchanged() {
        val w = 20
        val h = 10
        val red = IntArray(w * h) { 0xFFFF0000.toInt() }
        val split = IntArray(w * h) { i -> if (i % w < 10) 0xFF0000FF.toInt() else 0xFFFFFFFF.toInt() }
        val bytes = encode(w, h, red, split)

        assertEquals("GIF89a", String(bytes, 0, 6, Charsets.US_ASCII))
        assertTrue(String(bytes, Charsets.ISO_8859_1).contains("NETSCAPE2.0"))
        val reader = reader(bytes)
        assertEquals(2, reader.getNumImages(true))
        val first = reader.read(0)
        assertEquals(w, first.width)
        assertEquals(h, first.height)
        assertEquals(0xFF0000, first.rgb(3, 3))
        val second = reader.read(1)
        assertEquals(0x0000FF, second.rgb(2, 5))
        assertEquals(0xFFFFFF, second.rgb(15, 5))
    }

    @Test
    fun noiseDecodesToTheDitheredPalette() {
        // Random colours fill the LZW table many times over, so this also covers the clear codes
        // and every code width.
        val w = 301
        val h = 157
        val random = Random(7)
        val frame = IntArray(w * h) { random.nextInt() }
        val reader = reader(encode(w, h, frame))
        val image = reader.read(0)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val expected = GifEncoder.paletteColor(GifEncoder.paletteIndex(frame[y * w + x], x, y))
                assertEquals("pixel $x,$y", expected, image.rgb(x, y))
            }
        }
    }

    @Test
    fun flatAreasCompressWell() {
        val w = 320
        val h = 240
        val gradient = IntArray(w * h) { i -> (i % w * 255 / w) * 0x010101 }
        val bytes = encode(w, h, gradient)
        assertTrue("${bytes.size} bytes", bytes.size < w * h / 3)
        val image = reader(bytes).read(0)
        assertEquals(0x000000, image.rgb(0, 0))
        assertEquals(0xFFFFFF, image.rgb(w - 1, 0))
    }

    @Test
    fun ditheringMixesNeighbouringLevels() {
        // Mid grey sits between two palette levels: dithering uses both.
        val levels = (0 until 16).map { i -> GifEncoder.paletteIndex(0xFF808080.toInt(), i % 4, i / 4) }.toSet()
        assertEquals(2, levels.size)
    }
}
