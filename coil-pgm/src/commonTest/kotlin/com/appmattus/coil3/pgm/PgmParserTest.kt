package com.appmattus.coil3.pgm

import com.appmattus.coil3.core.Argb8888Raster
import com.appmattus.coil3.core.Rgba16Raster
import okio.Buffer
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PgmParserTest {

    @Test
    fun parsesPlainP2() {
        val image = parse8Bit(
            """
            P2
            # a 3x2 image
            3 2
            255
            0 128 255
            18 52 86
            """.trimIndent(),
        )

        assertEquals(3, image.width)
        assertEquals(2, image.height)
        assertFalse(image.hasAlpha)
        assertContentEquals(
            intArrayOf(BLACK, 0xFF808080.toInt(), WHITE, 0xFF121212.toInt(), 0xFF343434.toInt(), 0xFF565656.toInt()),
            image.pixels,
        )
    }

    @Test
    fun parsesRawP5() {
        val image = assertIs<Argb8888Raster>(parse("P5\n2 1\n255\n", 0x12, 0xFF))

        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertContentEquals(intArrayOf(0xFF121212.toInt(), WHITE), image.pixels)
    }

    @Test
    fun parsesSixteenBitRawP5AsSixteenBit() {
        val image = assertIs<Rgba16Raster>(parse("P5 2 1 65535 ", 0xFF, 0xFF, 0x12, 0x34))

        assertFalse(image.hasAlpha)
        assertContentEquals(samples(0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 0x1234, 0x1234, 0x1234, 0xFFFF), image.samples)
    }

    @Test
    fun scalesSixteenBitSamplesToFullRange() {
        val image = assertIs<Rgba16Raster>(parse("P5 2 1 1023 ", 0x03, 0xFF, 0x02, 0x00))

        // 1023 -> 65535, 512 -> round(512 * 65535 / 1023) = 32800
        assertContentEquals(samples(0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 32800, 32800, 32800, 0xFFFF), image.samples)
    }

    @Test
    fun parsesPlainP2WithMaxvalAbove255AsSixteenBit() {
        val image = assertIs<Rgba16Raster>(parse("P2 1 1 65535 32768"))

        assertContentEquals(samples(0x8000, 0x8000, 0x8000, 0xFFFF), image.samples)
    }

    @Test
    fun treatsMaxval255AsEightBit() {
        assertIs<Argb8888Raster>(parse("P2 1 1 255 0"))
        assertIs<Rgba16Raster>(parse("P2 1 1 256 0"))
    }

    @Test
    fun scalesSamplesToMaxval() {
        val image = parse8Bit("P2 3 1 15  15 0 8")

        assertContentEquals(intArrayOf(WHITE, BLACK, 0xFF888888.toInt()), image.pixels)
    }

    @Test
    fun parsesMaxvalOfOne() {
        // Unlike PBM, 0 is black and maxval is white.
        assertContentEquals(intArrayOf(BLACK, WHITE), parse8Bit("P2 2 1 1 0 1").pixels)
    }

    @Test
    fun skipsCommentsAnywhereInHeader() {
        val image = parse8Bit("P2#comment\n1 # width\n1\n# maxval next\n255\n200")

        assertContentEquals(intArrayOf(0xFFC8C8C8.toInt()), image.pixels)
    }

    @Test
    fun readsBinaryRasterAfterSingleWhitespace() {
        // The byte after the single whitespace following maxval is a sample, even if it looks like whitespace.
        val image = assertIs<Argb8888Raster>(parse("P5 2 1 255\n", 0x0A, 0x20))

        assertContentEquals(intArrayOf(0xFF0A0A0A.toInt(), 0xFF202020.toInt()), image.pixels)
    }

    @Test
    fun readsOnlyFirstImageOfMultiImageFile() {
        val image = assertIs<Argb8888Raster>(parse("P5 1 1 255\n", 0x40, *"P5 1 1 255\n".encodeToByteArray().toInts(), 0x80))

        assertContentEquals(intArrayOf(0xFF404040.toInt()), image.pixels)
    }

    @Test
    fun rejectsOtherNetpbmFormats() {
        assertFailsWith<IOException> { parse("P3 1 1 255 0 0 0") }
        assertFailsWith<IOException> { parse("P6 1 1 255\n", 0, 0, 0) }
        assertFailsWith<IOException> { parse("P1 1 1 0") }
    }

    @Test
    fun rejectsInvalidHeaderValues() {
        assertFailsWith<IOException> { parse("P2 0 1 255") }
        assertFailsWith<IOException> { parse("P2 1 x 255") }
        assertFailsWith<IOException> { parse("P2 1 1 0") }
        assertFailsWith<IOException> { parse("P2 1 1 65536") }
    }

    @Test
    fun rejectsSampleAboveMaxval() {
        assertFailsWith<IOException> { parse("P2 1 1 15 16") }
        assertFailsWith<IOException> { parse("P5 1 1 1023 ", 0x04, 0x00) }
        assertFailsWith<IOException> { parse("P5 1 1 100 ", 101) }
    }

    @Test
    fun rejectsInvalidPlainSample() {
        assertFailsWith<IOException> { parse("P2 1 1 255 abc") }
    }

    @Test
    fun rejectsTruncatedData() {
        assertFailsWith<IOException> { parse("P2 2 1") }
        assertFailsWith<IOException> { parse("P2 2 1 255 0") }
        assertFailsWith<IOException> { parse("P5 2 1 255\n", 0) }
        assertFailsWith<IOException> { parse("P5 1 1 65535\n", 0) }
    }

    @Test
    fun recognisesPgmMagicNumber() {
        assertTrue(PgmDecoder.hasPgmMagicNumber(Buffer().writeUtf8("P2\n1 1 255\n0")))
        assertTrue(PgmDecoder.hasPgmMagicNumber(Buffer().writeUtf8("P5 1 1 255\n")))
        assertFalse(PgmDecoder.hasPgmMagicNumber(Buffer().writeUtf8("P6 1 1 255\n")))
        assertFalse(PgmDecoder.hasPgmMagicNumber(Buffer().writeUtf8("P")))
    }

    private fun parse(header: String, vararg raster: Int) =
        PgmParser.parse(Buffer().writeUtf8(header).write(ByteArray(raster.size) { raster[it].toByte() }))

    private fun parse8Bit(text: String) = assertIs<Argb8888Raster>(parse(text))

    private fun samples(vararg values: Int) = ShortArray(values.size) { values[it].toShort() }

    private fun ByteArray.toInts() = IntArray(size) { this[it].toInt() and 0xFF }

    private companion object {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val BLACK = 0xFF000000.toInt()
    }
}
