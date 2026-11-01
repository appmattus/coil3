package com.appmattus.coil3.ppm

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.Image
import coil3.asImage
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual fun createImageBitmap(width: Int, height: Int, pixels: IntArray): ImageBitmap {
    // Skia takes raw bytes, so lay each ARGB pixel out as R, G, B, A.
    val bytes = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val argb = pixels[i]
        val offset = i * 4
        bytes[offset] = (argb shr 16).toByte()
        bytes[offset + 1] = (argb shr 8).toByte()
        bytes[offset + 2] = argb.toByte()
        bytes[offset + 3] = (argb ushr 24).toByte()
    }

    val bitmap = Bitmap()
    val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
    check(bitmap.allocPixels(info)) { "Unable to allocate a ${width}x$height bitmap." }
    check(bitmap.installPixels(bytes)) { "Unable to install pixels into a ${width}x$height bitmap." }
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}

internal actual fun ImageBitmap.asCoilImage(): Image = asSkiaBitmap().asImage()
