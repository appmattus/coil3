package com.appmattus.coil3.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Skiko-backed conversions are tested on Apple targets, where Skia is linked into the test binary.
@OptIn(InternalCoilDecoderApi::class)
class ImageBitmapsTest {

    @Test
    fun eightBitImageIsOpaqueRgba8888() {
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0xFF123456.toInt())

        val imageBitmap = Argb8888Raster(2, 1, false, pixels).toImageBitmap()
        val bitmap = imageBitmap.asSkiaBitmap()

        assertEquals(ImageBitmapConfig.Argb8888, imageBitmap.config)
        assertEquals(ColorType.RGBA_8888, bitmap.colorType)
        assertEquals(ColorAlphaType.OPAQUE, bitmap.alphaType)
        assertContentEquals(pixels, readPixels(imageBitmap))
    }

    @Test
    fun eightBitImageWithAlphaIsUnpremultiplied() {
        val pixels = intArrayOf(0x80FF0000.toInt(), 0x00000000, 0xFF123456.toInt())

        val imageBitmap = Argb8888Raster(3, 1, true, pixels).toImageBitmap()

        assertEquals(ColorAlphaType.UNPREMUL, imageBitmap.asSkiaBitmap().alphaType)
        assertPixelsClose(pixels, readPixels(imageBitmap))
    }

    @Test
    fun sixteenBitImageIsRgbaF16() {
        val image = Rgba16Raster(2, 1, false, shortArrayOf(-1, 0, 0x4000, -1, 0, 0x1234, -0x5433, -1))

        val imageBitmap = image.toImageBitmap()
        val bitmap = imageBitmap.asSkiaBitmap()

        assertEquals(ImageBitmapConfig.F16, imageBitmap.config)
        assertEquals(ColorType.RGBA_F16, bitmap.colorType)
        assertEquals(ColorAlphaType.OPAQUE, bitmap.alphaType)
        assertEquals(2, bitmap.width)
        assertEquals(1, bitmap.height)

        // The raw pixels hold the half floats exactly, at full precision...
        assertContentEquals(image.toRgbaF16(premultiplied = false), readHalves(imageBitmap))
        // ...and read back as the expected 8-bit colours.
        assertContentEquals(image.toArgb8888(), readPixels(imageBitmap))
    }

    @Test
    fun sixteenBitImageWithAlphaIsUnpremultiplied() {
        val image = Rgba16Raster(2, 1, true, shortArrayOf(-1, 0, 0x4000, -0x8000, 0, 0x1234, -0x5433, -1))

        val imageBitmap = image.toImageBitmap()

        assertEquals(ColorAlphaType.UNPREMUL, imageBitmap.asSkiaBitmap().alphaType)
        assertContentEquals(image.toRgbaF16(premultiplied = false), readHalves(imageBitmap))
        assertPixelsClose(image.toArgb8888(), readPixels(imageBitmap))
    }

    private fun readPixels(imageBitmap: ImageBitmap): IntArray {
        val pixels = IntArray(imageBitmap.width * imageBitmap.height)
        imageBitmap.readPixels(pixels)
        return pixels
    }

    private fun readHalves(imageBitmap: ImageBitmap): ShortArray {
        val bytes = imageBitmap.asSkiaBitmap().readPixels()!!
        return ShortArray(bytes.size / 2) {
            ((bytes[it * 2].toInt() and 0xFF) or (bytes[it * 2 + 1].toInt() shl 8)).toShort()
        }
    }
}

/** Translucent pixels may be premultiplied on the way back out, so allow a little rounding in each channel. */
internal fun assertPixelsClose(expected: IntArray, actual: IntArray, tolerance: Int = 2) {
    assertEquals(expected.size, actual.size)
    for (i in expected.indices) {
        val e = expected[i]
        val a = actual[i]
        val alpha = e ushr 24
        val close = (0 until 32 step 8).all { shift ->
            // Colour is meaningless when fully transparent.
            (shift != 24 && alpha == 0) || abs(((e ushr shift) and 0xFF) - ((a ushr shift) and 0xFF)) <= tolerance
        }
        assertTrue(close, "Pixel $i: expected ${e.toUInt().toString(16)} but was ${a.toUInt().toString(16)}")
    }
}
