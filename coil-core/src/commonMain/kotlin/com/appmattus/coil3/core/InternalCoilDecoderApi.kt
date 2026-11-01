package com.appmattus.coil3.core

/** Marks APIs shared between the decoder modules. They are not meant for use outside this project. */
@RequiresOptIn(
    message = "This API is internal to the Coil decoder modules and may change without notice.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalCoilDecoderApi
