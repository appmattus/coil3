package com.appmattus.coil3.netpbm

import com.appmattus.coil3.core.RasterImage
import okio.BufferedSource
import okio.IOException

/** Parses any Netpbm image, choosing the format from its magic number. */
internal object NetpbmParser {

    fun parse(source: BufferedSource): RasterImage {
        val peek = source.peek()
        val magicNumber = if (peek.request(2)) peek.readUtf8(2) else ""

        return when (magicNumber) {
            "P1", "P4" -> PbmParser.parse(source)
            "P2", "P5" -> PgmParser.parse(source)
            "P3", "P6" -> PpmParser.parse(source)
            "P7" -> PamParser.parse(source)
            else -> throw IOException("Not a Netpbm image: unknown magic number '$magicNumber'.")
        }
    }
}
