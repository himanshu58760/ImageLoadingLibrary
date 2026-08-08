package com.imageloader.core.engine

import com.imageloader.core.cache.CacheKey
import com.imageloader.core.request.ImageResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coalesces identical in-flight work so concurrent requests share one pipeline run.
 */
internal class InFlightRegistry {
    private val mutex = Mutex()
    private val jobs = mutableMapOf<CacheKey, CompletableDeferred<ImageResult>>()

    suspend fun joinOrRun(
        key: CacheKey,
        block: suspend () -> ImageResult,
    ): ImageResult {
        val (deferred, isOwner) = mutex.withLock {
            val existing = jobs[key]
            if (existing != null) {
                existing to false
            } else {
                val created = CompletableDeferred<ImageResult>()
                jobs[key] = created
                created to true
            }
        }

        if (!isOwner) {
            return deferred.await()
        }

        try {
            val result = block()
            deferred.complete(result)
            return result
        } catch (t: Throwable) {
            deferred.completeExceptionally(t)
            throw t
        } finally {
            mutex.withLock {
                if (jobs[key] === deferred) {
                    jobs.remove(key)
                }
            }
        }
    }
}
