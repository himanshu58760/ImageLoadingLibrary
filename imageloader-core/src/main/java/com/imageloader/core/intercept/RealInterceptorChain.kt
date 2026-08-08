package com.imageloader.core.intercept

import com.imageloader.core.request.ImageRequest
import com.imageloader.core.request.ImageResult
import com.imageloader.core.size.Size

internal class RealInterceptorChain(
    private val interceptors: List<Interceptor>,
    private val index: Int,
    override val request: ImageRequest,
    override val size: Size,
) : Interceptor.Chain {

    override suspend fun proceed(): ImageResult {
        check(index < interceptors.size) { "Interceptor chain exhausted" }
        val next = copy(index = index + 1)
        return interceptors[index].intercept(next)
    }

    override fun withRequest(request: ImageRequest): Interceptor.Chain =
        copy(request = request)

    private fun copy(
        index: Int = this.index,
        request: ImageRequest = this.request,
        size: Size = this.size,
    ) = RealInterceptorChain(interceptors, index, request, size)
}
