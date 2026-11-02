package com.appmattus.coil3.pgm

import com.appmattus.coil3.core.NetpbmFormat
import com.appmattus.coil3.core.RasterImage
import com.appmattus.coil3.core.readNetpbmImage
import okio.BufferedSource

/** Parses the Netpbm PGM formats `P2` (plain) and `P5` (raw). */
internal object PgmParser {
    fun parse(source: BufferedSource): RasterImage = readNetpbmImage(source, NetpbmFormat.Pgm)
}
