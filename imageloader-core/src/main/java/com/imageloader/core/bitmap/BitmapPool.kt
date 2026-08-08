package com.imageloader.core.bitmap

import android.graphics.Bitmap

/**
 * Reusable mutable [Bitmap] store for decode / transform paths (OOM mitigation).
 *
 * Leaf port — no dependency on caches or Engine.
 */
interface BitmapPool {
    /** Current retained size in bytes. */
    val currentSize: Long

    /** Max retained size in bytes. */
    val maxSize: Long

    /**
     * Obtain a mutable bitmap of at least [width] x [height] with [config].
     * Implementations may reuse a pooled bitmap or allocate a new one.
     */
    fun get(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap

    /**
     * Offer [bitmap] back to the pool. No-op (or recycle) if not poolable /
     * pool is full after eviction attempts.
     */
    fun put(bitmap: Bitmap)

    /** Drop all pooled bitmaps. */
    fun clear()

    /** Trim toward [maxSizeBytes] under memory pressure. */
    fun trimToSize(maxSizeBytes: Long)
}
