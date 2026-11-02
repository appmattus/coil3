package com.appmattus.coil3.heif

import androidx.compose.ui.graphics.ImageBitmap

// Neither the JDK nor Skia can decode HEVC.
internal actual val isHeifDecodingSupported: Boolean = false

internal actual fun decodeHeif(bytes: ByteArray): ImageBitmap =
    throw UnsupportedOperationException("HEIF decoding isn't supported on the JVM.")
