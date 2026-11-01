package com.appmattus.coil3.ppm

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.asImage

internal actual fun createImageBitmap(width: Int, height: Int, pixels: IntArray): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()

internal actual fun ImageBitmap.asCoilImage(): Image = asAndroidBitmap().asImage()
