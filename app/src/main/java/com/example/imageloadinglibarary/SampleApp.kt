package com.example.imageloadinglibarary

import android.app.Application
import com.imageloader.core.ImageLoader
import com.imageloader.core.cache.DiskCache
import com.imageloader.core.cache.MemoryCache
import com.imageloader.core.dispatch.IoTaskDispatcher
import java.io.File

class SampleApp : Application() {
    lateinit var imageLoader: ImageLoader
        private set

    override fun onCreate() {
        super.onCreate()
        imageLoader = ImageLoader.Builder(this)
            .maxParallelism(6)
            .taskDispatcher(IoTaskDispatcher(maxParallelism = 6))
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "sample_image_disk"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}

val Application.sampleImageLoader: ImageLoader
    get() = (this as SampleApp).imageLoader
