package com.appmattus.coil3.heif

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeifDecoderTest {

    @Test
    fun recognisesHeicFiles() {
        assertTrue(HeifDecoder.hasHeifBrand(Buffer().write(HeifFixtures.quadrants)))
        assertTrue(HeifDecoder.hasHeifBrand(Buffer().write(HeifFixtures.halfTransparent)))
    }

    @Test
    fun recognisesHevcMajorBrands() {
        for (brand in listOf("heic", "heix", "heim", "heis", "hevc", "hevx", "hevm", "hevs")) {
            assertTrue(isHeif(ftyp(brand)), brand)
        }
    }

    @Test
    fun recognisesHevcCompatibleBrand() {
        assertTrue(isHeif(ftyp("mif1", "miaf", "heic")))
        assertTrue(isHeif(ftyp("msf1", "hevc")))
    }

    @Test
    fun rejectsOtherBrands() {
        assertFalse(isHeif(ftyp("mif1"))) // Generic HEIF, which could hold any codec.
        assertFalse(isHeif(ftyp("avif", "mif1", "miaf"))) // AVIF
        assertFalse(isHeif(ftyp("isom", "mp41"))) // MP4
        assertFalse(isHeif(ftyp("qt  "))) // QuickTime
    }

    @Test
    fun rejectsNonFtypData() {
        assertFalse(isHeif(byteArrayOf()))
        assertFalse(isHeif(ftyp("heic").copyOf(15)))
        assertFalse(isHeif("\u0089PNG\r\n\u001A\n\u0000\u0000\u0000\rIHDR".encodeToByteArray()))
        assertFalse(isHeif(ftyp("heic").also { it[4] = 'm'.code.toByte() })) // "mtyp"
    }

    @Test
    fun rejectsMalformedBoxSize() {
        assertFalse(isHeif(ftyp("heic").also { it[3] = 8 })) // Smaller than the brand fields.
        assertFalse(isHeif(ftyp("heic").also { it[3] = 18 })) // Not a whole number of brands.
    }

    @Test
    fun rejectsTruncatedCompatibleBrands() {
        val box = ftyp("mif1", "miaf", "heic")
        assertFalse(isHeif(box.copyOf(box.size - 4)))
    }

    @Test
    fun ignoresBrandsBeyondTheLimit() {
        val brands = List(60) { "miaf" } + "heic"
        assertFalse(isHeif(ftyp("mif1", *brands.toTypedArray())))
        assertTrue(isHeif(ftyp("mif1", *brands.drop(1).toTypedArray())))
    }

    @Test
    fun peekingBrandsDoesNotConsumeSource() {
        val source = Buffer().write(HeifFixtures.quadrants)

        assertTrue(HeifDecoder.hasHeifBrand(source))
        assertContentEquals(HeifFixtures.quadrants, source.readByteArray())
    }

    private fun isHeif(bytes: ByteArray) = HeifDecoder.hasHeifBrand(Buffer().write(bytes))

    /** Builds an `ftyp` box with [majorBrand] and [compatibleBrands]. */
    private fun ftyp(majorBrand: String, vararg compatibleBrands: String): ByteArray {
        val buffer = Buffer()
            .writeInt(16 + compatibleBrands.size * 4)
            .writeUtf8("ftyp")
            .writeUtf8(majorBrand)
            .writeInt(0)
        compatibleBrands.forEach { buffer.writeUtf8(it) }
        return buffer.readByteArray()
    }
}
