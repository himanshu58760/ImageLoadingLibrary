package com.imageloader.views

import android.view.View
import android.view.ViewTreeObserver
import com.imageloader.core.request.SizeResolver
import com.imageloader.core.size.Size
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Resolves size from a [View]'s measured dimensions, waiting for layout if needed.
 */
class ViewSizeResolver(
    private val view: View,
) : SizeResolver {
    override suspend fun size(): Size = suspendCancellableCoroutine { continuation ->
        val width = view.width
        val height = view.height
        if (width > 0 && height > 0) {
            continuation.resume(Size.pixels(width, height))
            return@suspendCancellableCoroutine
        }

        val observer = view.viewTreeObserver
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val vto = view.viewTreeObserver
                if (vto.isAlive) {
                    vto.removeOnPreDrawListener(this)
                } else {
                    observer.removeOnPreDrawListener(this)
                }
                if (continuation.isActive) {
                    val w = view.width.coerceAtLeast(1)
                    val h = view.height.coerceAtLeast(1)
                    continuation.resume(Size.pixels(w, h))
                }
                return true
            }
        }
        observer.addOnPreDrawListener(listener)
        continuation.invokeOnCancellation {
            val vto = view.viewTreeObserver
            if (vto.isAlive) {
                vto.removeOnPreDrawListener(listener)
            }
        }
        // Ensure a layout pass happens.
        view.requestLayout()
    }
}
