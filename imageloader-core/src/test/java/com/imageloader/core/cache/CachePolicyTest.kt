package com.imageloader.core.cache

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {

    @Test
    fun enabled_readsAndWrites() {
        assertTrue(CachePolicy.ENABLED.readEnabled)
        assertTrue(CachePolicy.ENABLED.writeEnabled)
    }

    @Test
    fun disabled_neither() {
        assertFalse(CachePolicy.DISABLED.readEnabled)
        assertFalse(CachePolicy.DISABLED.writeEnabled)
    }

    @Test
    fun readOnly_and_writeOnly() {
        assertTrue(CachePolicy.READ_ONLY.readEnabled)
        assertFalse(CachePolicy.READ_ONLY.writeEnabled)
        assertFalse(CachePolicy.WRITE_ONLY.readEnabled)
        assertTrue(CachePolicy.WRITE_ONLY.writeEnabled)
    }
}
