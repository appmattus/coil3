package com.appmattus.coil3.ppm

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import java.nio.ShortBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ImageBitmapsDeviceTest {

    @Test
    fun eightBitImageIsArgb8888() {
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0xFF123456.toInt())

        val bitmap = createImageBitmap(Ppm8BitImage(2, 1, pixels)).asAndroidBitmap()

        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        assertContentEquals(pixels, bitmap.pixels())
    }

    @Test
    fun sixteenBitImageIsRgbaF16FromApi26() {
        val image = Ppm16BitImage(2, 1, shortArrayOf(-1, 0, 0x4000, 0, 0x1234, -0x5433))

        val bitmap = createImageBitmap(image).asAndroidBitmap()

        assertEquals(2, bitmap.width)
        assertEquals(1, bitmap.height)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(Bitmap.Config.RGBA_F16, bitmap.config)
            assertEquals(ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB), bitmap.colorSpace)

            // The raw pixels hold the half floats exactly, at full precision.
            val halves = ShortBuffer.allocate(image.width * image.height * 4)
            bitmap.copyPixelsToBuffer(halves)
            assertContentEquals(image.toRgbaF16(), halves.array())
        } else {
            assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        }

        // Either way, the colours read back as expected in 8-bit sRGB.
        assertContentEquals(image.toArgb8888(), bitmap.pixels())
    }

    private fun Bitmap.pixels(): IntArray = IntArray(width * height).also {
        getPixels(it, 0, width, 0, 0, width, height)
    }
}
