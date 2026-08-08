package com.imageloader.core.intercept

import com.imageloader.core.request.ImageRequest
import com.imageloader.core.request.ImageResult
import com.imageloader.core.size.Size

/**
 * OkHttp-style middleware stage in the image pipeline.
 */
fun interface Interceptor {
    suspend fun intercept(chain: Chain): ImageResult

    interface Chain {
        val request: ImageRequest
        val size: Size
        suspend fun proceed(): ImageResult
        fun withRequest(request: ImageRequest): Chain
    }
}
