package com.appmattus.coil3.ppm

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
 * A [Decoder] for Netpbm PPM images in both ASCII (`P3`) and binary (`P6`) form.
 *
 * Pixels are decoded into a Compose [androidx.compose.ui.graphics.ImageBitmap], which is then wrapped as a
 * Coil [coil3.Image] so it can be displayed on every supported platform.
 */
public class PpmDecoder(
    private val source: ImageSource,
    @Suppress("unused") private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val ppm = source.source().use(PpmParser::parse)
        val imageBitmap = createImageBitmap(ppm.width, ppm.height, ppm.pixels)

        return DecodeResult(
            image = imageBitmap.asCoilImage(),
            isSampled = false,
        )
    }

    /** Registers [PpmDecoder] with an [ImageLoader]. */
    public class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return PpmDecoder(result.source, options)
        }

        private fun isApplicable(result: SourceFetchResult): Boolean =
            result.mimeType == MIME_TYPE ||
                result.source.fileOrNull()?.name?.endsWith(".ppm", ignoreCase = true) == true ||
                hasPpmMagicNumber(result.source.source())
    }

    public companion object {
        /** The MIME type of PPM images. */
        public const val MIME_TYPE: String = "image/x-portable-pixmap"

        private val P3 = "P3".encodeUtf8()
        private val P6 = "P6".encodeUtf8()

        private fun hasPpmMagicNumber(source: BufferedSource): Boolean =
            source.rangeEquals(0, P3) || source.rangeEquals(0, P6)
    }
}
