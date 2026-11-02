package com.appmattus.coil3.core

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.Image
import coil3.asImage
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

@InternalCoilDecoderApi
internal actual fun RasterImage.toImageBitmap(): ImageBitmap {
    // Samples are never premultiplied, so tell Skia that rather than converting them.
    val alphaType = if (hasAlpha) ColorAlphaType.UNPREMUL else ColorAlphaType.OPAQUE
    return when (this) {
        is Argb8888Raster -> createBitmap(width, height, ColorType.RGBA_8888, alphaType, rgba8888Bytes(pixels))
        is Rgba16Raster -> createBitmap(
            width,
            height,
            ColorType.RGBA_F16,
            alphaType,
            rgbaF16Bytes(toRgbaF16(premultiplied = false)),
        )
    }.asComposeImageBitmap()
}

private fun createBitmap(
    width: Int,
    height: Int,
    colorType: ColorType,
    alphaType: ColorAlphaType,
    bytes: ByteArray,
): Bitmap {
    val bitmap = Bitmap()
    val info = ImageInfo(width, height, colorType, alphaType, ColorSpace.sRGB)
    check(bitmap.allocPixels(info)) { "Unable to allocate a ${width}x$height bitmap." }
    check(bitmap.installPixels(bytes)) { "Unable to install pixels into a ${width}x$height bitmap." }
    bitmap.setImmutable()
    return bitmap
}

/** Lays each ARGB pixel out as the bytes R, G, B, A. */
private fun rgba8888Bytes(pixels: IntArray): ByteArray {
    val bytes = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val argb = pixels[i]
        val offset = i * 4
        bytes[offset] = (argb shr 16).toByte()
        bytes[offset + 1] = (argb shr 8).toByte()
        bytes[offset + 2] = argb.toByte()
        bytes[offset + 3] = (argb ushr 24).toByte()
    }
    return bytes
}

/** Lays out half-float samples as little-endian bytes, as Skia expects on every supported platform. */
private fun rgbaF16Bytes(samples: ShortArray): ByteArray {
    val bytes = ByteArray(samples.size * 2)
    for (i in samples.indices) {
        val sample = samples[i].toInt()
        bytes[i * 2] = sample.toByte()
        bytes[i * 2 + 1] = (sample shr 8).toByte()
    }
    return bytes
}

internal actual fun ImageBitmap.asCoilImage(): Image = asSkiaBitmap().asImage()
