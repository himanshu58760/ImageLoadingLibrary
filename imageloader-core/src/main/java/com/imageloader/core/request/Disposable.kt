package com.imageloader.core.request

/**
 * Cancel handle for an in-flight request.
 *
 * Named like Coil’s Disposable; **not** RxJava’s `io.reactivex.disposables.Disposable`.
 * [dispose] cancels the underlying coroutine [Job].
 */
interface Disposable {
    val isDisposed: Boolean
    fun dispose()
}
