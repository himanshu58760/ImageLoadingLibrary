package com.imageloader.core.bitmap

import android.graphics.Bitmap
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicLong

/**
 * Byte-budget LRU [BitmapPool] keyed by width/height/config.
 *
 * HARDWARE and immutable bitmaps are never retained.
 */
class LruBitmapPool(
    override val maxSize: Long,
) : BitmapPool {

    init {
        require(maxSize >= 0L) { "maxSize must be >= 0" }
    }

    private val lock = Any()
    private val buckets = LinkedHashMap<PoolKey, LinkedList<Bitmap>>(32, 0.75f, true)
    private val sizeBytes = AtomicLong(0L)

    override val currentSize: Long
        get() = sizeBytes.get()

    override fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        require(width > 0 && height > 0) { "width/height must be > 0" }
        val normalized = normalizeConfig(config)
        val key = PoolKey(width, height, normalized)
        synchronized(lock) {
            val list = buckets[key]
            val reused = list?.pollLast()
            if (reused != null) {
                if (list.isEmpty()) buckets.remove(key)
                sizeBytes.addAndGet(-reused.safeByteCount())
                reused.eraseColor(0)
                return reused
            }
        }
        return Bitmap.createBitmap(width, height, normalized)
    }

    override fun put(bitmap: Bitmap) {
        if (!bitmap.isPoolable()) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }
        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        val key = PoolKey(bitmap.width, bitmap.height, normalizeConfig(config))
        val bytes = bitmap.safeByteCount()
        synchronized(lock) {
            if (bytes > maxSize) {
                bitmap.recycle()
                return
            }
            val list = buckets.getOrPut(key) { LinkedList() }
            list.addLast(bitmap)
            sizeBytes.addAndGet(bytes)
            evictTo(maxSize)
        }
    }

    override fun clear() {
        synchronized(lock) {
            buckets.values.forEach { list ->
                list.forEach { if (!it.isRecycled) it.recycle() }
                list.clear()
            }
            buckets.clear()
            sizeBytes.set(0L)
        }
    }

    override fun trimToSize(maxSizeBytes: Long) {
        synchronized(lock) {
            evictTo(maxSizeBytes.coerceAtLeast(0L))
        }
    }

    private fun evictTo(target: Long) {
        while (sizeBytes.get() > target && buckets.isNotEmpty()) {
            val iterator = buckets.entries.iterator()
            if (!iterator.hasNext()) break
            val entry = iterator.next()
            val bitmap = entry.value.pollFirst()
            if (bitmap == null) {
                iterator.remove()
                continue
            }
            sizeBytes.addAndGet(-bitmap.safeByteCount())
            if (!bitmap.isRecycled) bitmap.recycle()
            if (entry.value.isEmpty()) iterator.remove()
        }
    }

    private fun normalizeConfig(config: Bitmap.Config): Bitmap.Config =
        if (config == Bitmap.Config.HARDWARE) Bitmap.Config.ARGB_8888 else config

    private data class PoolKey(
        val width: Int,
        val height: Int,
        val config: Bitmap.Config,
    )
}
