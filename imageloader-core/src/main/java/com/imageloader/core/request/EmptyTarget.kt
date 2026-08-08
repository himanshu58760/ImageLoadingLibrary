package com.imageloader.core.request

/**
 * No-op [Target] used by [com.imageloader.core.ImageLoader.preload].
 * Not part of the public API surface for app code.
 */
internal object EmptyTarget : Target {
    override fun onSuccess(result: ImageResult.Success) = Unit
    override fun onError(result: ImageResult.Error) = Unit
}
