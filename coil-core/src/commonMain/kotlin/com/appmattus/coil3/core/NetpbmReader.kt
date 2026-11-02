package com.appmattus.coil3.core

import okio.BufferedSource
import okio.IOException

/** The color model of a Netpbm format that uses the `width height maxval` header (PGM and PPM). */
@InternalCoilDecoderApi
public enum class NetpbmFormat(
    internal val formatName: String,
    internal val plainMagicNumber: String,
    internal val rawMagicNumber: String,
    internal val channels: Int,
) {
    /** Grayscale: `P2` (plain) and `P5` (raw), one sample per pixel. */
    Pgm(formatName = "PGM", plainMagicNumber = "P2", rawMagicNumber = "P5", channels = 1),

    /** RGB: `P3` (plain) and `P6` (raw), three samples per pixel. */
    Ppm(formatName = "PPM", plainMagicNumber = "P3", rawMagicNumber = "P6", channels = 3),
}

/**
 * Reads a [format] image from [source].
 *
 * Plain images store samples as whitespace-separated ASCII numbers; raw images store them as one byte when the
 * maxval is below 256, otherwise as two bytes, most significant first. Samples are scaled to the full 8-bit range,
 * or to the full 16-bit range when the maxval is above 255 so the extra precision is kept.
 *
 * Only the first image is read from raw files that contain several.
 */
@InternalCoilDecoderApi
public fun readNetpbmImage(source: BufferedSource, format: NetpbmFormat): RasterImage {
    val reader = NetpbmReader(source, format.formatName)

    val magicNumber = reader.readToken()
    if (magicNumber != format.plainMagicNumber && magicNumber != format.rawMagicNumber) {
        throw IOException(
            "Unsupported ${format.formatName} format: '$magicNumber'. " +
                "Only ${format.plainMagicNumber} and ${format.rawMagicNumber} are supported.",
        )
    }
    val isPlain = magicNumber == format.plainMagicNumber

    val width = reader.readPositiveInt("width")
    val height = reader.readPositiveInt("height")
    val maxValue = reader.readPositiveInt("maxval")
    if (maxValue > MAX_VALUE) {
        throw IOException("Invalid ${format.formatName} maxval: $maxValue. Must be at most $MAX_VALUE.")
    }

    val pixelCount = width.toLong() * height
    if (pixelCount * 4 > Int.MAX_VALUE) {
        throw IOException("${format.formatName} image is too large: ${width}x$height.")
    }

    val readSample: (target: Int) -> Int = { target ->
        val sample = when {
            isPlain -> reader.readToken().toIntOrNull()
                ?: throw IOException("Invalid ${format.formatName} sample.")
            maxValue < 256 -> source.readByte().toInt() and 0xFF
            else -> source.readShort().toInt() and 0xFFFF
        }
        if (sample !in 0..maxValue) {
            throw IOException("${format.formatName} sample $sample is outside the range 0..$maxValue.")
        }
        scaleSample(sample, maxValue, target)
    }
    val isGray = format.channels == 1

    return if (maxValue < 256) {
        val pixels = IntArray(pixelCount.toInt()) {
            val r = readSample(255)
            val g = if (isGray) r else readSample(255)
            val b = if (isGray) r else readSample(255)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        Argb8888Raster(width, height, hasAlpha = false, pixels)
    } else {
        val samples = ShortArray(pixelCount.toInt() * 4)
        for (i in 0 until pixelCount.toInt()) {
            val r = readSample(65535)
            samples[i * 4] = r.toShort()
            samples[i * 4 + 1] = (if (isGray) r else readSample(65535)).toShort()
            samples[i * 4 + 2] = (if (isGray) r else readSample(65535)).toShort()
            samples[i * 4 + 3] = OPAQUE_16_BIT
        }
        Rgba16Raster(width, height, hasAlpha = false, samples)
    }
}

/** Reads the whitespace-separated tokens of a Netpbm header or plain raster. */
private class NetpbmReader(private val source: BufferedSource, private val formatName: String) {

    fun readPositiveInt(name: String): Int {
        val token = readToken()
        val value = token.toIntOrNull()
        if (value == null || value <= 0) throw IOException("Invalid $formatName $name: '$token'.")
        return value
    }

    /**
     * Reads the next whitespace-separated token, skipping `#` comments.
     *
     * Exactly one whitespace character after the token is consumed, which in the header is the single
     * separator that precedes a raw raster.
     */
    fun readToken(): String {
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

        if (sb.isEmpty()) throw IOException("Unexpected end of $formatName data.")
        return sb.toString()
    }
}

private const val MAX_VALUE = 65535
private const val OPAQUE_16_BIT: Short = -1 // 0xFFFF
