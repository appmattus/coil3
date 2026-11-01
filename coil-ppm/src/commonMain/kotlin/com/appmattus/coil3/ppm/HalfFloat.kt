package com.appmattus.coil3.ppm

/** IEEE 754 half-precision 1.0. */
internal const val HALF_ONE: Short = 0x3C00

/** Converts [value] to an IEEE 754 half-precision float, rounding to nearest even. */
internal fun floatToHalf(value: Float): Short {
    val bits = value.toRawBits()
    val sign = (bits ushr 16) and 0x8000
    val exponent = (bits ushr 23) and 0xFF
    val mantissa = bits and 0x7FFFFF

    // NaN and infinity.
    if (exponent == 0xFF) return (sign or 0x7C00 or (if (mantissa != 0) 0x200 else 0)).toShort()

    val halfExponent = exponent - 127 + 15

    // Too large: overflow to infinity.
    if (halfExponent >= 0x1F) return (sign or 0x7C00).toShort()

    // Too small for a normal half: produce a subnormal, or zero.
    if (halfExponent <= 0) {
        if (halfExponent < -10) return sign.toShort()
        val fullMantissa = mantissa or 0x800000
        val shift = 14 - halfExponent
        return (sign or roundShift(fullMantissa, shift)).toShort()
    }

    // A carry out of the mantissa correctly bumps the exponent, up to infinity.
    return (sign or roundShift((halfExponent shl 23) or mantissa, 13)).toShort()
}

/** Shifts [value] right by [shift] bits, rounding to nearest even. */
private fun roundShift(value: Int, shift: Int): Int {
    val shifted = value ushr shift
    val remainder = value and ((1 shl shift) - 1)
    val halfway = 1 shl (shift - 1)
    return if (remainder > halfway || (remainder == halfway && (shifted and 1) != 0)) shifted + 1 else shifted
}
