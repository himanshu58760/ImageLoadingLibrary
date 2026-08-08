package com.imageloader.core

import com.imageloader.core.decode.BitmapFactoryDecoder
import com.imageloader.core.decode.Decoder
import com.imageloader.core.fetch.Fetcher
import com.imageloader.core.fetch.FileFetcher
import com.imageloader.core.fetch.HttpUrlFetcher
import com.imageloader.core.intercept.Interceptor
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.File

/**
 * Ordered factories + custom interceptors for an [ImageLoader].
 */
class ComponentRegistry private constructor(
    val fetcherFactories: List<Fetcher.Factory>,
    val decoderFactories: List<Decoder.Factory>,
    val interceptors: List<Interceptor>,
) {
    fun newBuilder() = Builder(this)

    class Builder {
        private val fetcherFactories = mutableListOf<Fetcher.Factory>()
        private val decoderFactories = mutableListOf<Decoder.Factory>()
        private val interceptors = mutableListOf<Interceptor>()

        constructor()

        internal constructor(registry: ComponentRegistry) {
            fetcherFactories += registry.fetcherFactories
            decoderFactories += registry.decoderFactories
            interceptors += registry.interceptors
        }

        fun add(factory: Fetcher.Factory) = apply { fetcherFactories += factory }

        fun add(factory: Decoder.Factory) = apply { decoderFactories += factory }

        fun add(interceptor: Interceptor) = apply { interceptors += interceptor }

        fun build() = ComponentRegistry(
            fetcherFactories = fetcherFactories.toList(),
            decoderFactories = decoderFactories.toList(),
            interceptors = interceptors.toList(),
        )
    }

    companion object {
        fun default(
            callFactory: Call.Factory = OkHttpClient(),
            fetchCacheDirectory: File,
        ): ComponentRegistry = Builder()
            .add(HttpUrlFetcher.Factory(callFactory, fetchCacheDirectory))
            .add(FileFetcher.Factory())
            .add(BitmapFactoryDecoder.Factory())
            .build()
    }
}
