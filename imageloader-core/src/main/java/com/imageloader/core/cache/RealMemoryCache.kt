package com.imageloader.core.cache

import android.content.ComponentCallbacks2
import java.util.LinkedHashMap

/**
 * Strong-ref byte-budget LRU [MemoryCache].
 *
 * Evicted entries [com.imageloader.core.bitmap.SharedBitmap.release] their cache-owned ref.
 */
internal class RealMemoryCache(
    override val maxSize: Long,
) : MemoryCache {

    init {
        require(maxSize >= 0L) { "maxSize must be >= 0" }
    }

    private val lock = Any()
    private var currentSize = 0L

    private val map = object : LinkedHashMap<CacheKey, MemoryCache.Value>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, MemoryCache.Value>?): Boolean {
            // Eviction handled explicitly in set/trim so we can release refs.
            return false
        }
    }

    override val size: Long
        get() = synchronized(lock) { currentSize }

    override fun get(key: CacheKey): MemoryCache.Value? = synchronized(lock) {
        val value = map[key] ?: return null
        // Caller receives an acquired ref; cache keeps its own.
        value.image.acquire()
        value
    }

    override fun set(key: CacheKey, value: MemoryCache.Value) = synchronized(lock) {
        val previous = map.put(key, value)
        // Cache holds one ref for the stored entry.
        value.image.acquire()
        currentSize += value.sizeBytes
        if (previous != null) {
            currentSize -= previous.sizeBytes
            previous.image.release()
        }
        evictTo(maxSize)
    }

    override fun remove(key: CacheKey): Boolean = synchronized(lock) {
        val removed = map.remove(key) ?: return false
        currentSize -= removed.sizeBytes
        removed.image.release()
        true
    }

    override fun clear() = synchronized(lock) {
        map.values.forEach { it.image.release() }
        map.clear()
        currentSize = 0L
    }

    @Suppress("DEPRECATION")
    override fun trimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> clear()
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ->
                synchronized(lock) { evictTo(maxSize / 2) }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE ->
                synchronized(lock) { evictTo((maxSize * 0.75).toLong()) }
        }
    }

    private fun evictTo(target: Long) {
        val iterator = map.entries.iterator()
        while (currentSize > target && iterator.hasNext()) {
            val entry = iterator.next()
            iterator.remove()
            currentSize -= entry.value.sizeBytes
            entry.value.image.release()
        }
        if (currentSize < 0L) currentSize = 0L
    }
}
