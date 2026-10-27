package io.github.erenkng.vhscam.core.gif

import java.io.OutputStream

/**
 * Writes animated GIF89a files the way late-90s web graphics looked: every frame is mapped onto
 * the 216-colour web-safe palette with ordered (Bayer) dithering.
 *
 * Create it, [addFrame] for each frame, then [finish]. Nothing is buffered beyond one frame.
 *
 * @param loops how many times the animation plays; 0 loops forever.
 */
class GifEncoder(private val out: OutputStream, val width: Int, val height: Int, loops: Int = 0) {

    private val indices = ByteArray(width * height)
    private var finished = false

    init {
        require(width in 1..0xFFFF && height in 1..0xFFFF) { "Bad size $width x $height" }
        writeAscii("GIF89a")
        // Logical screen: global colour table of 256 entries with 8 bits per primary.
        writeShort(width)
        writeShort(height)
        out.write(0xF7)
        out.write(0) // background colour index
        out.write(0) // square pixels
        for (i in 0 until PALETTE_SIZE) {
            if (i < WEB_SAFE_COLORS) {
                out.write(i / 36 * LEVEL_STEP)
                out.write(i / 6 % 6 * LEVEL_STEP)
                out.write(i % 6 * LEVEL_STEP)
            } else {
                repeat(3) { out.write(0) }
            }
        }
        // NETSCAPE2.0 application extension: the loop count.
        out.write(0x21)
        out.write(0xFF)
        out.write(11)
        writeAscii("NETSCAPE2.0")
        out.write(3)
        out.write(1)
        writeShort(loops)
        out.write(0)
    }

    /**
     * Adds a frame shown for [delayCentis] hundredths of a second.
     *
     * @param argb width x height pixels, row by row, as `0xAARRGGBB` (alpha is ignored).
     */
    fun addFrame(argb: IntArray, delayCentis: Int) {
        check(!finished) { "Already finished" }
        require(argb.size == width * height) { "Expected ${width * height} pixels, got ${argb.size}" }
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                indices[row + x] = paletteIndex(argb[row + x], x, y).toByte()
            }
        }
        // Graphic control extension: no transparency, leave the frame in place, then the delay.
        out.write(0x21)
        out.write(0xF9)
        out.write(4)
        out.write(0x04)
        writeShort(delayCentis.coerceIn(0, 0xFFFF))
        out.write(0)
        out.write(0)
        // Image descriptor: the whole screen, no local colour table, not interlaced.
        out.write(0x2C)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)
        out.write(0)
        out.write(MIN_CODE_SIZE)
        LzwWriter(out).encode(indices)
    }

    /** Writes the trailer. The stream is flushed but not closed. */
    fun finish() {
        if (finished) return
        finished = true
        out.write(0x3B)
        out.flush()
    }

    private fun writeShort(value: Int) {
        out.write(value and 0xFF)
        out.write(value shr 8 and 0xFF)
    }

    private fun writeAscii(text: String) = out.write(text.toByteArray(Charsets.US_ASCII))

    companion object {
        private const val PALETTE_SIZE = 256
        private const val WEB_SAFE_COLORS = 216
        private const val LEVEL_STEP = 51
        private const val MIN_CODE_SIZE = 8

        /** 4x4 Bayer matrix. */
        private val BAYER = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

        /** Level 0..5 of each 8-bit value for each of the 16 dither thresholds. */
        private val LEVELS: Array<ByteArray> = Array(16) { cell ->
            val threshold = (BAYER[cell] + 0.5f) / 16f - 0.5f
            ByteArray(256) { v -> (v / LEVEL_STEP.toFloat() + threshold + 0.5f).toInt().coerceIn(0, 5).toByte() }
        }

        /** Web-safe palette index of a pixel at ([x], [y]), dithered. */
        fun paletteIndex(argb: Int, x: Int, y: Int): Int {
            val levels = LEVELS[(y and 3) * 4 + (x and 3)]
            val r = levels[argb shr 16 and 0xFF].toInt()
            val g = levels[argb shr 8 and 0xFF].toInt()
            val b = levels[argb and 0xFF].toInt()
            return r * 36 + g * 6 + b
        }

        /** RGB of a palette index. */
        fun paletteColor(index: Int): Int =
            if (index < WEB_SAFE_COLORS) {
                (index / 36 * LEVEL_STEP shl 16) or (index / 6 % 6 * LEVEL_STEP shl 8) or (index % 6 * LEVEL_STEP)
            } else {
                0
            }
    }
}

/**
 * GIF's variable-length LZW for 8-bit indices: codes start at 9 bits and grow to 12; when the
 * table is full a clear code starts a new one. Output goes out in sub-blocks of up to 255 bytes.
 */
private class LzwWriter(private val out: OutputStream) {

    private val block = ByteArray(255)
    private var blockSize = 0
    private var bitBuffer = 0
    private var bitCount = 0

    // Open-addressing table from (prefix code, next index) to code.
    private val keys = IntArray(TABLE_SIZE)
    private val codes = IntArray(TABLE_SIZE)

    fun encode(indices: ByteArray) {
        var codeSize = MIN_CODE_SIZE + 1
        var next = FIRST_CODE
        keys.fill(EMPTY)
        writeCode(CLEAR, codeSize)
        var prefix = indices[0].toInt() and 0xFF
        for (i in 1 until indices.size) {
            val k = indices[i].toInt() and 0xFF
            val key = prefix shl 8 or k
            var slot = hash(key)
            while (keys[slot] != EMPTY && keys[slot] != key) slot = (slot + 1) and (TABLE_SIZE - 1)
            if (keys[slot] == key) {
                prefix = codes[slot]
                continue
            }
            writeCode(prefix, codeSize)
            // The decoder adds its entry one code later, so it widens its codes when `next`
            // reaches the next power of two; the encoder does the same before adding.
            if (next >= 1 shl codeSize && codeSize < MAX_CODE_SIZE) codeSize++
            if (next < MAX_CODES) {
                keys[slot] = key
                codes[slot] = next++
            } else {
                writeCode(CLEAR, codeSize)
                keys.fill(EMPTY)
                next = FIRST_CODE
                codeSize = MIN_CODE_SIZE + 1
            }
            prefix = k
        }
        writeCode(prefix, codeSize)
        if (next >= 1 shl codeSize && codeSize < MAX_CODE_SIZE) codeSize++
        writeCode(END, codeSize)
        if (bitCount > 0) writeByte(bitBuffer and 0xFF)
        flushBlock()
        out.write(0) // block terminator
    }

    private fun writeCode(code: Int, size: Int) {
        bitBuffer = bitBuffer or (code shl bitCount)
        bitCount += size
        while (bitCount >= 8) {
            writeByte(bitBuffer and 0xFF)
            bitBuffer = bitBuffer ushr 8
            bitCount -= 8
        }
    }

    private fun writeByte(value: Int) {
        block[blockSize++] = value.toByte()
        if (blockSize == block.size) flushBlock()
    }

    private fun flushBlock() {
        if (blockSize == 0) return
        out.write(blockSize)
        out.write(block, 0, blockSize)
        blockSize = 0
    }

    private fun hash(key: Int): Int = (key * -0x61c88647) ushr (32 - TABLE_BITS)

    private companion object {
        const val MIN_CODE_SIZE = 8
        const val MAX_CODE_SIZE = 12
        const val MAX_CODES = 1 shl MAX_CODE_SIZE
        const val CLEAR = 1 shl MIN_CODE_SIZE
        const val END = CLEAR + 1
        const val FIRST_CODE = CLEAR + 2
        const val TABLE_BITS = 13
        const val TABLE_SIZE = 1 shl TABLE_BITS
        const val EMPTY = -1
    }
}
