package com.appmattus.coil3.netpbm

import com.appmattus.coil3.core.Argb8888Raster
import okio.Buffer
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class PbmParserTest {

    @Test
    fun parsesPlainP1() {
        val image = parse(
            """
            P1
            # a 3x2 image
            3 2
            1 0 1
            0 1 0
            """.trimIndent(),
        )

        assertEquals(3, image.width)
        assertEquals(2, image.height)
        assertFalse(image.hasAlpha)
        assertContentEquals(intArrayOf(B, W, B, W, B, W), image.pixels)
    }

    @Test
    fun parsesPlainP1WithoutSeparators() {
        val image = parse("P1\n4 2\n1001\n01\n\t1 0")

        assertContentEquals(intArrayOf(B, W, W, B, W, B, B, W), image.pixels)
    }

    @Test
    fun parsesRawP4() {
        val image = parse("P4\n8 1\n", 0b10110001)

        assertContentEquals(intArrayOf(B, W, B, B, W, W, W, B), image.pixels)
    }

    @Test
    fun padsRawP4RowsToWholeBytes() {
        // 10 pixels per row: two bytes per row, with the last 6 bits of each row ignored.
        val image = parse("P4 10 2\n", 0b11000000, 0b01111111, 0b00000000, 0b10000000)

        assertContentEquals(
            intArrayOf(B, B, W, W, W, W, W, W, W, B, W, W, W, W, W, W, W, W, B, W),
            image.pixels,
        )
    }

    @Test
    fun readsBinaryRasterAfterSingleWhitespace() {
        // 0x20 (space) and 0x0A (newline) are pixel data here, not whitespace.
        val image = parse("P4 8 2\n", 0x20, 0x0A)

        assertContentEquals(intArrayOf(W, W, B, W, W, W, W, W, W, W, W, W, B, W, B, W), image.pixels)
    }

    @Test
    fun skipsCommentsInHeader() {
        val image = parse("P1#comment\n2 # width\n# height next\n1\n10")

        assertContentEquals(intArrayOf(B, W), image.pixels)
    }

    @Test
    fun readsOnlyFirstImageOfMultiImageFile() {
        val second = "P4 8 1\n".encodeToByteArray().map { it.toInt() and 0xFF }.toIntArray()
        val image = parse("P4 8 1\n", 0xFF, *second, 0x00)

        assertContentEquals(IntArray(8) { B }, image.pixels)
    }

    @Test
    fun rejectsOtherNetpbmFormats() {
        assertFailsWith<IOException> { parse("P2 1 1 255 0") }
        assertFailsWith<IOException> { parse("P5 1 1 255\n", 0) }
    }

    @Test
    fun rejectsInvalidDimensions() {
        assertFailsWith<IOException> { parse("P1 0 1") }
        assertFailsWith<IOException> { parse("P1 1 x 1") }
    }

    @Test
    fun rejectsInvalidPlainPixel() {
        assertFailsWith<IOException> { parse("P1 2 1 1 2") }
        assertFailsWith<IOException> { parse("P1 1 1 #1") }
    }

    @Test
    fun rejectsTruncatedData() {
        assertFailsWith<IOException> { parse("P1 2") }
        assertFailsWith<IOException> { parse("P1 2 1 1") }
        assertFailsWith<IOException> { parse("P4 8 2\n", 0) }
        assertFailsWith<IOException> { parse("P4 9 1\n", 0) }
    }

    private fun parse(header: String, vararg raster: Int) = assertIs<Argb8888Raster>(
        PbmParser.parse(Buffer().writeUtf8(header).write(ByteArray(raster.size) { raster[it].toByte() })),
    )

    private companion object {
        const val B = 0xFF000000.toInt()
        const val W = 0xFFFFFFFF.toInt()
    }
}
