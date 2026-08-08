package com.imageloader.core.dispatch

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class IoTaskDispatcherTest {

    @Test
    fun defaultMaxParallelism_isClampedBetween4And8() {
        val value = defaultMaxParallelism()
        assertTrue(value in 4..8)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveParallelism() {
        IoTaskDispatcher(maxFetchParallelism = 0)
    }

    @Test
    fun customDispatcher_canBoundConcurrency() = runBlocking {
        val limiter = CountingDispatcher(maxConcurrent = 2)
        val dispatcher = object : TaskDispatcher {
            override val fetch: CoroutineDispatcher = limiter
            override val decode: CoroutineDispatcher = limiter
        }

        val jobs = (1..6).map {
            async {
                withContext(dispatcher.decode) {
                    Thread.sleep(30)
                }
            }
        }
        jobs.awaitAll()

        assertEquals(2, limiter.maxObserved.get())
    }

    /**
     * Minimal dispatcher that tracks peak in-flight tasks for parallelism tests.
     */
    private class CountingDispatcher(
        private val maxConcurrent: Int,
    ) : CoroutineDispatcher() {
        private val inFlight = AtomicInteger(0)
        val maxObserved = AtomicInteger(0)
        private val waitLock = Object()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            Thread {
                synchronized(waitLock) {
                    while (inFlight.get() >= maxConcurrent) {
                        waitLock.wait()
                    }
                    val now = inFlight.incrementAndGet()
                    maxObserved.updateAndGet { maxOf(it, now) }
                }
                try {
                    block.run()
                } finally {
                    synchronized(waitLock) {
                        inFlight.decrementAndGet()
                        waitLock.notifyAll()
                    }
                }
            }.start()
        }
    }
}
