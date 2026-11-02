package com.appmattus.coil3.heif

import androidx.compose.ui.graphics.ImageBitmap
import okio.IOException
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Decoding runs where the platform supports it and Skia or Android is available: on Apple targets here, and on
 * Android in HeifDecodingDeviceTest.
 */
class HeifDecodingTest {

    @Test
    fun decodesOpaqueImage() {
        if (!isHeifDecodingSupported) return

        val bitmap = decodeHeif(HeifFixtures.quadrants)

        assertEquals(HeifFixtures.WIDTH, bitmap.width)
        assertEquals(HeifFixtures.HEIGHT, bitmap.height)
        assertFalse(bitmap.hasAlpha)
        assertPixelClose(0xFFFF0000.toInt(), bitmap.pixel(LEFT, TOP))
        assertPixelClose(0xFF00FF00.toInt(), bitmap.pixel(RIGHT, TOP))
        assertPixelClose(0xFF0000FF.toInt(), bitmap.pixel(LEFT, BOTTOM))
        assertPixelClose(0xFFFFFFFF.toInt(), bitmap.pixel(RIGHT, BOTTOM))
    }

    @Test
    fun decodesAlpha() {
        if (!isHeifDecodingSupported) return

        val bitmap = decodeHeif(HeifFixtures.halfTransparent)

        assertTrue(bitmap.hasAlpha)
        assertPixelClose(0xFFFF0000.toInt(), bitmap.pixel(LEFT, MIDDLE))
        assertEquals(0, bitmap.pixel(RIGHT, MIDDLE) ushr 24)
    }

    @Test
    fun rejectsCorruptData() {
        if (!isHeifDecodingSupported) return

        assertFailsWith<IOException> { decodeHeif(HeifFixtures.quadrants.copyOf(200)) }
        assertFailsWith<IOException> { decodeHeif(byteArrayOf(1, 2, 3)) }
    }

    @Test
    fun unsupportedPlatformsRefuseToDecode() {
        if (isHeifDecodingSupported) return

        assertFailsWith<UnsupportedOperationException> { decodeHeif(HeifFixtures.quadrants) }
    }

    private fun ImageBitmap.pixel(x: Int, y: Int): Int {
        val pixels = IntArray(1)
        readPixels(pixels, startX = x, startY = y, width = 1, height = 1)
        return pixels[0]
    }

    private companion object {
        // The centre of each quadrant, well away from the edges that lossy compression blurs.
        const val LEFT = HeifFixtures.WIDTH / 4
        const val RIGHT = HeifFixtures.WIDTH * 3 / 4
        const val TOP = HeifFixtures.HEIGHT / 4
        const val BOTTOM = HeifFixtures.HEIGHT * 3 / 4
        const val MIDDLE = HeifFixtures.HEIGHT / 2
    }
}

/** HEVC is lossy and stores colour as subsampled YCbCr, so allow some difference in each channel. */
internal fun assertPixelClose(expected: Int, actual: Int, tolerance: Int = 12) {
    val close = (0 until 32 step 8).all { shift ->
        abs(((expected ushr shift) and 0xFF) - ((actual ushr shift) and 0xFF)) <= tolerance
    }
    assertTrue(close, "Expected ${expected.toUInt().toString(16)} but was ${actual.toUInt().toString(16)}")
}
