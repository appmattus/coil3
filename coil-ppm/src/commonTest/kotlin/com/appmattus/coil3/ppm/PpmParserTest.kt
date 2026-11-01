package com.appmattus.coil3.ppm

import okio.Buffer
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class PpmParserTest {

    @Test
    fun parsesAsciiP3() {
        val image = parse8Bit(
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

        val image = assertIs<Ppm8BitImage>(PpmParser.parse(buffer))

        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertContentEquals(intArrayOf(RED, 0xFF123456.toInt()), image.pixels)
    }

    @Test
    fun parsesSixteenBitBinaryP6AsSixteenBit() {
        val buffer = Buffer()
            .writeUtf8("P6 2 1 65535 ")
            .write(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x80.toByte(), 0x00, 0x00, 0x00))
            .write(byteArrayOf(0x12, 0x34, 0x00, 0x01, 0xAB.toByte(), 0xCD.toByte()))

        val image = assertIs<Ppm16BitImage>(PpmParser.parse(buffer))

        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertContentEquals(samples(0xFFFF, 0x8000, 0x0000, 0x1234, 0x0001, 0xABCD), image.samples)
    }

    @Test
    fun scalesSixteenBitSamplesToFullRange() {
        val buffer = Buffer()
            .writeUtf8("P6 1 1 1023 ")
            .write(byteArrayOf(0x03, 0xFF.toByte(), 0x00, 0x00, 0x02, 0x00))

        val image = assertIs<Ppm16BitImage>(PpmParser.parse(buffer))

        // 1023 -> 65535, 0 -> 0, 512 -> round(512 * 65535 / 1023) = 32800
        assertContentEquals(samples(0xFFFF, 0x0000, 32800), image.samples)
    }

    @Test
    fun parsesAsciiP3WithMaxColorValueAbove255AsSixteenBit() {
        val image = assertIs<Ppm16BitImage>(PpmParser.parse(Buffer().writeUtf8("P3 1 1 65535 65535 32768 1")))

        assertContentEquals(samples(0xFFFF, 0x8000, 0x0001), image.samples)
    }

    @Test
    fun treatsMaxColorValue255AsEightBit() {
        assertIs<Ppm8BitImage>(PpmParser.parse(Buffer().writeUtf8("P3 1 1 255 0 0 0")))
        assertIs<Ppm16BitImage>(PpmParser.parse(Buffer().writeUtf8("P3 1 1 256 0 0 0")))
    }

    @Test
    fun reducesSixteenBitToArgb8888() {
        val image = Ppm16BitImage(2, 1, samples(0xFFFF, 0x8000, 0x0000, 0x0080, 0x007F, 0x1234))

        // 0x8000 -> 128, 0x0080 -> 0.498 -> 0, 0x1234 -> 18.1 -> 18
        assertContentEquals(intArrayOf(0xFFFF8000.toInt(), 0xFF000012.toInt()), image.toArgb8888())
    }

    @Test
    fun convertsSixteenBitToRgbaF16() {
        val image = Ppm16BitImage(1, 1, samples(0xFFFF, 0x0000, 0x8000))

        // 0x8000 / 65535 is just over 0.5 (0x3800), which rounds to the next half float up.
        assertContentEquals(halves(0x3C00, 0x0000, 0x3800, 0x3C00), image.toRgbaF16())
    }

    @Test
    fun scalesSamplesToMaxColorValue() {
        val image = parse8Bit("P3 3 1 15  15 15 15  0 0 0  8 8 8")

        assertContentEquals(intArrayOf(WHITE, BLACK, 0xFF888888.toInt()), image.pixels)
    }

    @Test
    fun skipsCommentsDirectlyAfterTokens() {
        val image = parse8Bit("P3#comment\n1#comment\n1\n255\n0 0 255")

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
    fun rejectsInvalidMaxColorValue() {
        assertFailsWith<IOException> { parse("P3 1 1 0") }
        assertFailsWith<IOException> { parse("P3 1 1 65536") }
    }

    @Test
    fun rejectsSampleAboveMaxColorValue() {
        assertFailsWith<IOException> { parse("P3 1 1 15 16 0 0") }
        assertFailsWith<IOException> { parse("P3 1 1 1023 1024 0 0") }
    }

    @Test
    fun rejectsTruncatedData() {
        assertFailsWith<IOException> { parse("P3 2 1 255 0 0 0") }
        assertFailsWith<IOException> { PpmParser.parse(Buffer().writeUtf8("P6 2 1 255\n").write(ByteArray(4))) }
        assertFailsWith<IOException> { PpmParser.parse(Buffer().writeUtf8("P6 1 1 65535\n").write(ByteArray(5))) }
    }

    private fun parse(text: String) = PpmParser.parse(Buffer().writeUtf8(text))

    private fun parse8Bit(text: String) = assertIs<Ppm8BitImage>(parse(text))

    private fun samples(vararg values: Int) = ShortArray(values.size) { values[it].toShort() }

    private fun halves(vararg values: Int) = ShortArray(values.size) { values[it].toShort() }

    private companion object {
        const val RED = 0xFFFF0000.toInt()
        const val GREEN = 0xFF00FF00.toInt()
        const val BLUE = 0xFF0000FF.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val BLACK = 0xFF000000.toInt()
    }
}
