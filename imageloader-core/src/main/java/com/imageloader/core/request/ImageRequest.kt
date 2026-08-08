package com.imageloader.core.request

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.Lifecycle
import com.imageloader.core.cache.CachePolicy
import com.imageloader.core.size.Precision
import com.imageloader.core.size.Size
import com.imageloader.core.transform.Transformation

/**
 * Immutable description of a single image load.
 */
class ImageRequest private constructor(
    val context: Context,
    val data: Any,
    val target: Target?,
    val sizeResolver: SizeResolver,
    val precision: Precision,
    val memoryCachePolicy: CachePolicy,
    val diskCachePolicy: CachePolicy,
    val placeholder: Drawable?,
    val error: Drawable?,
    val lifecycle: Lifecycle?,
    val transformations: List<Transformation>,
    val listener: Listener?,
) {
    fun newBuilder(): Builder = Builder(this)

    fun interface Listener {
        fun onFinished(result: ImageResult)
    }

    class Builder {
        private val context: Context
        private var data: Any? = null
        private var target: Target? = null
        private var sizeResolver: SizeResolver = PreciseSizeResolver(Size.ORIGINAL)
        private var precision: Precision = Precision.INEXACT
        private var memoryCachePolicy: CachePolicy = CachePolicy.ENABLED
        private var diskCachePolicy: CachePolicy = CachePolicy.ENABLED
        private var placeholder: Drawable? = null
        private var error: Drawable? = null
        private var lifecycle: Lifecycle? = null
        private var transformations: List<Transformation> = emptyList()
        private var listener: Listener? = null

        constructor(context: Context) {
            this.context = context.applicationContext
        }

        internal constructor(request: ImageRequest) {
            context = request.context
            data = request.data
            target = request.target
            sizeResolver = request.sizeResolver
            precision = request.precision
            memoryCachePolicy = request.memoryCachePolicy
            diskCachePolicy = request.diskCachePolicy
            placeholder = request.placeholder
            error = request.error
            lifecycle = request.lifecycle
            transformations = request.transformations
            listener = request.listener
        }

        fun data(data: Any?) = apply { this.data = data }

        fun target(target: Target?) = apply { this.target = target }

        fun size(size: Size) = apply { sizeResolver = PreciseSizeResolver(size) }

        fun size(width: Int, height: Int) = size(Size.pixels(width, height))

        fun sizeResolver(resolver: SizeResolver) = apply { sizeResolver = resolver }

        fun precision(precision: Precision) = apply { this.precision = precision }

        fun memoryCachePolicy(policy: CachePolicy) = apply { memoryCachePolicy = policy }

        fun diskCachePolicy(policy: CachePolicy) = apply { diskCachePolicy = policy }

        fun placeholder(drawable: Drawable?) = apply { placeholder = drawable }

        fun error(drawable: Drawable?) = apply { error = drawable }

        fun lifecycle(lifecycle: Lifecycle?) = apply { this.lifecycle = lifecycle }

        fun transformations(vararg transformations: Transformation) = apply {
            this.transformations = transformations.toList()
        }

        fun transformations(transformations: List<Transformation>) = apply {
            this.transformations = transformations.toList()
        }

        fun listener(listener: Listener?) = apply { this.listener = listener }

        fun build(): ImageRequest {
            val data = checkNotNull(data) { "data == null" }
            return ImageRequest(
                context = context,
                data = data,
                target = target,
                sizeResolver = sizeResolver,
                precision = precision,
                memoryCachePolicy = memoryCachePolicy,
                diskCachePolicy = diskCachePolicy,
                placeholder = placeholder,
                error = error,
                lifecycle = lifecycle,
                transformations = transformations,
                listener = listener,
            )
        }
    }
}
