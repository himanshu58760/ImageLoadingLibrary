package com.imageloader.core.key

import com.imageloader.core.cache.CacheKey
import com.imageloader.core.request.ImageRequest
import com.imageloader.core.size.Size

/**
 * Builds stable cache keys for memory (size-aware) and disk (encoded bytes).
 */
class CacheKeyer {

    fun memoryKey(request: ImageRequest, size: Size): CacheKey {
        val transforms = request.transformations.joinToString(separator = ",") { it.key }
        return CacheKey(
            "mem:${request.data}|${size.width}x${size.height}|${request.precision}|$transforms",
        )
    }

    fun diskKey(request: ImageRequest): CacheKey =
        CacheKey("disk:${request.data}")
}
