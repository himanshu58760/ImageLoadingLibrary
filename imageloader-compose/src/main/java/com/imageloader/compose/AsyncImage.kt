package com.imageloader.compose

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.imageloader.core.ImageLoader
import com.imageloader.core.request.ImageRequest
import com.imageloader.core.request.ImageResult
import com.imageloader.core.request.PreciseSizeResolver
import com.imageloader.core.request.Target
import com.imageloader.core.size.Size

/**
 * Compose image loader that cancels work when leaving the composition (LazyColumn-safe).
 */
@Composable
fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    error: Painter? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    builder: ImageRequest.Builder.() -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val requestSize = remember(constraints) { constraints.toImageSize() }

        var painter by remember(model, imageLoader) { mutableStateOf<Painter?>(null) }
        var showError by remember(model, imageLoader) { mutableStateOf(false) }
        val placeholderState = rememberUpdatedState(placeholder)
        val errorState = rememberUpdatedState(error)

        DisposableEffect(model, imageLoader, requestSize) {
            if (model == null) {
                painter = null
                showError = false
                return@DisposableEffect onDispose { }
            }

            showError = false
            painter = null

            val target = object : Target {
                override fun onStart(placeholder: Drawable?) {
                    painter = placeholder.toPainterOrNull() ?: placeholderState.value
                    showError = false
                }

                override fun onSuccess(result: ImageResult.Success) {
                    painter = result.drawable.toPainterOrNull()
                    showError = false
                }

                override fun onError(result: ImageResult.Error) {
                    painter = result.drawable.toPainterOrNull() ?: errorState.value
                    showError = painter == null
                }
            }

            val request = ImageRequest.Builder(context)
                .data(model)
                .target(target)
                .sizeResolver(PreciseSizeResolver(requestSize))
                .lifecycle(lifecycleOwner.lifecycle)
                .apply(builder)
                .build()

            val disposable = imageLoader.enqueue(request)
            onDispose { disposable.dispose() }
        }

        val display = painter ?: if (showError) error else placeholder
        if (display != null) {
            Image(
                painter = display,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                alignment = alignment,
                contentScale = contentScale,
            )
        }
    }
}

private fun Constraints.toImageSize(): Size {
    val width = if (hasBoundedWidth) maxWidth.coerceAtLeast(1) else 512
    val height = if (hasBoundedHeight) maxHeight.coerceAtLeast(1) else 512
    return Size.pixels(width, height)
}

private fun Drawable?.toPainterOrNull(): Painter? = when (this) {
    is BitmapDrawable -> bitmap?.takeUnless { it.isRecycled }?.let { BitmapPainter(it.asImageBitmap()) }
    is ColorDrawable -> ColorPainter(Color(color))
    else -> null
}
