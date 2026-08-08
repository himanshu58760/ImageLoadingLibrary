package com.imageloader.core.decode

import android.graphics.Bitmap

/**
 * Decoded bitmap payload. Caller owns the [bitmap] (or wraps it in SharedBitmap).
 */
data class DecodeResult(
    val bitmap: Bitmap,
    val isSampled: Boolean,
)
