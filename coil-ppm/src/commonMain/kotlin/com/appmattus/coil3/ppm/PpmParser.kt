package com.appmattus.coil3.ppm

import okio.BufferedSource
import okio.IOException

/** A decoded PPM image, with one ARGB_8888 pixel per entry in row-major order. */
internal class PpmImage(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
)

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
        if (pixelCount > Int.MAX_VALUE) {
            throw IOException("PPM image is too large: ${width}x$height.")
        }

        val readSample: () -> Int = when {
            magicNumber == "P3" -> { -> readToken(source).toIntOrNull() ?: throw IOException("Invalid PPM sample.") }
            maxColorValue < 256 -> { -> source.readByte().toInt() and 0xFF }
            // Binary samples wider than one byte are stored as two bytes, most significant first.
            else -> { -> source.readShort().toInt() and 0xFFFF }
        }

        val pixels = IntArray(pixelCount.toInt())
        for (i in pixels.indices) {
            val r = scale(readSample(), maxColorValue)
            val g = scale(readSample(), maxColorValue)
            val b = scale(readSample(), maxColorValue)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        return PpmImage(width, height, pixels)
    }

    /** Scales [sample] from `0..maxColorValue` to `0..255`, rounding to the nearest value. */
    private fun scale(sample: Int, maxColorValue: Int): Int {
        if (sample !in 0..maxColorValue) {
            throw IOException("PPM sample $sample is outside the range 0..$maxColorValue.")
        }
        return (sample * 255 + maxColorValue / 2) / maxColorValue
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
