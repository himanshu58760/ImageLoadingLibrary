package com.imageloader.core.bitmap

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Ref-counted bitmap wrapper (Glide-style).
 *
 * Multiple consumers (memory cache + Targets) can share one bitmap; when the
 * count reaches zero the bitmap is offered to [pool] if poolable.
 */
class SharedBitmap(
    val bitmap: Bitmap,
    private val pool: BitmapPool? = null,
    initialRefCount: Int = 1,
) {
    private val refs = AtomicInteger(initialRefCount)

    init {
        require(initialRefCount >= 1) { "initialRefCount must be >= 1" }
        require(!bitmap.isRecycled) { "bitmap is recycled" }
    }

    val refCount: Int
        get() = refs.get()

    /** Increment ref count; returns this for chaining. */
    fun acquire(): SharedBitmap {
        while (true) {
            val current = refs.get()
            check(current > 0) { "Cannot acquire a released SharedBitmap" }
            if (refs.compareAndSet(current, current + 1)) return this
        }
    }

    /**
     * Decrement ref count. At zero, offers [bitmap] to [pool] (or recycles if
     * there is no pool / not poolable).
     */
    fun release() {
        while (true) {
            val current = refs.get()
            check(current > 0) { "SharedBitmap already released" }
            if (!refs.compareAndSet(current, current - 1)) continue
            if (current == 1) {
                disposeBitmap()
            }
            return
        }
    }

    private fun disposeBitmap() {
        if (bitmap.isRecycled) return
        val targetPool = pool
        if (targetPool != null && bitmap.isPoolable()) {
            targetPool.put(bitmap)
        } else if (bitmap.isPoolable()) {
            bitmap.recycle()
        }
        // Immutable / HARDWARE: leave to GC.
    }
}
