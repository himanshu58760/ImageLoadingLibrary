package com.imageloader.core

import android.content.Context
import com.imageloader.core.bitmap.BitmapPool
import com.imageloader.core.bitmap.LruBitmapPool
import com.imageloader.core.cache.DiskCache
import com.imageloader.core.cache.MemoryCache
import com.imageloader.core.dispatch.IoTaskDispatcher
import com.imageloader.core.dispatch.TaskDispatcher
import com.imageloader.core.dispatch.defaultMaxParallelism
import com.imageloader.core.request.Disposable
import com.imageloader.core.request.ImageRequest
import com.imageloader.core.request.ImageResult
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.File

/**
 * Executes [ImageRequest]s — fetch, decode, cache, and deliver results.
 *
 * Concurrency substrate is kotlinx-coroutines only.
 */
interface ImageLoader {
    fun enqueue(request: ImageRequest): Disposable

    suspend fun execute(request: ImageRequest): ImageResult

    /**
     * Warm memory/disk for later UI loads. Uses an internal no-op Target.
     * Optional [onFinished] is invoked once on the main thread (not on cancel).
     */
    fun preload(
        request: ImageRequest,
        onFinished: ((ImageResult) -> Unit)? = null,
    ): Disposable

    fun preload(
        data: Any,
        onFinished: ((ImageResult) -> Unit)? = null,
        builder: ImageRequest.Builder.() -> Unit = {},
    ): Disposable

    /** Suspend helper — same pipeline as [preload]; coroutine cancel cancels work. */
    suspend fun awaitPreload(request: ImageRequest): ImageResult

    suspend fun awaitPreload(
        data: Any,
        builder: ImageRequest.Builder.() -> Unit = {},
    ): ImageResult

    fun shutdown()

    fun newBuilder(): Builder

    class Builder(context: Context) {
        private val applicationContext: Context = context.applicationContext
        private var memoryCacheLazy: (() -> MemoryCache?)? = null
        private var diskCacheLazy: (() -> DiskCache?)? = null
        private var bitmapPoolLazy: (() -> BitmapPool)? = null
        private var taskDispatcher: TaskDispatcher? = null
        private var callFactory: Call.Factory? = null
        private var componentRegistry: ComponentRegistry? = null

        internal constructor(imageLoader: RealImageLoader) : this(imageLoader.applicationContext) {
            memoryCacheLazy = { imageLoader.memoryCache }
            diskCacheLazy = { imageLoader.diskCache }
            bitmapPoolLazy = { imageLoader.bitmapPool }
            taskDispatcher = imageLoader.taskDispatcher
            callFactory = imageLoader.callFactory
            componentRegistry = imageLoader.components
        }

        fun memoryCache(initializer: () -> MemoryCache?) = apply {
            memoryCacheLazy = initializer
        }

        fun memoryCache(cache: MemoryCache?) = memoryCache { cache }

        fun diskCache(initializer: () -> DiskCache?) = apply {
            diskCacheLazy = initializer
        }

        fun diskCache(cache: DiskCache?) = diskCache { cache }

        fun bitmapPool(pool: BitmapPool) = apply {
            bitmapPoolLazy = { pool }
        }

        fun taskDispatcher(dispatcher: TaskDispatcher) = apply {
            taskDispatcher = dispatcher
        }

        fun maxParallelism(n: Int) = taskDispatcher(IoTaskDispatcher(n))

        fun okHttpClient(client: Call.Factory) = apply {
            callFactory = client
        }

        fun components(registry: ComponentRegistry) = apply {
            componentRegistry = registry
        }

        fun components(builder: ComponentRegistry.Builder.() -> Unit) = apply {
            componentRegistry = (componentRegistry?.newBuilder() ?: ComponentRegistry.Builder())
                .apply(builder)
                .build()
        }

        fun build(): ImageLoader {
            val dispatcher = taskDispatcher ?: IoTaskDispatcher(defaultMaxParallelism())
            val pool = bitmapPoolLazy?.invoke()
                ?: LruBitmapPool(maxSize = Runtime.getRuntime().maxMemory() / 16)
            val memory = memoryCacheLazy?.invoke()
                ?: MemoryCache.Builder(applicationContext).build()
            val diskDir = File(applicationContext.cacheDir, "image_disk_cache")
            val disk = diskCacheLazy?.invoke()
                ?: DiskCache.Builder()
                    .directory(diskDir)
                    .build()
            val calls = callFactory ?: OkHttpClient()
            val fetchTmp = File(applicationContext.cacheDir, "image_fetch_tmp")
            val registry = componentRegistry
                ?: ComponentRegistry.default(calls, fetchTmp)

            return RealImageLoader(
                applicationContext = applicationContext,
                memoryCache = memory,
                diskCache = disk,
                bitmapPool = pool,
                taskDispatcher = dispatcher,
                callFactory = calls,
                components = registry,
            )
        }
    }
}
