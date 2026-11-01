package com.appmattus.coil3.ppm

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image

/** Creates an [ImageBitmap] from opaque ARGB_8888 [pixels] in row-major order. */
internal expect fun createImageBitmap(width: Int, height: Int, pixels: IntArray): ImageBitmap

/** Wraps this [ImageBitmap] as a Coil [Image]. */
internal expect fun ImageBitmap.asCoilImage(): Image
