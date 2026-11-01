package com.appmattus.coil3.core

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image
import coil3.decode.DecodeResult

/**
 * Creates the [DecodeResult] for [this] image: a Compose [ImageBitmap] wrapped as a Coil [Image].
 *
 * 8-bit images become ARGB_8888 bitmaps. 16-bit images become RGBA_F16 bitmaps so the extra precision is kept,
 * except on Android before API 26, where they are reduced to ARGB_8888.
 */
@InternalCoilDecoderApi
public fun RasterImage.toDecodeResult(): DecodeResult = DecodeResult(
    image = toImageBitmap().asCoilImage(),
    isSampled = false,
)

/** Creates an [ImageBitmap] from [this] image. */
@InternalCoilDecoderApi
internal expect fun RasterImage.toImageBitmap(): ImageBitmap

/** Wraps this [ImageBitmap] as a Coil [Image]. */
internal expect fun ImageBitmap.asCoilImage(): Image
