package com.appmattus.coil3.heif

import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.nio.ByteBuffer

// BitmapFactory and ImageDecoder gained HEIF support in Android 9 (API 28).
internal actual val isHeifDecodingSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

internal actual fun decodeHeif(bytes: ByteArray): ImageBitmap {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        throw UnsupportedOperationException("HEIF decoding needs Android 9 (API 28) or later.")
    }
    // ImageDecoder applies the image's rotation and mirroring. It throws ImageDecoder.DecodeException, an
    // IOException, if the data can't be decoded.
    val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
    return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
        // Software bitmaps can be read back and drawn anywhere, as with the other decoders here.
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }.asImageBitmap()
}
