package com.appmattus.coil3.pam

import com.appmattus.coil3.core.toDecodeResult
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import okio.use

/**
 * A [Decoder] for Netpbm PAM (`P7`) images.
 *
 * Supports the standard tuple types `BLACKANDWHITE`, `GRAYSCALE` and `RGB`, each with or without `_ALPHA`. Images
 * with any other tuple type are read by their depth: 1 = grayscale, 2 = grayscale + alpha, 3 = RGB, 4 = RGB + alpha.
 *
 * Pixels are decoded into a Compose [androidx.compose.ui.graphics.ImageBitmap], which is then wrapped as a Coil
 * [coil3.Image]. Images with a max value above 255 are decoded to a 16-bit (RGBA_F16) bitmap so the extra precision
 * is kept, except on Android before API 26, where they are reduced to 8 bits per sample.
 */
public class PamDecoder(
    private val source: ImageSource,
    @Suppress("unused") private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        return source.source().use(PamParser::parse).toDecodeResult()
    }

    /** Registers [PamDecoder] with an [ImageLoader]. */
    public class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return PamDecoder(result.source, options)
        }

        private fun isApplicable(result: SourceFetchResult): Boolean =
            result.mimeType == MIME_TYPE ||
                result.source.fileOrNull()?.name?.endsWith(".pam", ignoreCase = true) == true ||
                hasPamMagicNumber(result.source.source())
    }

    public companion object {
        /** The MIME type of PAM images. */
        public const val MIME_TYPE: String = "image/x-portable-arbitrarymap"

        // "P7" must be followed by a newline. This avoids matching XV thumbnails, which start with "P7 332".
        private val P7_LF = "P7\n".encodeUtf8()
        private val P7_CRLF = "P7\r\n".encodeUtf8()

        internal fun hasPamMagicNumber(source: BufferedSource): Boolean =
            source.rangeEquals(0, P7_LF) || source.rangeEquals(0, P7_CRLF)
    }
}
