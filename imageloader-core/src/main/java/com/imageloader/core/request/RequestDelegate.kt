package com.imageloader.core.request

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

/**
 * Binds a request [Job] to an optional [Lifecycle].
 */
internal interface RequestDelegate {
    fun start()
    suspend fun awaitStarted()
    fun complete()
}

internal object NoneRequestDelegate : RequestDelegate {
    override fun start() = Unit
    override suspend fun awaitStarted() = Unit
    override fun complete() = Unit
}

internal class LifecycleRequestDelegate(
    private val lifecycle: Lifecycle,
    private val job: Job,
) : RequestDelegate, DefaultLifecycleObserver {

    private val started = CompletableDeferred<Unit>()
    private var registered = false

    override fun start() {
        if (registered) return
        registered = true
        lifecycle.addObserver(this)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            started.complete(Unit)
        }
    }

    override suspend fun awaitStarted() {
        started.await()
    }

    override fun complete() {
        if (registered) {
            lifecycle.removeObserver(this)
            registered = false
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        started.complete(Unit)
    }

    override fun onStop(owner: LifecycleOwner) {
        // Pause: cancel in-flight work for this request.
        job.cancel()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        job.cancel()
        complete()
    }
}

internal fun RequestDelegate(
    lifecycle: Lifecycle?,
    job: Job,
): RequestDelegate =
    if (lifecycle != null) LifecycleRequestDelegate(lifecycle, job) else NoneRequestDelegate
