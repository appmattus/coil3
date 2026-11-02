package com.appmattus.coil3.heif

import com.appmattus.coil3.core.toDecodeResult
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.BufferedSource
import okio.use

/**
 * A [Decoder] for HEIF images with HEVC-coded content, such as the HEIC photos taken by iPhones.
 *
 * Decoding uses the platform's own HEIF support, so it is only available on:
 *
 * - Android API 28 and above, through `ImageDecoder`.
 * - iOS and macOS, through ImageIO.
 *
 * Elsewhere (JVM, JS, Wasm and older Android versions) the [Factory] returns null, so Coil tries its other decoders.
 *
 * Images are rotated and mirrored as their metadata says, and are decoded at full size into a Compose
 * [androidx.compose.ui.graphics.ImageBitmap], which is then wrapped as a Coil [coil3.Image]. Only the primary image
 * is decoded.
 */
public class HeifDecoder(
    private val source: ImageSource,
    @Suppress("unused") private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        return decodeHeif(bytes).toDecodeResult()
    }

    /** Registers [HeifDecoder] with an [ImageLoader]. Does nothing on platforms that can't decode HEIF. */
    public class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isHeifDecodingSupported || !isApplicable(result)) return null
            return HeifDecoder(result.source, options)
        }

        private fun isApplicable(result: SourceFetchResult): Boolean =
            result.mimeType in MIME_TYPES ||
                result.source.fileOrNull()?.name?.substringAfterLast('.', "")?.lowercase() in FILE_EXTENSIONS ||
                hasHeifBrand(result.source.source())
    }

    public companion object {
        /** The MIME types of HEIF images and image sequences. */
        public val MIME_TYPES: Set<String> = setOf(
            "image/heic",
            "image/heif",
            "image/heic-sequence",
            "image/heif-sequence",
        )

        private val FILE_EXTENSIONS = setOf("heic", "heif", "hif")

        /** The brands that mark a file as HEIF with HEVC-coded images (ISO/IEC 23008-12 Annex B). */
        private val HEVC_BRANDS = setOf("heic", "heix", "heim", "heis", "hevc", "hevx", "hevm", "hevs")

        /** Brands are read from at most this many bytes, which leaves room for 60 compatible brands. */
        private const val MAX_FTYP_SIZE = 256L

        /**
         * Returns true if [source] starts with an ISO base media file `ftyp` box whose major or compatible brands
         * include an HEVC HEIF brand, such as `heic`. Files that are only marked with the generic `mif1` or `msf1`
         * brands aren't matched, as they could just as well hold AVIF or JPEG images.
         */
        internal fun hasHeifBrand(source: BufferedSource): Boolean {
            val peek = source.peek()
            if (!peek.request(16)) return false
            val boxSize = peek.readInt().toLong() and 0xFFFFFFFFL
            if (peek.readUtf8(4) != "ftyp" || boxSize < 16 || boxSize % 4 != 0L) return false

            val majorBrand = peek.readUtf8(4)
            peek.skip(4) // minor version
            if (majorBrand in HEVC_BRANDS) return true

            val compatibleBrandCount = (minOf(boxSize, MAX_FTYP_SIZE) - 16) / 4
            for (i in 0 until compatibleBrandCount) {
                if (!peek.request(4)) return false
                if (peek.readUtf8(4) in HEVC_BRANDS) return true
            }
            return false
        }
    }
}
