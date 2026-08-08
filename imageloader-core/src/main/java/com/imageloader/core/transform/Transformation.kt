package com.imageloader.core.transform

import android.graphics.Bitmap

/**
 * Post-decode bitmap transform. [key] is included in the memory cache key.
 */
interface Transformation {
    val key: String
    fun transform(input: Bitmap): Bitmap
}
