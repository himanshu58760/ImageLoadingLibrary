package com.imageloader.core.fetch

import com.imageloader.core.decode.Options
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

class HttpUrlFetcherTest {

    @Test
    fun fetch_downloadsBytesToTempFile() = runBlocking {
        MockWebServer().use { server ->
            val payload = byteArrayOf(1, 2, 3, 4, 5)
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Buffer().write(payload))
                    .addHeader("Content-Type", "image/png"),
            )
            server.start()

            val dir = File.createTempFile("fetch-cache", null).apply {
                delete()
                mkdirs()
            }
            val fetcher = HttpUrlFetcher(
                url = server.url("/img.png").toString(),
                callFactory = OkHttpClient(),
                cacheDirectory = dir,
            )
            val result = fetcher.fetch()
            result.source.use { source ->
                val bytes = source.openStream().use { it.readBytes() }
                assertEquals(payload.toList(), bytes.toList())
            }
            dir.deleteRecursively()
            Unit
        }
    }

    @Test
    fun fetch_cancel_abortsCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(ByteArray(1024)))
                    .setBodyDelay(2, TimeUnit.SECONDS),
            )
            server.start()

            val dir = File.createTempFile("fetch-cancel", null).apply {
                delete()
                mkdirs()
            }
            val fetcher = HttpUrlFetcher(
                url = server.url("/slow.png").toString(),
                callFactory = OkHttpClient(),
                cacheDirectory = dir,
            )

            val job = async { fetcher.fetch() }
            // Give the call a moment to start, then cancel.
            kotlinx.coroutines.delay(100)
            job.cancel()
            val completed = runCatching { withTimeout(1_000) { job.await() } }
            assertTrue(completed.isFailure)
            dir.deleteRecursively()
            Unit
        }
    }

    @Test
    fun factory_handlesHttpStringsOnly() {
        val dir = File.createTempFile("fetch-factory", null).apply {
            delete()
            mkdirs()
        }
        val factory = HttpUrlFetcher.Factory(cacheDirectory = dir)
        assertTrue(factory.create("https://example.com/a.png", Options()) is HttpUrlFetcher)
        assertEquals(null, factory.create("/local/path.png", Options()))
        assertEquals(null, factory.create(File("/tmp/x"), Options()))
        dir.deleteRecursively()
    }
}
