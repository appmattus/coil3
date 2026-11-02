package com.appmattus.coil3.netpbm

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
 * A [Decoder] for the Netpbm image formats:
 *
 * - PBM (bitmap): `P1` (plain) and `P4` (raw).
 * - PGM (grayscale): `P2` (plain) and `P5` (raw).
 * - PPM (RGB): `P3` (plain) and `P6` (raw).
 * - PAM: `P7`, with the `BLACKANDWHITE`, `GRAYSCALE` and `RGB` tuple types, each with or without `_ALPHA`. Other
 *   tuple types are read by depth: 1 = grayscale, 2 = grayscale + alpha, 3 = RGB, 4 = RGB + alpha.
 *
 * Pixels are decoded into a Compose [androidx.compose.ui.graphics.ImageBitmap], which is then wrapped as a Coil
 * [coil3.Image]. Images with a maxval above 255 are decoded to a 16-bit (RGBA_F16) bitmap so the extra precision is
 * kept, except on Android before API 26, where they are reduced to 8 bits per sample.
 */
public class NetpbmDecoder(
    private val source: ImageSource,
    @Suppress("unused") private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        return source.source().use(NetpbmParser::parse).toDecodeResult()
    }

    /** Registers [NetpbmDecoder] with an [ImageLoader]. */
    public class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return NetpbmDecoder(result.source, options)
        }

        private fun isApplicable(result: SourceFetchResult): Boolean =
            result.mimeType in MIME_TYPES ||
                result.source.fileOrNull()?.name?.substringAfterLast('.', "")?.lowercase() in FILE_EXTENSIONS ||
                hasNetpbmMagicNumber(result.source.source())
    }

    public companion object {
        /** The MIME types of the Netpbm formats. */
        public val MIME_TYPES: Set<String> = setOf(
            "image/x-portable-bitmap",
            "image/x-portable-graymap",
            "image/x-portable-pixmap",
            "image/x-portable-anymap",
            "image/x-portable-arbitrarymap",
        )

        private val FILE_EXTENSIONS = setOf("pbm", "pgm", "ppm", "pnm", "pam")

        /**
         * Returns true if [source] starts with a Netpbm magic number: `P1` to `P6` followed by whitespace or a
         * comment, or `P7` followed by a newline (which avoids matching XV thumbnails, which start with `P7 332`).
         */
        internal fun hasNetpbmMagicNumber(source: BufferedSource): Boolean {
            val peek = source.peek()
            if (!peek.request(3)) return false
            val bytes = peek.readByteArray(3)
            if (bytes[0] != 'P'.code.toByte()) return false
            val next = bytes[2].toInt().toChar()
            return when (bytes[1].toInt().toChar()) {
                in '1'..'6' -> next.isWhitespace() || next == '#'
                '7' -> next == '\n' || next == '\r'
                else -> false
            }
        }
    }
}
