package com.appmattus.coil3.pam

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

class PamParserTest {

    @Test
    fun parsesRgb() {
        val image = parse8Bit(header(2, 1, depth = 3, maxValue = 255, "RGB"), 0xFF, 0, 0, 0x12, 0x34, 0x56)

        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertFalse(image.hasAlpha)
        assertContentEquals(intArrayOf(0xFFFF0000.toInt(), 0xFF123456.toInt()), image.pixels)
    }

    @Test
    fun parsesRgbAlpha() {
        val image = parse8Bit(header(2, 1, depth = 4, maxValue = 255, "RGB_ALPHA"), 0xFF, 0, 0, 0x80, 1, 2, 3, 0)

        assertTrue(image.hasAlpha)
        assertContentEquals(intArrayOf(0x80FF0000.toInt(), 0x00010203), image.pixels)
    }

    @Test
    fun parsesGrayscale() {
        val image = parse8Bit(header(3, 1, depth = 1, maxValue = 255, "GRAYSCALE"), 0, 0x80, 0xFF)

        assertFalse(image.hasAlpha)
        assertContentEquals(intArrayOf(0xFF000000.toInt(), 0xFF808080.toInt(), 0xFFFFFFFF.toInt()), image.pixels)
    }

    @Test
    fun parsesGrayscaleAlpha() {
        val image = parse8Bit(header(1, 1, depth = 2, maxValue = 255, "GRAYSCALE_ALPHA"), 0x40, 0x80)

        assertTrue(image.hasAlpha)
        assertContentEquals(intArrayOf(0x80404040.toInt()), image.pixels)
    }

    @Test
    fun parsesBlackAndWhite() {
        // Unlike PBM, 0 is black and 1 is white.
        val image = parse8Bit(header(2, 1, depth = 1, maxValue = 1, "BLACKANDWHITE"), 0, 1)

        assertContentEquals(intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), image.pixels)
    }

    @Test
    fun parsesBlackAndWhiteAlpha() {
        val image = parse8Bit(header(2, 1, depth = 2, maxValue = 1, "BLACKANDWHITE_ALPHA"), 1, 1, 1, 0)

        assertTrue(image.hasAlpha)
        assertContentEquals(intArrayOf(0xFFFFFFFF.toInt(), 0x00FFFFFF), image.pixels)
    }

    @Test
    fun scalesSamplesToMaxValue() {
        val image = parse8Bit(header(1, 1, depth = 3, maxValue = 15, "RGB"), 15, 0, 8)

        assertContentEquals(intArrayOf(0xFFFF0088.toInt()), image.pixels)
    }

    @Test
    fun parsesSixteenBitRgbAlpha() {
        val image = assertIs<Rgba16Raster>(
            parse(header(1, 1, depth = 4, maxValue = 65535, "RGB_ALPHA"), 0xFF, 0xFF, 0x80, 0x00, 0x00, 0x01, 0x12, 0x34),
        )

        assertTrue(image.hasAlpha)
        assertContentEquals(samples(0xFFFF, 0x8000, 0x0001, 0x1234), image.samples)
    }

    @Test
    fun parsesSixteenBitGrayscale() {
        val image = assertIs<Rgba16Raster>(
            parse(header(2, 1, depth = 1, maxValue = 1023, "GRAYSCALE"), 0x03, 0xFF, 0x02, 0x00),
        )

        assertFalse(image.hasAlpha)
        // 1023 -> 65535, 512 -> round(512 * 65535 / 1023) = 32800; alpha is opaque.
        assertContentEquals(samples(0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF, 32800, 32800, 32800, 0xFFFF), image.samples)
    }

    @Test
    fun treatsMaxValue255AsEightBit() {
        assertIs<Argb8888Raster>(parse(header(1, 1, depth = 1, maxValue = 255, "GRAYSCALE"), 0))
        assertIs<Rgba16Raster>(parse(header(1, 1, depth = 1, maxValue = 256, "GRAYSCALE"), 0, 0))
    }

    @Test
    fun readsUnknownOrMissingTupleTypeByDepth() {
        assertContentEquals(intArrayOf(0x80404040.toInt()), parse8Bit(header(1, 1, 2, 255, null), 0x40, 0x80).pixels)
        assertContentEquals(intArrayOf(0x04010203), parse8Bit(header(1, 1, 4, 255, "CMYK?"), 1, 2, 3, 4).pixels)
    }

    @Test
    fun joinsTupleTypeSplitOverSeveralLines() {
        val text = "P7\nWIDTH 1\nHEIGHT 1\nDEPTH 2\nMAXVAL 255\nTUPLTYPE GRAYSCALE\nTUPLTYPE ALPHA\nENDHDR\n"

        // "GRAYSCALE ALPHA" is not a standard tuple type, so it is read by depth (2 = grayscale + alpha).
        assertTrue(parse8Bit(text, 0x40, 0x80).hasAlpha)
    }

    @Test
    fun ignoresCommentsBlankLinesUnknownKeywordsAndWhitespace() {
        val text = "P7\r\n# comment\n\n  WIDTH   1  \nHEIGHT\t1\nFOO bar\nDEPTH 3\nMAXVAL 255\nTUPLTYPE RGB\nENDHDR\r\n"

        assertContentEquals(intArrayOf(0xFF010203.toInt()), parse8Bit(text, 1, 2, 3).pixels)
    }

    @Test
    fun rejectsMissingMagicNumber() {
        assertFailsWith<IOException> { parse("P6\n1 1 255\n", 0, 0, 0) }
        // XV thumbnails also start with P7.
        assertFailsWith<IOException> { parse("P7 332\n#XVVERSION:\n", 0) }
    }

    @Test
    fun rejectsMissingHeaderFields() {
        assertFailsWith<IOException> { parse("P7\nHEIGHT 1\nDEPTH 1\nMAXVAL 255\nENDHDR\n", 0) }
        assertFailsWith<IOException> { parse("P7\nWIDTH 1\nDEPTH 1\nMAXVAL 255\nENDHDR\n", 0) }
        assertFailsWith<IOException> { parse("P7\nWIDTH 1\nHEIGHT 1\nMAXVAL 255\nENDHDR\n", 0) }
        assertFailsWith<IOException> { parse("P7\nWIDTH 1\nHEIGHT 1\nDEPTH 1\nENDHDR\n", 0) }
    }

    @Test
    fun rejectsInvalidHeaderValues() {
        assertFailsWith<IOException> { parse(header(0, 1, 1, 255, null), 0) }
        assertFailsWith<IOException> { parse(header(1, 1, 1, 0, null), 0) }
        assertFailsWith<IOException> { parse(header(1, 1, 1, 65536, null), 0) }
        assertFailsWith<IOException> { parse("P7\nWIDTH x\nHEIGHT 1\nDEPTH 1\nMAXVAL 255\nENDHDR\n", 0) }
    }

    @Test
    fun rejectsDepthThatDoesNotMatchTupleType() {
        assertFailsWith<IOException> { parse(header(1, 1, depth = 4, maxValue = 255, "RGB"), 0, 0, 0, 0) }
        assertFailsWith<IOException> { parse(header(1, 1, depth = 1, maxValue = 255, "RGB_ALPHA"), 0) }
    }

    @Test
    fun rejectsUnsupportedDepth() {
        assertFailsWith<IOException> { parse(header(1, 1, depth = 5, maxValue = 255, null), 0, 0, 0, 0, 0) }
    }

    @Test
    fun rejectsSampleAboveMaxValue() {
        assertFailsWith<IOException> { parse(header(1, 1, depth = 1, maxValue = 1, "BLACKANDWHITE"), 2) }
        assertFailsWith<IOException> { parse(header(1, 1, depth = 1, maxValue = 1023, "GRAYSCALE"), 0x04, 0x00) }
    }

    @Test
    fun rejectsTruncatedData() {
        assertFailsWith<IOException> { parse("P7\nWIDTH 1\nHEIGHT 1\n") }
        assertFailsWith<IOException> { parse(header(2, 1, depth = 3, maxValue = 255, "RGB"), 0, 0, 0, 0, 0) }
        assertFailsWith<IOException> { parse(header(1, 1, depth = 1, maxValue = 65535, "GRAYSCALE"), 0) }
    }

    @Test
    fun recognisesPamMagicNumber() {
        assertTrue(PamDecoder.hasPamMagicNumber(Buffer().writeUtf8("P7\nWIDTH 1")))
        assertTrue(PamDecoder.hasPamMagicNumber(Buffer().writeUtf8("P7\r\nWIDTH 1")))
        assertFalse(PamDecoder.hasPamMagicNumber(Buffer().writeUtf8("P7 332\n")))
        assertFalse(PamDecoder.hasPamMagicNumber(Buffer().writeUtf8("P6\n")))
        assertFalse(PamDecoder.hasPamMagicNumber(Buffer().writeUtf8("P7")))
    }

    private fun header(width: Int, height: Int, depth: Int, maxValue: Int, tupleType: String?) = buildString {
        append("P7\nWIDTH $width\nHEIGHT $height\nDEPTH $depth\nMAXVAL $maxValue\n")
        if (tupleType != null) append("TUPLTYPE $tupleType\n")
        append("ENDHDR\n")
    }

    private fun parse(header: String, vararg raster: Int) =
        PamParser.parse(Buffer().writeUtf8(header).write(ByteArray(raster.size) { raster[it].toByte() }))

    private fun parse8Bit(header: String, vararg raster: Int) = assertIs<Argb8888Raster>(parse(header, *raster))

    private fun samples(vararg values: Int) = ShortArray(values.size) { values[it].toShort() }
}
