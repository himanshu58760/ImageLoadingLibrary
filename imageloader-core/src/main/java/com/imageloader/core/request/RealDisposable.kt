package com.imageloader.core.request

import kotlinx.coroutines.Job

internal class RealDisposable(
    private val job: Job,
) : Disposable {
    override val isDisposed: Boolean
        get() = !job.isActive

    override fun dispose() {
        job.cancel()
    }
}
