package com.imageloader.core.bitmap

import android.graphics.Bitmap
import android.os.Build

internal fun Bitmap.Config?.bytesPerPixel(): Int = when (this ?: Bitmap.Config.ARGB_8888) {
    Bitmap.Config.ALPHA_8 -> 1
    Bitmap.Config.RGB_565 -> 2
    Bitmap.Config.ARGB_4444 -> 2
    Bitmap.Config.RGBA_F16 -> 8
    Bitmap.Config.HARDWARE -> 0 // not pooled; size via allocationByteCount when needed
    else -> 4
}

internal fun allocationByteCountOf(width: Int, height: Int, config: Bitmap.Config?): Long {
    val bpp = config.bytesPerPixel()
    if (bpp == 0) return 0L
    return width.toLong() * height.toLong() * bpp
}

internal fun Bitmap.isPoolable(): Boolean {
    if (isRecycled) return false
    if (!isMutable) return false
    if (Build.VERSION.SDK_INT >= 26 && config == Bitmap.Config.HARDWARE) return false
    return true
}

internal fun Bitmap.safeByteCount(): Long {
    if (isRecycled) return 0L
    return try {
        allocationByteCount.toLong()
    } catch (_: Exception) {
        allocationByteCountOf(width, height, config)
    }
}
