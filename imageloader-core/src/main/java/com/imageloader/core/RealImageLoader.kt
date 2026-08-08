package com.imageloader.core

import android.content.Context
import com.imageloader.core.bitmap.BitmapPool
import com.imageloader.core.cache.DiskCache
import com.imageloader.core.cache.MemoryCache
import com.imageloader.core.dispatch.TaskDispatcher
import com.imageloader.core.engine.InFlightRegistry
import com.imageloader.core.intercept.EngineInterceptor
import com.imageloader.core.intercept.MemoryCacheInterceptor
import com.imageloader.core.intercept.RealInterceptorChain
import com.imageloader.core.key.CacheKeyer
import com.imageloader.core.request.Disposable
import com.imageloader.core.request.EmptyTarget
import com.imageloader.core.request.ImageRequest
import com.imageloader.core.request.ImageResult
import com.imageloader.core.request.RealDisposable
import com.imageloader.core.request.RequestDelegate
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.Call
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.coroutineContext

internal class RealImageLoader(
    val applicationContext: Context,
    val memoryCache: MemoryCache?,
    val diskCache: DiskCache?,
    val bitmapPool: BitmapPool,
    val taskDispatcher: TaskDispatcher,
    val callFactory: Call.Factory,
    val components: ComponentRegistry,
) : ImageLoader {

    private val keyer = CacheKeyer()
    private val inFlight = InFlightRegistry()
    private val scope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, _ -> /* logged by callers via ImageResult */ },
    )

    private val interceptors = buildList {
        addAll(components.interceptors)
        add(MemoryCacheInterceptor(memoryCache, keyer))
        add(
            EngineInterceptor(
                components = components,
                diskCache = diskCache,
                bitmapPool = bitmapPool,
                dispatcher = taskDispatcher,
                keyer = keyer,
            ),
        )
    }

    override fun enqueue(request: ImageRequest): Disposable {
        val job = scope.async {
            executeInternal(request)
        }
        return RealDisposable(job)
    }

    override suspend fun execute(request: ImageRequest): ImageResult =
        executeInternal(request)

    override fun preload(
        request: ImageRequest,
        onFinished: ((ImageResult) -> Unit)?,
    ): Disposable {
        val preloadRequest = request.newBuilder()
            .target(EmptyTarget)
            .listener(null)
            .build()
        val job = scope.async {
            val result = executeInternal(preloadRequest)
            if (onFinished != null) {
                withContext(Dispatchers.Main.immediate) {
                    onFinished(result)
                }
            }
            result
        }
        return RealDisposable(job)
    }

    override fun preload(
        data: Any,
        onFinished: ((ImageResult) -> Unit)?,
        builder: ImageRequest.Builder.() -> Unit,
    ): Disposable {
        val request = ImageRequest.Builder(applicationContext)
            .data(data)
            .apply(builder)
            .build()
        return preload(request, onFinished)
    }

    override suspend fun awaitPreload(request: ImageRequest): ImageResult {
        val preloadRequest = request.newBuilder()
            .target(EmptyTarget)
            .listener(null)
            .build()
        return executeInternal(preloadRequest)
    }

    override suspend fun awaitPreload(
        data: Any,
        builder: ImageRequest.Builder.() -> Unit,
    ): ImageResult {
        val request = ImageRequest.Builder(applicationContext)
            .data(data)
            .apply(builder)
            .build()
        return awaitPreload(request)
    }

    override fun shutdown() {
        scope.cancel()
        memoryCache?.clear()
        bitmapPool.clear()
    }

    override fun newBuilder(): ImageLoader.Builder = ImageLoader.Builder(this)

    private suspend fun executeInternal(request: ImageRequest): ImageResult {
        val job = currentCoroutineContext().job
        val delegate = RequestDelegate(request.lifecycle, job)
        delegate.start()
        try {
            delegate.awaitStarted()
            withContext(Dispatchers.Main.immediate) {
                request.target?.onStart(request.placeholder)
            }

            val size = request.sizeResolver.size()
            val memoryKey = keyer.memoryKey(request, size)

            val result = inFlight.joinOrRun(memoryKey) {
                RealInterceptorChain(
                    interceptors = interceptors,
                    index = 0,
                    request = request,
                    size = size,
                ).proceed()
            }

            withContext(Dispatchers.Main.immediate) {
                when (result) {
                    is ImageResult.Success -> request.target?.onSuccess(result)
                    is ImageResult.Error -> request.target?.onError(result)
                }
                request.listener?.onFinished(result)
            }
            return result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            val error = ImageResult.Error(t, request.error)
            withContext(Dispatchers.Main.immediate) {
                request.target?.onError(error)
                request.listener?.onFinished(error)
            }
            return error
        } finally {
            delegate.complete()
        }
    }
}
