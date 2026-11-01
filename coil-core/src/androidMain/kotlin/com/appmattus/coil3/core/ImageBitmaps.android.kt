package com.appmattus.coil3.core

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.asImage
import java.nio.ShortBuffer

@InternalCoilDecoderApi
internal actual fun RasterImage.toImageBitmap(): ImageBitmap {
    val bitmap = when (this) {
        is Argb8888Raster -> createArgb8888Bitmap(width, height, hasAlpha, pixels)
        is Rgba16Raster -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createRgbaF16Bitmap(this)
        } else {
            createArgb8888Bitmap(width, height, hasAlpha, toArgb8888())
        }
    }
    return bitmap.asImageBitmap()
}

/** [pixels] are unpremultiplied; Android premultiplies them while creating the bitmap. */
private fun createArgb8888Bitmap(width: Int, height: Int, hasAlpha: Boolean, pixels: IntArray): Bitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).apply { setHasAlpha(hasAlpha) }

@InternalCoilDecoderApi
@RequiresApi(Build.VERSION_CODES.O)
private fun createRgbaF16Bitmap(image: Rgba16Raster): Bitmap {
    // Samples are gamma encoded, so use (extended) sRGB rather than the linear default for RGBA_F16.
    val bitmap = Bitmap.createBitmap(
        image.width,
        image.height,
        Bitmap.Config.RGBA_F16,
        image.hasAlpha,
        ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB),
    )
    // RGBA_F16 stores each pixel as four premultiplied half floats (R, G, B, A) in native byte order, and
    // copyPixelsFromBuffer copies them as is.
    bitmap.copyPixelsFromBuffer(ShortBuffer.wrap(image.toRgbaF16(premultiplied = true)))
    return bitmap
}

internal actual fun ImageBitmap.asCoilImage(): Image = asAndroidBitmap().asImage()
