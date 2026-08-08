package com.imageloader.core.decode

import android.graphics.Bitmap
import com.imageloader.core.bitmap.BitmapPool
import com.imageloader.core.size.Precision
import com.imageloader.core.size.Size

/**
 * Decode / fetch options shared by leaf codecs.
 * Expanded later by Engine with cache keys, headers, etc.
 */
data class Options(
    val size: Size = Size.ORIGINAL,
    val precision: Precision = Precision.INEXACT,
    val config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    val bitmapPool: BitmapPool = EmptyBitmapPool,
)

/** Allocates fresh bitmaps; never retains them. */
object EmptyBitmapPool : BitmapPool {
    override val currentSize: Long = 0L
    override val maxSize: Long = 0L

    override fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap =
        Bitmap.createBitmap(width, height, config)

    override fun put(bitmap: Bitmap) = Unit

    override fun clear() = Unit

    override fun trimToSize(maxSizeBytes: Long) = Unit
}
