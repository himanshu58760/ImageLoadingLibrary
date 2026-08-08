package com.imageloader.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import com.imageloader.core.cache.CachePolicy
import com.imageloader.core.cache.DiskCache
import com.imageloader.core.cache.MemoryCache
import com.imageloader.core.request.ImageRequest
import com.imageloader.core.request.ImageResult
import com.imageloader.core.request.Target
import com.imageloader.core.size.Size
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageLoaderTest {

    private fun pngBytes(width: Int = 64, height: Int = 64): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.BLUE)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun newLoader(server: MockWebServer? = null): ImageLoader {
        val context = RuntimeEnvironment.getApplication()
        val diskDir = File(context.cacheDir, "test_disk_${System.nanoTime()}").apply { mkdirs() }
        return ImageLoader.Builder(context)
            .okHttpClient(OkHttpClient())
            .memoryCache(MemoryCache.Builder(context).maxSizeBytes(8L * 1024 * 1024).build())
            .diskCache(DiskCache.Builder().directory(diskDir).maxSizeBytes(8L * 1024 * 1024).build())
            .maxParallelism(4)
            .build()
    }

    @Test
    fun execute_fetchesAndDecodes() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(pngBytes()))
                    .addHeader("Content-Type", "image/png"),
            )
            server.start()
            val loader = newLoader(server)
            val result = loader.execute(
                ImageRequest.Builder(RuntimeEnvironment.getApplication())
                    .data(server.url("/a.png").toString())
                    .size(Size.pixels(64, 64))
                    .build(),
            )
            assertTrue(result is ImageResult.Success)
            loader.shutdown()
            Unit
        }
    }

    @Test
    fun memoryCacheHit_onSecondExecute() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(pngBytes()))
                    .addHeader("Content-Type", "image/png"),
            )
            server.start()
            val loader = newLoader(server)
            val url = server.url("/cached.png").toString()
            val request = ImageRequest.Builder(RuntimeEnvironment.getApplication())
                .data(url)
                .size(Size.pixels(32, 32))
                .build()

            assertTrue(loader.execute(request) is ImageResult.Success)
            assertEquals(1, server.requestCount)

            assertTrue(loader.execute(request) is ImageResult.Success)
            assertEquals(1, server.requestCount) // memory hit — no second network
            loader.shutdown()
            Unit
        }
    }

    @Test
    fun preload_thenExecute_isMemoryHit() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(pngBytes(128, 128)))
                    .addHeader("Content-Type", "image/png"),
            )
            server.start()
            val loader = newLoader(server)
            val url = server.url("/warm.png").toString()

            val warm = loader.awaitPreload(url) {
                size(64, 64)
            }
            assertTrue(warm is ImageResult.Success)
            assertEquals(1, server.requestCount)

            val result = loader.execute(
                ImageRequest.Builder(RuntimeEnvironment.getApplication())
                    .data(url)
                    .size(64, 64)
                    .build(),
            )
            assertTrue(result is ImageResult.Success)
            assertEquals(1, server.requestCount)
            loader.shutdown()
            Unit
        }
    }

    @Test
    fun preload_onFinished_calledOnce() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(pngBytes()))
                    .addHeader("Content-Type", "image/png"),
            )
            server.start()
            val loader = newLoader(server)
            val latch = CountDownLatch(1)
            val finished = AtomicReference<ImageResult?>(null)
            val count = AtomicInteger(0)

            loader.preload(
                data = server.url("/cb.png").toString(),
                onFinished = {
                    count.incrementAndGet()
                    finished.set(it)
                    latch.countDown()
                },
            ) {
                size(32, 32)
            }

            // ImageLoader scope uses Main; idle Robolectric looper so work runs.
            val deadline = System.currentTimeMillis() + 5_000
            while (!latch.await(50, TimeUnit.MILLISECONDS) && System.currentTimeMillis() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
            }
            assertTrue(latch.count == 0L)
            assertEquals(1, count.get())
            assertNotNull(finished.get())
            assertTrue(finished.get() is ImageResult.Success)
            loader.shutdown()
        }
    }

    @Test
    fun enqueue_deliversToTarget() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(pngBytes()))
                    .addHeader("Content-Type", "image/png"),
            )
            server.start()
            val loader = newLoader(server)
            val latch = CountDownLatch(1)
            val target = object : Target {
                override fun onSuccess(result: ImageResult.Success) {
                    latch.countDown()
                }

                override fun onError(result: ImageResult.Error) {
                    latch.countDown()
                }
            }
            loader.enqueue(
                ImageRequest.Builder(RuntimeEnvironment.getApplication())
                    .data(server.url("/t.png").toString())
                    .size(32, 32)
                    .target(target)
                    .build(),
            )
            val deadline = System.currentTimeMillis() + 5_000
            while (!latch.await(50, TimeUnit.MILLISECONDS) && System.currentTimeMillis() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
            }
            assertTrue(latch.count == 0L)
            loader.shutdown()
        }
    }

    @Test
    fun diskOnlyPolicy_skipsMemoryWrite() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(pngBytes()))
                    .addHeader("Content-Type", "image/png"),
            )
            // Second response if memory miss forces network again without disk
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(pngBytes()))
                    .addHeader("Content-Type", "image/png"),
            )
            server.start()
            val loader = newLoader(server)
            val url = server.url("/disk.png").toString()
            val request = ImageRequest.Builder(RuntimeEnvironment.getApplication())
                .data(url)
                .size(32, 32)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()

            assertTrue(loader.execute(request) is ImageResult.Success)
            assertTrue(loader.execute(request) is ImageResult.Success)
            // Disk hit should avoid second network
            assertEquals(1, server.requestCount)
            loader.shutdown()
            Unit
        }
    }
}
