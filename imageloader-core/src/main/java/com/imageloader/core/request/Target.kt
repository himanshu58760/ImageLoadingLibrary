package com.imageloader.core.request

import android.graphics.drawable.Drawable

/**
 * UI (or no-op) sink for a single request's lifecycle callbacks.
 */
interface Target {
    fun onStart(placeholder: Drawable?) = Unit
    fun onSuccess(result: ImageResult.Success)
    fun onError(result: ImageResult.Error)
}
