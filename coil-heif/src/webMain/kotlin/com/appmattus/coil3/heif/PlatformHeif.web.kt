package com.appmattus.coil3.heif

import androidx.compose.ui.graphics.ImageBitmap

// Skia can't decode HEVC, and of the major browsers only Safari can.
internal actual val isHeifDecodingSupported: Boolean = false

internal actual fun decodeHeif(bytes: ByteArray): ImageBitmap =
    throw UnsupportedOperationException("HEIF decoding isn't supported in the browser.")
