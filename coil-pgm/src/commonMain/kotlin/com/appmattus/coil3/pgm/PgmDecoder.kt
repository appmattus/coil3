package com.appmattus.coil3.pgm

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
 * A [Decoder] for Netpbm PGM (grayscale) images in both plain (`P2`) and raw (`P5`) form.
 *
 * Pixels are decoded into a Compose [androidx.compose.ui.graphics.ImageBitmap], which is then wrapped as a Coil
 * [coil3.Image]. Images with a maxval above 255 are decoded to a 16-bit (RGBA_F16) bitmap so the extra precision is
 * kept, except on Android before API 26, where they are reduced to 8 bits per sample.
 */
public class PgmDecoder(
    private val source: ImageSource,
    @Suppress("unused") private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        return source.source().use(PgmParser::parse).toDecodeResult()
    }

    /** Registers [PgmDecoder] with an [ImageLoader]. */
    public class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return PgmDecoder(result.source, options)
        }

        private fun isApplicable(result: SourceFetchResult): Boolean =
            result.mimeType == MIME_TYPE ||
                result.source.fileOrNull()?.name?.endsWith(".pgm", ignoreCase = true) == true ||
                hasPgmMagicNumber(result.source.source())
    }

    public companion object {
        /** The MIME type of PGM images. */
        public const val MIME_TYPE: String = "image/x-portable-graymap"

        private val P2 = "P2".encodeUtf8()
        private val P5 = "P5".encodeUtf8()

        internal fun hasPgmMagicNumber(source: BufferedSource): Boolean =
            source.rangeEquals(0, P2) || source.rangeEquals(0, P5)
    }
}
