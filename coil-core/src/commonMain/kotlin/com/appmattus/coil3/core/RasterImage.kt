package com.appmattus.coil3.core

/** Decoded pixels, ready to become a bitmap. Color samples are never premultiplied by alpha. */
@InternalCoilDecoderApi
public sealed interface RasterImage {
    public val width: Int
    public val height: Int
    public val hasAlpha: Boolean
}

/** An image with at most 8 bits per sample, stored as one ARGB_8888 pixel per entry in row-major order. */
@InternalCoilDecoderApi
public class Argb8888Raster(
    override val width: Int,
    override val height: Int,
    override val hasAlpha: Boolean,
    public val pixels: IntArray,
) : RasterImage

/**
 * An image with more than 8 bits per sample, stored as four unsigned 16-bit samples (red, green, blue, alpha) per
 * pixel in row-major order, using the full `0..65535` range. Alpha is `65535` when the image has none.
 */
@InternalCoilDecoderApi
public class Rgba16Raster(
    override val width: Int,
    override val height: Int,
    override val hasAlpha: Boolean,
    public val samples: ShortArray,
) : RasterImage {

    /** Reduces this image to 8 bits per sample, for platforms without 16-bit bitmaps. */
    public fun toArgb8888(): IntArray = IntArray(width * height) { i ->
        val r = to8Bit(samples[i * 4])
        val g = to8Bit(samples[i * 4 + 1])
        val b = to8Bit(samples[i * 4 + 2])
        val a = to8Bit(samples[i * 4 + 3])
        (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Converts this image to RGBA half-float samples, as used by RGBA_F16 bitmaps. When [premultiplied] is true the
     * color samples are multiplied by alpha, as Android requires.
     */
    public fun toRgbaF16(premultiplied: Boolean): ShortArray {
        val rgba = ShortArray(width * height * 4)
        for (i in 0 until width * height) {
            val alpha = unorm(samples[i * 4 + 3])
            val factor = if (premultiplied) alpha else 1f
            rgba[i * 4] = floatToHalf(unorm(samples[i * 4]) * factor)
            rgba[i * 4 + 1] = floatToHalf(unorm(samples[i * 4 + 1]) * factor)
            rgba[i * 4 + 2] = floatToHalf(unorm(samples[i * 4 + 2]) * factor)
            rgba[i * 4 + 3] = floatToHalf(alpha)
        }
        return rgba
    }

    private fun to8Bit(sample: Short): Int = ((sample.toInt() and 0xFFFF) * 255 + 32767) / 65535

    private fun unorm(sample: Short): Float = (sample.toInt() and 0xFFFF) / 65535f
}

/** Scales [sample] from `0..maxValue` to `0..target`, rounding to the nearest value. */
@InternalCoilDecoderApi
public fun scaleSample(sample: Int, maxValue: Int, target: Int): Int =
    ((sample.toLong() * target + maxValue / 2) / maxValue).toInt()
