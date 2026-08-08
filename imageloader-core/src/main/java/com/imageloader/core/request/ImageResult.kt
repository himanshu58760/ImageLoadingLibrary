package com.imageloader.core.request

/**
 * Terminal outcome of a load / preload / execute call.
 *
 * Cancellation is **not** modeled here — [CancellationException] propagates.
 * Leaf sealed type for call-site exhaustiveness; drawable payload is filled by later layers.
 */
sealed interface ImageResult {
    data class Success(
        val drawable: android.graphics.drawable.Drawable,
        val memoryCacheKey: com.imageloader.core.cache.CacheKey? = null,
        val diskCacheKey: com.imageloader.core.cache.CacheKey? = null,
        val isSampled: Boolean = false,
    ) : ImageResult

    data class Error(
        val throwable: Throwable,
        val drawable: android.graphics.drawable.Drawable? = null,
    ) : ImageResult
}
