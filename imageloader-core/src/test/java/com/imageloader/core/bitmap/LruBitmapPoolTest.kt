package com.imageloader.core.bitmap

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LruBitmapPoolTest {

    @Test
    fun get_put_reusesSameBitmap() {
        val pool = LruBitmapPool(maxSize = 10L * 1024 * 1024)
        val first = pool.get(64, 64, Bitmap.Config.ARGB_8888)
        pool.put(first)
        assertTrue(pool.currentSize > 0)

        val second = pool.get(64, 64, Bitmap.Config.ARGB_8888)
        assertSame(first, second)
        assertEquals(0L, pool.currentSize)
    }

    @Test
    fun differentSize_doesNotReuse() {
        val pool = LruBitmapPool(maxSize = 10L * 1024 * 1024)
        val a = pool.get(32, 32, Bitmap.Config.ARGB_8888)
        pool.put(a)
        val b = pool.get(64, 64, Bitmap.Config.ARGB_8888)
        assertNotSame(a, b)
        b.recycle()
        pool.clear()
    }

    @Test
    fun trimToSize_evicts() {
        val pool = LruBitmapPool(maxSize = 10L * 1024 * 1024)
        repeat(4) {
            pool.put(pool.get(128, 128, Bitmap.Config.ARGB_8888))
        }
        assertTrue(pool.currentSize > 0)
        pool.trimToSize(0)
        assertEquals(0L, pool.currentSize)
    }

    @Test
    fun clear_emptiesPool() {
        val pool = LruBitmapPool(maxSize = 10L * 1024 * 1024)
        pool.put(pool.get(16, 16, Bitmap.Config.ARGB_8888))
        pool.clear()
        assertEquals(0L, pool.currentSize)
    }

    @Test
    fun immutableBitmap_isNotPooled() {
        val pool = LruBitmapPool(maxSize = 10L * 1024 * 1024)
        val immutable = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
            .apply { setHasAlpha(true) }
            .let { it.copy(Bitmap.Config.ARGB_8888, /* mutable = */ false) }
        assertFalse(immutable.isMutable)
        pool.put(immutable)
        assertEquals(0L, pool.currentSize)
    }
}
