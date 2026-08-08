package com.imageloader.core.intercept

import android.graphics.drawable.BitmapDrawable
import com.imageloader.core.bitmap.SharedBitmap
import com.imageloader.core.cache.MemoryCache
import com.imageloader.core.key.CacheKeyer
import com.imageloader.core.request.ImageResult
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal class MemoryCacheInterceptor(
    private val memoryCache: MemoryCache?,
    private val keyer: CacheKeyer,
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val cache = memoryCache
        val key = keyer.memoryKey(request, chain.size)

        if (cache != null && request.memoryCachePolicy.readEnabled) {
            val cached = cache.get(key)
            if (cached != null) {
                val drawable = BitmapDrawable(
                    request.context.resources,
                    cached.image.bitmap,
                )
                return ImageResult.Success(
                    drawable = drawable,
                    memoryCacheKey = key,
                    isSampled = cached.isSampled,
                )
            }
        }

        coroutineContext.ensureActive()
        val result = chain.proceed()

        if (result is ImageResult.Success &&
            cache != null &&
            request.memoryCachePolicy.writeEnabled
        ) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null && !bitmap.isRecycled) {
                val shared = SharedBitmap(bitmap)
                cache.set(key, MemoryCache.Value(shared, isSampled = result.isSampled))
                shared.release() // cache holds its own acquire
                return result.copy(memoryCacheKey = key)
            }
        }
        return result
    }
}
