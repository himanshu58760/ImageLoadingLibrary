package com.imageloader.core.decode

import com.imageloader.core.size.Dimension
import com.imageloader.core.size.Precision
import com.imageloader.core.size.Size
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Subsample / scale helpers for size-aware decoding.
 */
object DecodeUtils {

    /**
     * Power-of-two [android.graphics.BitmapFactory.Options.inSampleSize] that keeps
     * the decoded image at least as large as [dstSize] when possible.
     *
     * Returns 1 when [dstSize] is [Size.ORIGINAL] / undefined or source is smaller.
     */
    fun computeInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        dstSize: Size,
        precision: Precision = Precision.INEXACT,
    ): Int {
        require(srcWidth > 0 && srcHeight > 0) { "src dimensions must be > 0" }
        val (dstWidth, dstHeight) = resolveDestinationPixels(srcWidth, srcHeight, dstSize)
        if (dstWidth <= 0 || dstHeight <= 0) return 1
        if (srcWidth <= dstWidth && srcHeight <= dstHeight) return 1

        var inSampleSize = 1
        when (precision) {
            Precision.INEXACT -> {
                // Largest power-of-two such that decoded size >= destination on both axes.
                val widthRatio = srcWidth / dstWidth
                val heightRatio = srcHeight / dstHeight
                inSampleSize = max(1, min(widthRatio, heightRatio)).takeHighestOneBit()
                if (inSampleSize < 1) inSampleSize = 1
            }
            Precision.EXACT -> {
                // Same subsample first; exact scale applied after decode by caller if needed.
                val widthRatio = srcWidth.toFloat() / dstWidth
                val heightRatio = srcHeight.toFloat() / dstHeight
                val ratio = min(widthRatio, heightRatio)
                inSampleSize = max(1, ratio.toInt()).takeHighestOneBit()
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Resolve destination pixel size. Undefined/Original axes fall back to source size
     * (no downsampling on that axis).
     */
    fun resolveDestinationPixels(
        srcWidth: Int,
        srcHeight: Int,
        dstSize: Size,
    ): Pair<Int, Int> {
        val width = when (val w = dstSize.width) {
            is Dimension.Pixels -> w.px
            Dimension.Original, Dimension.Undefined -> srcWidth
        }
        val height = when (val h = dstSize.height) {
            is Dimension.Pixels -> h.px
            Dimension.Original, Dimension.Undefined -> srcHeight
        }
        return width to height
    }

    /** Even dimensions preferred for some codecs / inBitmap reuse. */
    fun even(value: Int): Int = value and 1.inv()

    fun sampledWidth(srcWidth: Int, inSampleSize: Int): Int =
        (srcWidth.toFloat() / inSampleSize).roundToInt().coerceAtLeast(1)

    fun sampledHeight(srcHeight: Int, inSampleSize: Int): Int =
        (srcHeight.toFloat() / inSampleSize).roundToInt().coerceAtLeast(1)
}
