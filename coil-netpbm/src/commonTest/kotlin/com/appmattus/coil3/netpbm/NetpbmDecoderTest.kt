package com.appmattus.coil3.netpbm

import com.appmattus.coil3.core.Argb8888Raster
import com.appmattus.coil3.core.Rgba16Raster
import okio.Buffer
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NetpbmDecoderTest {

    @Test
    fun parsesEveryFormatByMagicNumber() {
        val white = 0xFFFFFFFF.toInt()
        assertContentEquals(intArrayOf(white), parse8Bit("P1 1 1 0").pixels)
        assertContentEquals(intArrayOf(white), parse8Bit("P2 1 1 255 255").pixels)
        assertContentEquals(intArrayOf(white), parse8Bit("P3 1 1 255 255 255 255").pixels)
        assertContentEquals(intArrayOf(white), parse8Bit("P4 1 1\n", 0x00).pixels)
        assertContentEquals(intArrayOf(white), parse8Bit("P5 1 1 255\n", 0xFF).pixels)
        assertContentEquals(intArrayOf(white), parse8Bit("P6 1 1 255\n", 0xFF, 0xFF, 0xFF).pixels)
        assertContentEquals(
            intArrayOf(white),
            parse8Bit("P7\nWIDTH 1\nHEIGHT 1\nDEPTH 1\nMAXVAL 255\nTUPLTYPE GRAYSCALE\nENDHDR\n", 0xFF).pixels,
        )
        assertIs<Rgba16Raster>(parse("P5 1 1 65535\n", 0xFF, 0xFF))
    }

    @Test
    fun rejectsUnknownMagicNumber() {
        assertFailsWith<IOException> { parse("P8 1 1") }
        assertFailsWith<IOException> { parse("GIF89a") }
        assertFailsWith<IOException> { parse("P") }
        assertFailsWith<IOException> { parse("") }
    }

    @Test
    fun recognisesNetpbmMagicNumbers() {
        for (type in 1..6) {
            assertTrue(isNetpbm("P$type\n1 1"), "P$type with newline")
            assertTrue(isNetpbm("P$type 1 1"), "P$type with space")
            assertTrue(isNetpbm("P$type\t1 1"), "P$type with tab")
            assertTrue(isNetpbm("P$type#comment\n1 1"), "P$type with comment")
        }
        assertTrue(isNetpbm("P7\nWIDTH 1"))
        assertTrue(isNetpbm("P7\r\nWIDTH 1"))
    }

    @Test
    fun rejectsNonNetpbmData() {
        assertFalse(isNetpbm("P7 332\n")) // XV thumbnail
        assertFalse(isNetpbm("P0\n"))
        assertFalse(isNetpbm("P8\n"))
        assertFalse(isNetpbm("P12"))
        assertFalse(isNetpbm("Q1\n"))
        assertFalse(isNetpbm("P1"))
        assertFalse(isNetpbm(""))
    }

    @Test
    fun peekingMagicNumberDoesNotConsumeSource() {
        val source = Buffer().writeUtf8("P1 1 1 0")

        assertTrue(NetpbmDecoder.hasNetpbmMagicNumber(source))
        assertContentEquals(intArrayOf(0xFFFFFFFF.toInt()), assertIs<Argb8888Raster>(NetpbmParser.parse(source)).pixels)
    }

    private fun isNetpbm(text: String) = NetpbmDecoder.hasNetpbmMagicNumber(Buffer().writeUtf8(text))

    private fun parse(header: String, vararg raster: Int) =
        NetpbmParser.parse(Buffer().writeUtf8(header).write(ByteArray(raster.size) { raster[it].toByte() }))

    private fun parse8Bit(header: String, vararg raster: Int) = assertIs<Argb8888Raster>(parse(header, *raster))
}
