package com.appmattus.coil3.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@OptIn(InternalCoilDecoderApi::class)
class RasterImageTest {

    @Test
    fun reducesSixteenBitToArgb8888() {
        val image = Rgba16Raster(2, 1, true, samples(0xFFFF, 0x8000, 0x0000, 0xFFFF, 0x0080, 0x007F, 0x1234, 0x8000))

        // 0x8000 -> 128, 0x0080 -> 0.498 -> 0, 0x1234 -> 18.1 -> 18
        assertContentEquals(intArrayOf(0xFFFF8000.toInt(), 0x80000012.toInt()), image.toArgb8888())
    }

    @Test
    fun convertsSixteenBitToRgbaF16() {
        val image = Rgba16Raster(1, 1, false, samples(0xFFFF, 0x0000, 0x8000, 0xFFFF))

        // 0x8000 / 65535 is just over 0.5 (0x3800), which rounds to the next half float up.
        assertContentEquals(halves(0x3C00, 0x0000, 0x3800, 0x3C00), image.toRgbaF16(premultiplied = false))
        assertContentEquals(halves(0x3C00, 0x0000, 0x3800, 0x3C00), image.toRgbaF16(premultiplied = true))
    }

    @Test
    fun premultipliesRgbaF16ByAlpha() {
        // Alpha 0x4000 / 65535 is just over 0.25.
        val image = Rgba16Raster(1, 1, true, samples(0xFFFF, 0x8000, 0x0000, 0x4000))

        assertContentEquals(halves(0x3C00, 0x3800, 0x0000, 0x3400), image.toRgbaF16(premultiplied = false))
        assertContentEquals(halves(0x3400, 0x3000, 0x0000, 0x3400), image.toRgbaF16(premultiplied = true))
    }

    @Test
    fun scalesSamples() {
        assertEquals(255, scaleSample(15, 15, 255))
        assertEquals(136, scaleSample(8, 15, 255))
        assertEquals(65535, scaleSample(1023, 1023, 65535))
        assertEquals(32800, scaleSample(512, 1023, 65535))
        assertEquals(65535, scaleSample(65535, 65535, 65535))
        assertEquals(0, scaleSample(0, 1, 255))
    }

    private fun samples(vararg values: Int) = ShortArray(values.size) { values[it].toShort() }

    private fun halves(vararg values: Int) = ShortArray(values.size) { values[it].toShort() }
}
