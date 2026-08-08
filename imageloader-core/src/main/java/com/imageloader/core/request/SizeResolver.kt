package com.imageloader.core.request

import com.imageloader.core.size.Size

/**
 * Resolves the pixel size used for decode + memory cache keys.
 */
fun interface SizeResolver {
    suspend fun size(): Size
}

/** Fixed size known up front. */
class PreciseSizeResolver(
    private val size: Size,
) : SizeResolver {
    override suspend fun size(): Size = size
}
