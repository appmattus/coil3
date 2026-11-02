package com.appmattus.coil3.netpbm

import com.appmattus.coil3.core.RasterImage
import okio.BufferedSource

/** Parses the Netpbm PPM formats `P3` (ASCII) and `P6` (binary). */
internal object PpmParser {
    fun parse(source: BufferedSource): RasterImage = readNetpbmImage(source, NetpbmFormat.Ppm)
}
