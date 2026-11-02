package com.appmattus.coil3.core

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import java.nio.ShortBuffer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalCoilDecoderApi::class)
@RunWith(AndroidJUnit4::class)
class ImageBitmapsDeviceTest {

    @Test
    fun eightBitImageIsArgb8888() {
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0xFF123456.toInt())

        val bitmap = Argb8888Raster(2, 1, false, pixels).toImageBitmap().asAndroidBitmap()

        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        assertFalse(bitmap.hasAlpha())
        assertContentEquals(pixels, bitmap.pixels())
    }

    @Test
    fun eightBitImageWithAlpha() {
        val pixels = intArrayOf(0x80FF0000.toInt(), 0x00000000, 0xFF123456.toInt())

        val bitmap = Argb8888Raster(3, 1, true, pixels).toImageBitmap().asAndroidBitmap()

        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        assertTrue(bitmap.hasAlpha())
        assertPixelsClose(pixels, bitmap.pixels())
    }

    @Test
    fun sixteenBitImageIsRgbaF16FromApi26() {
        val image = Rgba16Raster(2, 1, false, shortArrayOf(-1, 0, 0x4000, -1, 0, 0x1234, -0x5433, -1))

        val bitmap = image.toImageBitmap().asAndroidBitmap()

        assertEquals(2, bitmap.width)
        assertEquals(1, bitmap.height)
        assertFalse(bitmap.hasAlpha())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(Bitmap.Config.RGBA_F16, bitmap.config)
            assertEquals(ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB), bitmap.colorSpace)
            // The raw pixels hold the half floats exactly, at full precision.
            assertContentEquals(image.toRgbaF16(premultiplied = true), bitmap.halves())
        } else {
            assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        }

        // Either way, the colours read back as expected in 8-bit sRGB.
        assertContentEquals(image.toArgb8888(), bitmap.pixels())
    }

    @Test
    fun sixteenBitImageWithAlphaIsPremultipliedRgbaF16FromApi26() {
        val image = Rgba16Raster(2, 1, true, shortArrayOf(-1, 0, 0x4000, -0x8000, 0, 0x1234, -0x5433, -1))

        val bitmap = image.toImageBitmap().asAndroidBitmap()

        assertTrue(bitmap.hasAlpha())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(Bitmap.Config.RGBA_F16, bitmap.config)
            assertContentEquals(image.toRgbaF16(premultiplied = true), bitmap.halves())
        } else {
            assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        }
        assertPixelsClose(image.toArgb8888(), bitmap.pixels())
    }

    private fun Bitmap.pixels(): IntArray = IntArray(width * height).also {
        getPixels(it, 0, width, 0, 0, width, height)
    }

    private fun Bitmap.halves(): ShortArray = ShortBuffer.allocate(width * height * 4).also {
        copyPixelsToBuffer(it)
    }.array()

    /** Android stores translucent pixels premultiplied, so allow a little rounding in each channel. */
    private fun assertPixelsClose(expected: IntArray, actual: IntArray, tolerance: Int = 2) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            val e = expected[i]
            val a = actual[i]
            val alpha = e ushr 24
            val close = (0 until 32 step 8).all { shift ->
                (shift != 24 && alpha == 0) || abs(((e ushr shift) and 0xFF) - ((a ushr shift) and 0xFF)) <= tolerance
            }
            assertTrue(close, "Pixel $i: expected ${e.toUInt().toString(16)} but was ${a.toUInt().toString(16)}")
        }
    }
}
