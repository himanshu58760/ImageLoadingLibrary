package com.imageloader.core.fetch

import android.net.Uri
import com.imageloader.core.decode.Options
import com.imageloader.core.source.ImageSource
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Fetches HTTP(S) image bytes via OkHttp into a temp file (full body before decode).
 *
 * Cancellation calls [Call.cancel] cooperatively.
 */
class HttpUrlFetcher(
    private val url: String,
    private val callFactory: Call.Factory,
    private val cacheDirectory: File,
) : Fetcher {

    override suspend fun fetch(): FetchResult = suspendCancellableCoroutine { continuation ->
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) {
            continuation.resumeWithException(
                IOException("Unable to create fetch cache dir: $cacheDirectory"),
            )
            return@suspendCancellableCoroutine
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val call = callFactory.newCall(request)
        val completed = AtomicBoolean(false)

        continuation.invokeOnCancellation {
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!completed.compareAndSet(false, true)) return
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!completed.compareAndSet(false, true)) return
                    if (continuation.isCancelled) return
                    try {
                        if (!resp.isSuccessful) {
                            throw IOException("HTTP ${resp.code} for $url")
                        }
                        val body = resp.body ?: throw IOException("Empty body for $url")
                        val temp = File.createTempFile("img_fetch_", ".tmp", cacheDirectory)
                        try {
                            body.byteStream().use { input ->
                                temp.outputStream().use { output -> input.copyTo(output) }
                            }
                            val result = FetchResult(
                                source = ImageSource.FileSource(temp, deleteOnClose = true),
                                mimeType = body.contentType()?.toString(),
                                contentLength = body.contentLength(),
                            )
                            continuation.resume(result)
                        } catch (t: Throwable) {
                            temp.delete()
                            throw t
                        }
                    } catch (t: Throwable) {
                        if (continuation.isCancelled) return
                        continuation.resumeWithException(
                            t as? Exception ?: IOException(t.message, t),
                        )
                    }
                }
            }
        })
    }

    class Factory(
        private val callFactory: Call.Factory = OkHttpClient(),
        private val cacheDirectory: File,
    ) : Fetcher.Factory {
        override fun create(data: Any, options: Options): Fetcher? {
            val url = data.toHttpUrlOrNull() ?: return null
            return HttpUrlFetcher(url, callFactory, cacheDirectory)
        }
    }
}

internal fun Any.toHttpUrlOrNull(): String? = when (this) {
    is String -> if (looksLikeHttpUrl(this)) this else null
    is Uri -> if (scheme == "http" || scheme == "https") toString() else null
    else -> null
}

private fun looksLikeHttpUrl(value: String): Boolean {
    val lower = value.lowercase()
    return lower.startsWith("http://") || lower.startsWith("https://")
}
