package com.imageloader.views

import android.widget.ImageView
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.imageloader.core.ImageLoader
import com.imageloader.core.request.Disposable
import com.imageloader.core.request.ImageRequest

/**
 * Load [data] into this [ImageView]. Cancels any previous request attached to the view.
 */
fun ImageView.load(
    data: Any?,
    imageLoader: ImageLoader,
    builder: ImageRequest.Builder.() -> Unit = {},
): Disposable {
    clear()
    if (data == null) {
        return Disposed
    }

    val lifecycle = findViewTreeLifecycleOwner()?.lifecycle
    val request = ImageRequest.Builder(context)
        .data(data)
        .target(ImageViewTarget(this))
        .sizeResolver(ViewSizeResolver(this))
        .lifecycle(lifecycle)
        .apply(builder)
        .build()

    val disposable = imageLoader.enqueue(request)
    setTag(R.id.imageloader_request, disposable)
    return disposable
}

/**
 * Cancel the in-flight request for this view and clear the drawable.
 */
fun ImageView.clear() {
    val previous = getTag(R.id.imageloader_request) as? Disposable
    previous?.dispose()
    setTag(R.id.imageloader_request, null)
    setImageDrawable(null)
}

private object Disposed : Disposable {
    override val isDisposed: Boolean = true
    override fun dispose() = Unit
}
