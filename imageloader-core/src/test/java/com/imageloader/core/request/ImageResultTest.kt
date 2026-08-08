package com.imageloader.core.request

import com.imageloader.core.cache.CacheKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageResultTest {

    @Test
    fun sealed_branches_areExhaustive() {
        val error: ImageResult = ImageResult.Error(IllegalStateException("x"))
        val message = when (error) {
            is ImageResult.Success -> "success"
            is ImageResult.Error -> error.throwable.message
        }
        assertEquals("x", message)
        assertTrue(error is ImageResult.Error)
    }

    @Test
    fun error_canCarryOptionalDrawableNull() {
        val result = ImageResult.Error(RuntimeException("boom"), drawable = null)
        assertEquals(null, result.drawable)
        assertEquals("boom", result.throwable.message)
    }

    @Test
    fun cacheKey_wiresForSuccessMetadata() {
        val key = CacheKey("mem-1")
        assertEquals("mem-1", key.value)
    }
}
