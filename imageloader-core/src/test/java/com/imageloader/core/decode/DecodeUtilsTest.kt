package com.imageloader.core.decode

import com.imageloader.core.size.Precision
import com.imageloader.core.size.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class DecodeUtilsTest {

    @Test
    fun originalSize_returnsSampleSize1() {
        val sample = DecodeUtils.computeInSampleSize(
            srcWidth = 4000,
            srcHeight = 3000,
            dstSize = Size.ORIGINAL,
        )
        assertEquals(1, sample)
    }

    @Test
    fun downsample_powerOfTwo() {
        val sample = DecodeUtils.computeInSampleSize(
            srcWidth = 4000,
            srcHeight = 3000,
            dstSize = Size.pixels(1000, 750),
            precision = Precision.INEXACT,
        )
        assertEquals(4, sample)
    }

    @Test
    fun smallerSource_returns1() {
        val sample = DecodeUtils.computeInSampleSize(
            srcWidth = 100,
            srcHeight = 100,
            dstSize = Size.pixels(400, 400),
        )
        assertEquals(1, sample)
    }

    @Test
    fun sampledDimensions() {
        assertEquals(1000, DecodeUtils.sampledWidth(4000, 4))
        assertEquals(750, DecodeUtils.sampledHeight(3000, 4))
    }
}
