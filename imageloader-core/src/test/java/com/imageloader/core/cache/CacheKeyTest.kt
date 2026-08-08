package com.imageloader.core.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CacheKeyTest {

    @Test
    fun equality_byValue() {
        assertEquals(CacheKey("a"), CacheKey("a"))
        assertNotEquals(CacheKey("a"), CacheKey("b"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blank_rejected() {
        CacheKey("   ")
    }

    @Test
    fun toString_isValue() {
        assertEquals("http://img", CacheKey("http://img").toString())
    }
}
