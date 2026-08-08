package com.imageloader.core.decode

import com.imageloader.core.source.ImageSource

/**
 * Decodes an [ImageSource] into a [DecodeResult].
 */
fun interface Decoder {
    suspend fun decode(): DecodeResult

    fun interface Factory {
        fun create(source: ImageSource, options: Options): Decoder?
    }
}
