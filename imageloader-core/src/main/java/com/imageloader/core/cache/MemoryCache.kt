package com.imageloader.core.cache

import android.content.ComponentCallbacks2
import android.content.Context
import com.imageloader.core.bitmap.SharedBitmap

/**
 * In-memory L1 cache of decoded images ([SharedBitmap]).
 *
 * Leaf port relative to Engine — depends only on L1 keys and L2 bitmaps.
 */
interface MemoryCache {
    val size: Long
    val maxSize: Long

    fun get(key: CacheKey): Value?

    fun set(key: CacheKey, value: Value)

    fun remove(key: CacheKey): Boolean

    fun clear()

    /** Trim under system memory pressure ([ComponentCallbacks2] levels). */
    fun trimMemory(level: Int)

    /**
     * Cached decoded image. Callers that retain [image] beyond the cache hit
     * must [SharedBitmap.acquire]; the cache owns one ref while the entry lives.
     */
    class Value(
        val image: SharedBitmap,
        val isSampled: Boolean = false,
    ) {
        val sizeBytes: Long
            get() = image.bitmap.allocationByteCount.toLong().coerceAtLeast(1L)
    }

    class Builder(context: Context) {
        @Suppress("unused")
        private val applicationContext = context.applicationContext
        private var maxSizeBytes: Long? = null
        private var maxSizePercent: Double = DEFAULT_MAX_SIZE_PERCENT

        fun maxSizeBytes(size: Long) = apply {
            require(size >= 0) { "maxSizeBytes must be >= 0" }
            maxSizeBytes = size
        }

        /**
         * Fraction of the app heap used as the cache budget (0.0 exclusive .. 1.0].
         * Ignored if [maxSizeBytes] is set.
         */
        fun maxSizePercent(percent: Double) = apply {
            require(percent > 0.0 && percent <= 1.0) { "maxSizePercent must be in (0, 1]" }
            maxSizePercent = percent
        }

        fun build(): MemoryCache {
            val max = maxSizeBytes ?: (Runtime.getRuntime().maxMemory() * maxSizePercent).toLong()
            return RealMemoryCache(maxSize = max.coerceAtLeast(0L))
        }

        private companion object {
            const val DEFAULT_MAX_SIZE_PERCENT = 0.25
        }
    }
}
