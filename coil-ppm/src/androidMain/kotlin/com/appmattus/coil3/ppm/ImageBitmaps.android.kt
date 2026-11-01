package com.appmattus.coil3.ppm

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

internal actual fun createImageBitmap(image: PpmImage): ImageBitmap {
    val bitmap = when (image) {
        is Ppm8BitImage -> createArgb8888Bitmap(image.width, image.height, image.pixels)
        is Ppm16BitImage -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createRgbaF16Bitmap(image)
        } else {
            createArgb8888Bitmap(image.width, image.height, image.toArgb8888())
        }
    }
    return bitmap.asImageBitmap()
}

private fun createArgb8888Bitmap(width: Int, height: Int, pixels: IntArray): Bitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

@RequiresApi(Build.VERSION_CODES.O)
private fun createRgbaF16Bitmap(image: Ppm16BitImage): Bitmap {
    // PPM samples are gamma encoded, so use (extended) sRGB rather than the linear default for RGBA_F16.
    val bitmap = Bitmap.createBitmap(
        image.width,
        image.height,
        Bitmap.Config.RGBA_F16,
        false,
        ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB),
    )
    // RGBA_F16 stores each pixel as four half floats (R, G, B, A) in native byte order.
    bitmap.copyPixelsFromBuffer(ShortBuffer.wrap(image.toRgbaF16()))
    return bitmap
}

internal actual fun ImageBitmap.asCoilImage(): Image = asAndroidBitmap().asImage()
