package com.appmattus.coil3.core

import kotlin.test.Test
import kotlin.test.assertEquals

class HalfFloatTest {

    @Test
    fun convertsExactValues() {
        assertHalf(0x0000, 0f)
        assertHalf(0x8000, -0f)
        assertHalf(0x3C00, 1f)
        assertHalf(0x3800, 0.5f)
        assertHalf(0xC000, -2f)
        assertHalf(0x7BFF, 65504f)
    }

    @Test
    fun convertsSubnormals() {
        assertHalf(0x0001, 5.9604645e-8f) // 2^-24, the smallest subnormal
        assertHalf(0x0200, 3.0517578e-5f) // 2^-15
        assertHalf(0x03FF, 6.097555e-5f) // the largest subnormal
        assertHalf(0x0400, 6.1035156e-5f) // 2^-14, the smallest normal
    }

    @Test
    fun roundsToNearestEven() {
        assertHalf(0x3555, 1f / 3f)
        assertHalf(0x3C00, 1.00048828125f) // halfway between 1 and the next half, rounds to even (down)
        assertHalf(0x3C02, 1.00146484375f) // halfway between 0x3C01 and 0x3C02, rounds to even (up)
        assertHalf(0x0000, 2.9802322e-8f) // half of the smallest subnormal, rounds to even (zero)
    }

    @Test
    fun convertsSpecialValues() {
        assertHalf(0x7C00, Float.POSITIVE_INFINITY)
        assertHalf(0xFC00, Float.NEGATIVE_INFINITY)
        assertHalf(0x7C00, 65520f) // rounds up past the largest half
        assertHalf(0x7C00, 1e10f)
        assertEquals(0x7E00, floatToHalf(Float.NaN).toInt() and 0xFFFF)
    }

    private fun assertHalf(expected: Int, value: Float) {
        assertEquals(expected, floatToHalf(value).toInt() and 0xFFFF, "floatToHalf($value)")
    }
}
