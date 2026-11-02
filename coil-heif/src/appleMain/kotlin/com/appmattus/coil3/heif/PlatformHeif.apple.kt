package com.appmattus.coil3.heif

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import okio.IOException
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateWithName
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreGraphics.kCGColorSpaceSRGB
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform

// ImageIO has decoded HEIF since iOS 11 and macOS 10.13.
internal actual val isHeifDecodingSupported: Boolean = true

@OptIn(ExperimentalForeignApi::class)
internal actual fun decodeHeif(bytes: ByteArray): ImageBitmap {
    val data = bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.convert()) }
        ?: throw IOException("Unable to copy the HEIF data.")
    try {
        val imageSource = CGImageSourceCreateWithData(data, null) ?: throw IOException("Unable to read the HEIF data.")
        try {
            val image = createOrientedImage(imageSource) ?: throw IOException("Unable to decode the HEIF image.")
            try {
                return image.toImageBitmap()
            } finally {
                CGImageRelease(image)
            }
        } finally {
            CFRelease(imageSource)
        }
    } finally {
        CFRelease(data)
    }
}

/**
 * Decodes the primary image with its rotation and mirroring applied. A "thumbnail" without a maximum size is the
 * full image, and unlike CGImageSourceCreateImageAtIndex, it can be transformed as the metadata says.
 */
@OptIn(ExperimentalForeignApi::class)
private fun createOrientedImage(imageSource: platform.ImageIO.CGImageSourceRef): CGImageRef? {
    val options = CFDictionaryCreateMutable(
        null,
        2,
        kCFTypeDictionaryKeyCallBacks.ptr,
        kCFTypeDictionaryValueCallBacks.ptr,
    ) ?: throw IOException("Unable to create the decoding options.")
    try {
        // Always decode the image itself, rather than any smaller thumbnail embedded in the file.
        CFDictionarySetValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
        CFDictionarySetValue(options, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
        return CGImageSourceCreateThumbnailAtIndex(imageSource, 0u, options)
    } finally {
        CFRelease(options)
    }
}

/** Draws this image into premultiplied 8-bit sRGB, the layout Skia calls RGBA_8888. */
@OptIn(ExperimentalForeignApi::class)
private fun CGImageRef.toImageBitmap(): ImageBitmap {
    val width = CGImageGetWidth(this).toInt()
    val height = CGImageGetHeight(this).toInt()
    val bytes = ByteArray(width * height * 4)
    val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB)
    try {
        bytes.usePinned { pinned ->
            val context = CGBitmapContextCreate(
                data = pinned.addressOf(0),
                width = width.convert(),
                height = height.convert(),
                bitsPerComponent = 8u,
                bytesPerRow = (width * 4).convert(),
                space = colorSpace,
                // R, G, B, A bytes in memory, premultiplied.
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big,
            ) ?: throw IOException("Unable to create a ${width}x$height bitmap context.")
            try {
                CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), this)
            } finally {
                CGContextRelease(context)
            }
        }
    } finally {
        CGColorSpaceRelease(colorSpace)
    }

    // ImageIO reports alpha for decoded images even when the file has none, so check the pixels themselves.
    val alphaType = if (hasTranslucentPixel(bytes)) ColorAlphaType.PREMUL else ColorAlphaType.OPAQUE
    val bitmap = Bitmap()
    val info = ImageInfo(width, height, ColorType.RGBA_8888, alphaType, ColorSpace.sRGB)
    check(bitmap.allocPixels(info)) { "Unable to allocate a ${width}x$height bitmap." }
    check(bitmap.installPixels(bytes)) { "Unable to install pixels into a ${width}x$height bitmap." }
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}

/** Returns true if any pixel in the RGBA [bytes] isn't fully opaque. */
private fun hasTranslucentPixel(bytes: ByteArray): Boolean {
    for (i in 3 until bytes.size step 4) {
        if (bytes[i] != 0xFF.toByte()) return true
    }
    return false
}
