package com.appmattus.coil3.ppm

import okio.Buffer
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PpmParserTest {

    @Test
    fun parsesAsciiP3() {
        val image = parse(
            """
            P3
            # a 2x2 image
            2 2
            255
            255 0 0   0 255 0
            0 0 255   255 255 255
            """.trimIndent(),
        )

        assertEquals(2, image.width)
        assertEquals(2, image.height)
        assertContentEquals(intArrayOf(RED, GREEN, BLUE, WHITE), image.pixels)
    }

    @Test
    fun parsesBinaryP6() {
        val buffer = Buffer()
            .writeUtf8("P6\n2 1\n255\n")
            .write(byteArrayOf(0xFF.toByte(), 0, 0, 0x12, 0x34, 0x56))

        val image = PpmParser.parse(buffer)

        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertContentEquals(intArrayOf(RED, 0xFF123456.toInt()), image.pixels)
    }

    @Test
    fun parsesSixteenBitBinaryP6() {
        val buffer = Buffer()
            .writeUtf8("P6 1 1 65535 ")
            .write(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x80.toByte(), 0x00, 0x00, 0x00))

        val image = PpmParser.parse(buffer)

        assertContentEquals(intArrayOf(0xFFFF8000.toInt()), image.pixels)
    }

    @Test
    fun scalesSamplesToMaxColorValue() {
        val image = parse("P3 3 1 15  15 15 15  0 0 0  8 8 8")

        assertContentEquals(intArrayOf(WHITE, BLACK, 0xFF888888.toInt()), image.pixels)
    }

    @Test
    fun skipsCommentsDirectlyAfterTokens() {
        val image = parse("P3#comment\n1#comment\n1\n255\n0 0 255")

        assertContentEquals(intArrayOf(BLUE), image.pixels)
    }

    @Test
    fun rejectsUnsupportedMagicNumber() {
        assertFailsWith<IOException> { parse("P2 1 1 255 0") }
    }

    @Test
    fun rejectsInvalidDimensions() {
        assertFailsWith<IOException> { parse("P3 0 1 255") }
        assertFailsWith<IOException> { parse("P3 x 1 255") }
    }

    @Test
    fun rejectsSampleAboveMaxColorValue() {
        assertFailsWith<IOException> { parse("P3 1 1 15 16 0 0") }
    }

    @Test
    fun rejectsTruncatedData() {
        assertFailsWith<IOException> { parse("P3 2 1 255 0 0 0") }
        assertFailsWith<IOException> { PpmParser.parse(Buffer().writeUtf8("P6 2 1 255\n").write(ByteArray(4))) }
    }

    private fun parse(text: String) = PpmParser.parse(Buffer().writeUtf8(text))

    private companion object {
        const val RED = 0xFFFF0000.toInt()
        const val GREEN = 0xFF00FF00.toInt()
        const val BLUE = 0xFF0000FF.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val BLACK = 0xFF000000.toInt()
    }
}
