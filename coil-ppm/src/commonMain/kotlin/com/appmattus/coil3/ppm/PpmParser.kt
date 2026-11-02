package com.appmattus.coil3.ppm

import com.appmattus.coil3.core.NetpbmFormat
import com.appmattus.coil3.core.RasterImage
import com.appmattus.coil3.core.readNetpbmImage
import okio.BufferedSource

/** Parses the Netpbm PPM formats `P3` (ASCII) and `P6` (binary). */
internal object PpmParser {
    fun parse(source: BufferedSource): RasterImage = readNetpbmImage(source, NetpbmFormat.Ppm)
}
