package com.appmattus.coil3.ppm

import okio.BufferedSource
import okio.IOException

/** A decoded PPM image. */
internal sealed interface PpmImage {
    val width: Int
    val height: Int
}

/** An image with at most 8 bits per sample, stored as one ARGB_8888 pixel per entry in row-major order. */
internal class Ppm8BitImage(
    override val width: Int,
    override val height: Int,
    val pixels: IntArray,
) : PpmImage

/**
 * An image with more than 8 bits per sample (max color value above 255), stored as three unsigned 16-bit samples
 * (red, green, blue) per pixel in row-major order, scaled to the full `0..65535` range.
 */
internal class Ppm16BitImage(
    override val width: Int,
    override val height: Int,
    val samples: ShortArray,
) : PpmImage {

    /** Reduces this image to 8 bits per sample, for platforms without 16-bit bitmaps. */
    fun toArgb8888(): IntArray = IntArray(width * height) { i ->
        val r = to8Bit(samples[i * 3])
        val g = to8Bit(samples[i * 3 + 1])
        val b = to8Bit(samples[i * 3 + 2])
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Converts this image to RGBA half-float samples, as used by RGBA_F16 bitmaps. Alpha is always 1. */
    fun toRgbaF16(): ShortArray {
        val rgba = ShortArray(width * height * 4)
        for (i in 0 until width * height) {
            rgba[i * 4] = unormToHalf(samples[i * 3])
            rgba[i * 4 + 1] = unormToHalf(samples[i * 3 + 1])
            rgba[i * 4 + 2] = unormToHalf(samples[i * 3 + 2])
            rgba[i * 4 + 3] = HALF_ONE
        }
        return rgba
    }

    private fun to8Bit(sample: Short): Int = ((sample.toInt() and 0xFFFF) * 255 + 32767) / 65535

    private fun unormToHalf(sample: Short): Short = floatToHalf((sample.toInt() and 0xFFFF) / 65535f)
}

/** Parses the Netpbm PPM formats `P3` (ASCII) and `P6` (binary). */
internal object PpmParser {

    fun parse(source: BufferedSource): PpmImage {
        val magicNumber = readToken(source)
        if (magicNumber != "P3" && magicNumber != "P6") {
            throw IOException("Unsupported PPM format: '$magicNumber'. Only P3 and P6 are supported.")
        }

        val width = readPositiveInt(source, "width")
        val height = readPositiveInt(source, "height")
        val maxColorValue = readPositiveInt(source, "max color value")
        if (maxColorValue > MAX_COLOR_VALUE) {
            throw IOException("Invalid PPM max color value: $maxColorValue. Must be at most $MAX_COLOR_VALUE.")
        }

        val pixelCount = width.toLong() * height
        if (pixelCount * 3 > Int.MAX_VALUE) {
            throw IOException("PPM image is too large: ${width}x$height.")
        }

        val readSample: () -> Int = when {
            magicNumber == "P3" -> { -> readToken(source).toIntOrNull() ?: throw IOException("Invalid PPM sample.") }
            maxColorValue < 256 -> { -> source.readByte().toInt() and 0xFF }
            // Binary samples wider than one byte are stored as two bytes, most significant first.
            else -> { -> source.readShort().toInt() and 0xFFFF }
        }

        return if (maxColorValue < 256) {
            val pixels = IntArray(pixelCount.toInt()) {
                val r = scale(readSample(), maxColorValue, 255)
                val g = scale(readSample(), maxColorValue, 255)
                val b = scale(readSample(), maxColorValue, 255)
                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            Ppm8BitImage(width, height, pixels)
        } else {
            val samples = ShortArray(pixelCount.toInt() * 3) {
                scale(readSample(), maxColorValue, 65535).toShort()
            }
            Ppm16BitImage(width, height, samples)
        }
    }

    /** Scales [sample] from `0..maxColorValue` to `0..target`, rounding to the nearest value. */
    private fun scale(sample: Int, maxColorValue: Int, target: Int): Int {
        if (sample !in 0..maxColorValue) {
            throw IOException("PPM sample $sample is outside the range 0..$maxColorValue.")
        }
        return ((sample.toLong() * target + maxColorValue / 2) / maxColorValue).toInt()
    }

    private fun readPositiveInt(source: BufferedSource, name: String): Int {
        val token = readToken(source)
        val value = token.toIntOrNull()
        if (value == null || value <= 0) throw IOException("Invalid PPM $name: '$token'.")
        return value
    }

    /**
     * Reads the next whitespace-separated token, skipping `#` comments.
     *
     * Exactly one whitespace character after the token is consumed, which in the header is the single
     * separator that precedes the binary raster in `P6` images.
     */
    private fun readToken(source: BufferedSource): String {
        val sb = StringBuilder()

        while (!source.exhausted()) {
            val ch = (source.readByte().toInt() and 0xFF).toChar()

            if (ch == '#') {
                // Skip the comment to the end of the line.
                while (!source.exhausted()) {
                    val next = source.readByte().toInt().toChar()
                    if (next == '\n' || next == '\r') break
                }
                if (sb.isNotEmpty()) break
                continue
            }

            if (ch.isWhitespace()) {
                if (sb.isNotEmpty()) break
                continue
            }

            sb.append(ch)
        }

        if (sb.isEmpty()) throw IOException("Unexpected end of PPM data.")
        return sb.toString()
    }

    private const val MAX_COLOR_VALUE = 65535
}
