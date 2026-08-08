package com.imageloader.views

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.imageloader.core.request.ImageResult
import com.imageloader.core.request.Target

/**
 * [Target] that draws into an [ImageView].
 */
class ImageViewTarget(
    val view: ImageView,
) : Target {
    override fun onStart(placeholder: Drawable?) {
        if (placeholder != null) {
            view.setImageDrawable(placeholder)
        }
    }

    override fun onSuccess(result: ImageResult.Success) {
        view.setImageDrawable(result.drawable)
    }

    override fun onError(result: ImageResult.Error) {
        result.drawable?.let { view.setImageDrawable(it) }
    }
}
