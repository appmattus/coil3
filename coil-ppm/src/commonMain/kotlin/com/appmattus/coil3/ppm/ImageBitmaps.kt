package com.appmattus.coil3.ppm

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image

/** Creates an [ImageBitmap] from [image]: ARGB_8888 for 8-bit images and RGBA_F16 (where supported) for 16-bit. */
internal expect fun createImageBitmap(image: PpmImage): ImageBitmap

/** Wraps this [ImageBitmap] as a Coil [Image]. */
internal expect fun ImageBitmap.asCoilImage(): Image
