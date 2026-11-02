package com.appmattus.coil3.heif

import androidx.compose.ui.graphics.ImageBitmap

/** Whether this platform can decode HEIF images. */
internal expect val isHeifDecodingSupported: Boolean

/**
 * Decodes the primary image of the HEIF file in [bytes], applying its rotation and mirroring.
 *
 * @throws okio.IOException if the data can't be decoded.
 * @throws UnsupportedOperationException if [isHeifDecodingSupported] is false.
 */
internal expect fun decodeHeif(bytes: ByteArray): ImageBitmap
