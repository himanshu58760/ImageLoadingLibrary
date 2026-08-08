package com.imageloader.core.intercept

import android.graphics.drawable.BitmapDrawable
import com.imageloader.core.ComponentRegistry
import com.imageloader.core.bitmap.BitmapPool
import com.imageloader.core.cache.DiskCache
import com.imageloader.core.decode.BitmapFactoryDecoder
import com.imageloader.core.decode.Options
import com.imageloader.core.dispatch.TaskDispatcher
import com.imageloader.core.key.CacheKeyer
import com.imageloader.core.request.ImageResult
import com.imageloader.core.source.ImageSource
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

/**
 * Terminal interceptor: disk read → fetch → disk write → decode → transforms.
 */
internal class EngineInterceptor(
    private val components: ComponentRegistry,
    private val diskCache: DiskCache?,
    private val bitmapPool: BitmapPool,
    private val dispatcher: TaskDispatcher,
    private val keyer: CacheKeyer,
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val size = chain.size
        val diskKey = keyer.diskKey(request)
        val decodeOptions = Options(
            size = size,
            precision = request.precision,
            bitmapPool = bitmapPool,
        )

        // Disk hit → decode encoded bytes.
        if (diskCache != null && request.diskCachePolicy.readEnabled) {
            diskCache.openSnapshot(diskKey)?.use { snapshot ->
                coroutineContext.ensureActive()
                val source = ImageSource.FileSource(snapshot.data, deleteOnClose = false)
                return decodeSource(source, request, decodeOptions, diskKey)
            }
        }

        // Network / file fetch.
        val fetcher = components.fetcherFactories.firstNotNullOfOrNull { factory ->
            factory.create(request.data, decodeOptions)
        } ?: throw IOException("No Fetcher for data: ${request.data}")

        val fetchResult = withContext(dispatcher.fetch) {
            fetcher.fetch()
        }
        coroutineContext.ensureActive()

        try {
            // Persist encoded bytes before interruptible decode.
            if (diskCache != null && request.diskCachePolicy.writeEnabled) {
                writeToDisk(diskCache, diskKey, fetchResult.source)
            }
            coroutineContext.ensureActive()
            return decodeSource(fetchResult.source, request, decodeOptions, diskKey)
        } finally {
            fetchResult.source.close()
        }
    }

    private suspend fun decodeSource(
        source: ImageSource,
        request: com.imageloader.core.request.ImageRequest,
        options: Options,
        diskKey: com.imageloader.core.cache.CacheKey,
    ): ImageResult.Success {
        val decoder = components.decoderFactories.firstNotNullOfOrNull { factory ->
            factory.create(source, options)
        } ?: BitmapFactoryDecoder(source, options)

        var decodeResult = withContext(dispatcher.decode) {
            decoder.decode()
        }
        coroutineContext.ensureActive()

        var bitmap = decodeResult.bitmap
        for (transformation in request.transformations) {
            val transformed = transformation.transform(bitmap)
            if (transformed !== bitmap) {
                bitmapPool.put(bitmap)
                bitmap = transformed
            }
        }

        val drawable = BitmapDrawable(request.context.resources, bitmap)
        return ImageResult.Success(
            drawable = drawable,
            diskCacheKey = diskKey,
            isSampled = decodeResult.isSampled,
        )
    }

    private fun writeToDisk(diskCache: DiskCache, key: com.imageloader.core.cache.CacheKey, source: ImageSource) {
        val editor = diskCache.openEditor(key) ?: return
        try {
            source.openStream().use { input ->
                editor.data.outputStream().use { output -> input.copyTo(output) }
            }
            editor.commit()
        } catch (_: Exception) {
            editor.abort()
        }
    }
}

private inline fun <T> DiskCache.Snapshot.use(block: (DiskCache.Snapshot) -> T): T {
    try {
        return block(this)
    } finally {
        close()
    }
}
