package com.appmattus.coil3.netpbm

import com.appmattus.coil3.core.Argb8888Raster
import com.appmattus.coil3.core.RasterImage
import okio.BufferedSource
import okio.IOException

/**
 * Parses the Netpbm PBM formats `P1` (plain) and `P4` (raw).
 *
 * In PBM, 1 is black and 0 is white. Raw rows pack eight pixels per byte, most significant bit first, and each row
 * is padded to a whole byte. Plain digits need not be separated by whitespace. Only the first image is read from raw
 * files that contain several.
 */
internal object PbmParser {

    fun parse(source: BufferedSource): RasterImage {
        val tokenizer = NetpbmTokenizer(source, "PBM")

        val magicNumber = tokenizer.readToken()
        if (magicNumber != "P1" && magicNumber != "P4") {
            throw IOException("Unsupported PBM format: '$magicNumber'. Only P1 and P4 are supported.")
        }

        val width = tokenizer.readPositiveInt("width")
        val height = tokenizer.readPositiveInt("height")
        val pixels = IntArray(checkedPixelCount(width, height, "PBM"))

        if (magicNumber == "P1") {
            for (i in pixels.indices) {
                pixels[i] = if (readPlainBit(source)) BLACK else WHITE
            }
        } else {
            val row = ByteArray((width + 7) / 8)
            for (y in 0 until height) {
                source.readFully(row)
                for (x in 0 until width) {
                    val bit = (row[x / 8].toInt() shr (7 - x % 8)) and 1
                    pixels[y * width + x] = if (bit == 1) BLACK else WHITE
                }
            }
        }

        return Argb8888Raster(width, height, hasAlpha = false, pixels)
    }

    /** Reads the next `0` or `1` from a plain raster, skipping whitespace. Returns true for `1` (black). */
    private fun readPlainBit(source: BufferedSource): Boolean {
        while (!source.exhausted()) {
            when (val ch = (source.readByte().toInt() and 0xFF).toChar()) {
                '0' -> return false
                '1' -> return true
                else -> if (!ch.isWhitespace()) throw IOException("Invalid PBM pixel: '$ch'.")
            }
        }
        throw IOException("Unexpected end of PBM data.")
    }

    private const val BLACK = 0xFF000000.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
}
