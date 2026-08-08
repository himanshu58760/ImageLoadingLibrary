package com.imageloader.core.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.imageloader.core.source.ImageSource
import kotlinx.coroutines.runInterruptible
import java.io.IOException

/**
 * [BitmapFactory]-based decoder with size-aware [inSampleSize] and optional [inBitmap] reuse.
 *
 * Runs under [runInterruptible] so Job cancellation can interrupt blocking decode
 * (important for fast list scrolling).
 */
class BitmapFactoryDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult = runInterruptible {
        source.use { imageSource ->
            decodeBlocking(imageSource)
        }
    }

    private fun decodeBlocking(imageSource: ImageSource): DecodeResult {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        imageSource.openStream().use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val srcWidth = bounds.outWidth
        val srcHeight = bounds.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) {
            throw IOException("Unsupported or corrupt image")
        }

        val inSampleSize = DecodeUtils.computeInSampleSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstSize = options.size,
            precision = options.precision,
        )

        val sampledW = DecodeUtils.sampledWidth(srcWidth, inSampleSize)
        val sampledH = DecodeUtils.sampledHeight(srcHeight, inSampleSize)
        val config = normalizeConfig(options.config)

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = config
            inMutable = true
            // Prefer pooled reusable buffer when sizes match.
            val pooled = options.bitmapPool.get(sampledW, sampledH, config)
            if (pooled.width == sampledW && pooled.height == sampledH && pooled.config == config) {
                inBitmap = pooled
            } else {
                options.bitmapPool.put(pooled)
            }
        }

        val bitmap = try {
            imageSource.openStream().use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: IllegalArgumentException) {
            // inBitmap rejected — retry without reuse.
            decodeOptions.inBitmap?.let { options.bitmapPool.put(it) }
            decodeOptions.inBitmap = null
            imageSource.openStream().use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: throw e
        } ?: run {
            decodeOptions.inBitmap?.let { options.bitmapPool.put(it) }
            throw IOException("BitmapFactory returned null")
        }

        // Exact precision: scale down if still larger than destination.
        val (dstW, dstH) = DecodeUtils.resolveDestinationPixels(srcWidth, srcHeight, options.size)
        val needsExactScale = options.precision == com.imageloader.core.size.Precision.EXACT &&
            (bitmap.width > dstW || bitmap.height > dstH) &&
            dstW > 0 && dstH > 0

        val finalBitmap = if (needsExactScale) {
            val scaled = Bitmap.createScaledBitmap(bitmap, dstW, dstH, true)
            if (scaled !== bitmap) {
                options.bitmapPool.put(bitmap)
            }
            scaled
        } else {
            bitmap
        }

        return DecodeResult(
            bitmap = finalBitmap,
            isSampled = inSampleSize > 1 || needsExactScale,
        )
    }

    private fun normalizeConfig(config: Bitmap.Config): Bitmap.Config =
        if (config == Bitmap.Config.HARDWARE) Bitmap.Config.ARGB_8888 else config

    class Factory : Decoder.Factory {
        override fun create(source: ImageSource, options: Options): Decoder =
            BitmapFactoryDecoder(source, options)
    }
}
