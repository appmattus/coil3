package com.appmattus.coil3.ppm

import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.ColorType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

// Skiko-backed conversions are tested on Apple targets, where Skia is linked into the test binary.
class ImageBitmapsTest {

    @Test
    fun eightBitImageIsArgb8888() {
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0xFF123456.toInt())

        val imageBitmap = createImageBitmap(Ppm8BitImage(2, 1, pixels))

        assertEquals(ImageBitmapConfig.Argb8888, imageBitmap.config)
        assertEquals(ColorType.RGBA_8888, imageBitmap.asSkiaBitmap().colorType)
        assertContentEquals(pixels, readPixels(imageBitmap))
    }

    @Test
    fun sixteenBitImageIsRgbaF16() {
        val image = Ppm16BitImage(2, 1, shortArrayOf(-1, 0, 0x4000, 0, 0x1234, -0x5433))

        val imageBitmap = createImageBitmap(image)
        val bitmap = imageBitmap.asSkiaBitmap()

        assertEquals(ImageBitmapConfig.F16, imageBitmap.config)
        assertEquals(ColorType.RGBA_F16, bitmap.colorType)
        assertEquals(2, bitmap.width)
        assertEquals(1, bitmap.height)

        // The raw pixels hold the half floats exactly, at full precision.
        val bytes = bitmap.readPixels()!!
        val halves = ShortArray(bytes.size / 2) {
            ((bytes[it * 2].toInt() and 0xFF) or (bytes[it * 2 + 1].toInt() shl 8)).toShort()
        }
        assertContentEquals(image.toRgbaF16(), halves)

        // ...and read back as the expected 8-bit colours.
        assertContentEquals(image.toArgb8888(), readPixels(imageBitmap))
    }

    private fun readPixels(imageBitmap: androidx.compose.ui.graphics.ImageBitmap): IntArray {
        val pixels = IntArray(imageBitmap.width * imageBitmap.height)
        imageBitmap.readPixels(pixels)
        return pixels
    }
}
