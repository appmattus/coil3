package com.appmattus.coil3.netpbm

import com.appmattus.coil3.core.Argb8888Raster
import com.appmattus.coil3.core.RasterImage
import com.appmattus.coil3.core.Rgba16Raster
import com.appmattus.coil3.core.scaleSample
import okio.BufferedSource
import okio.EOFException
import okio.IOException

/** How the samples of each tuple map to color and alpha. */
private enum class Layout(val depth: Int, val isGray: Boolean, val hasAlpha: Boolean) {
    Gray(depth = 1, isGray = true, hasAlpha = false),
    GrayAlpha(depth = 2, isGray = true, hasAlpha = true),
    Rgb(depth = 3, isGray = false, hasAlpha = false),
    RgbAlpha(depth = 4, isGray = false, hasAlpha = true),
}

/** Parses the Netpbm PAM format (`P7`). */
internal object PamParser {

    fun parse(source: BufferedSource): RasterImage {
        val header = readHeader(source)
        val layout = layoutOf(header.tupleType, header.depth)

        val pixelCount = header.width.toLong() * header.height
        if (pixelCount * 4 > Int.MAX_VALUE) {
            throw IOException("PAM image is too large: ${header.width}x${header.height}.")
        }

        val maxValue = header.maxValue
        // Samples are one byte when the max value fits in a byte, otherwise two bytes, most significant first.
        val readSample: () -> Int = if (maxValue < 256) {
            { source.readByte().toInt() and 0xFF }
        } else {
            { source.readShort().toInt() and 0xFFFF }
        }

        return if (maxValue < 256) {
            val pixels = IntArray(pixelCount.toInt()) {
                val r = scale(readSample(), maxValue, 255)
                val g = if (layout.isGray) r else scale(readSample(), maxValue, 255)
                val b = if (layout.isGray) r else scale(readSample(), maxValue, 255)
                val a = if (layout.hasAlpha) scale(readSample(), maxValue, 255) else 255
                (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            Argb8888Raster(header.width, header.height, layout.hasAlpha, pixels)
        } else {
            val samples = ShortArray(pixelCount.toInt() * 4)
            for (i in 0 until pixelCount.toInt()) {
                val r = scale(readSample(), maxValue, 65535)
                samples[i * 4] = r.toShort()
                samples[i * 4 + 1] = (if (layout.isGray) r else scale(readSample(), maxValue, 65535)).toShort()
                samples[i * 4 + 2] = (if (layout.isGray) r else scale(readSample(), maxValue, 65535)).toShort()
                samples[i * 4 + 3] = (if (layout.hasAlpha) scale(readSample(), maxValue, 65535) else 65535).toShort()
            }
            Rgba16Raster(header.width, header.height, layout.hasAlpha, samples)
        }
    }

    private class Header(val width: Int, val height: Int, val depth: Int, val maxValue: Int, val tupleType: String)

    private fun readHeader(source: BufferedSource): Header {
        if (readLine(source).trimEnd() != "P7") throw IOException("Not a PAM image: missing 'P7' magic number.")

        var width: Int? = null
        var height: Int? = null
        var depth: Int? = null
        var maxValue: Int? = null
        val tupleTypes = mutableListOf<String>()

        while (true) {
            val line = readLine(source).trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val parts = line.split(WHITESPACE, limit = 2)
            val keyword = parts[0]
            val value = parts.getOrElse(1) { "" }.trim()

            when (keyword) {
                "ENDHDR" -> break
                "WIDTH" -> width = parsePositiveInt(value, keyword)
                "HEIGHT" -> height = parsePositiveInt(value, keyword)
                "DEPTH" -> depth = parsePositiveInt(value, keyword)
                "MAXVAL" -> maxValue = parsePositiveInt(value, keyword).also {
                    if (it > MAX_VALUE) throw IOException("Invalid PAM MAXVAL: $it. Must be at most $MAX_VALUE.")
                }
                // A tuple type may be split over several lines, which are joined with a space.
                "TUPLTYPE" -> if (value.isNotEmpty()) tupleTypes += value
                // Ignore unknown header lines.
            }
        }

        return Header(
            width = width ?: throw missing("WIDTH"),
            height = height ?: throw missing("HEIGHT"),
            depth = depth ?: throw missing("DEPTH"),
            maxValue = maxValue ?: throw missing("MAXVAL"),
            tupleType = tupleTypes.joinToString(" "),
        )
    }

    private fun layoutOf(tupleType: String, depth: Int): Layout {
        val layout = when (tupleType) {
            "BLACKANDWHITE", "GRAYSCALE" -> Layout.Gray
            "BLACKANDWHITE_ALPHA", "GRAYSCALE_ALPHA" -> Layout.GrayAlpha
            "RGB" -> Layout.Rgb
            "RGB_ALPHA" -> Layout.RgbAlpha
            // Unknown or missing tuple type: interpret the samples by depth.
            else -> Layout.entries.firstOrNull { it.depth == depth }
                ?: throw IOException("Unsupported PAM DEPTH $depth for TUPLTYPE '$tupleType'.")
        }
        if (layout.depth != depth) {
            throw IOException("PAM TUPLTYPE $tupleType requires DEPTH ${layout.depth}, but DEPTH is $depth.")
        }
        return layout
    }

    /** Scales [sample] from `0..maxValue` to `0..target`, rounding to the nearest value. */
    private fun scale(sample: Int, maxValue: Int, target: Int): Int {
        if (sample > maxValue) throw IOException("PAM sample $sample is outside the range 0..$maxValue.")
        return scaleSample(sample, maxValue, target)
    }

    private fun readLine(source: BufferedSource): String = try {
        source.readUtf8LineStrict(MAX_LINE_LENGTH)
    } catch (e: EOFException) {
        throw IOException("Unexpected end of PAM header.", e)
    }

    private fun parsePositiveInt(value: String, keyword: String): Int {
        val number = value.toIntOrNull()
        if (number == null || number <= 0) throw IOException("Invalid PAM $keyword: '$value'.")
        return number
    }

    private fun missing(keyword: String) = IOException("PAM header is missing $keyword.")

    private val WHITESPACE = Regex("\\s+")
    private const val MAX_LINE_LENGTH = 4096L
    private const val MAX_VALUE = 65535
}
