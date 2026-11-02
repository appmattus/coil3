package com.appmattus.coil3.heif

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HeifDecodingDeviceTest {

    @Test
    fun supportedFromApi28() {
        assertEquals(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P, isHeifDecodingSupported)
    }

    @Test
    fun decodesOpaqueImage() {
        if (!isHeifDecodingSupported) return

        val bitmap = decodeHeif(quadrants).asAndroidBitmap()

        assertEquals(HeifFixtures.WIDTH, bitmap.width)
        assertEquals(HeifFixtures.HEIGHT, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        assertFalse(bitmap.hasAlpha())
        assertPixelClose(0xFFFF0000.toInt(), bitmap.getPixel(LEFT, TOP))
        assertPixelClose(0xFF00FF00.toInt(), bitmap.getPixel(RIGHT, TOP))
        assertPixelClose(0xFF0000FF.toInt(), bitmap.getPixel(LEFT, BOTTOM))
        assertPixelClose(0xFFFFFFFF.toInt(), bitmap.getPixel(RIGHT, BOTTOM))
    }

    @Test
    fun decodesImageWithAlphaPlane() {
        if (!isHeifDecodingSupported) return

        val bitmap = decodeHeif(halfTransparent).asAndroidBitmap()

        assertEquals(HeifFixtures.WIDTH, bitmap.width)
        assertEquals(HeifFixtures.HEIGHT, bitmap.height)
        assertPixelClose(0xFFFF0000.toInt(), bitmap.getPixel(LEFT, MIDDLE))
        // Android's HEIF decoder ignores the alpha plane (seen on API 37), so the transparent half isn't checked.
    }

    @Test
    fun rejectsCorruptData() {
        if (!isHeifDecodingSupported) return

        assertFailsWith<IOException> { decodeHeif(quadrants.copyOf(200)) }
        assertFailsWith<IOException> { decodeHeif(byteArrayOf(1, 2, 3)) }
    }

    @Test
    fun refusesToDecodeBeforeApi28() {
        if (isHeifDecodingSupported) return

        assertFailsWith<UnsupportedOperationException> { decodeHeif(quadrants) }
    }

    private fun assertPixelClose(expected: Int, actual: Int, tolerance: Int = 12) {
        val close = (0 until 32 step 8).all { shift ->
            abs(((expected ushr shift) and 0xFF) - ((actual ushr shift) and 0xFF)) <= tolerance
        }
        assertTrue(close, "Expected ${expected.toUInt().toString(16)} but was ${actual.toUInt().toString(16)}")
    }

    private val quadrants get() = HeifFixtures.quadrants
    private val halfTransparent get() = HeifFixtures.halfTransparent

    private companion object {
        // The centre of each quadrant, well away from the edges that lossy compression blurs.
        const val LEFT = HeifFixtures.WIDTH / 4
        const val RIGHT = HeifFixtures.WIDTH * 3 / 4
        const val TOP = HeifFixtures.HEIGHT / 4
        const val BOTTOM = HeifFixtures.HEIGHT * 3 / 4
        const val MIDDLE = HeifFixtures.HEIGHT / 2
    }
}
