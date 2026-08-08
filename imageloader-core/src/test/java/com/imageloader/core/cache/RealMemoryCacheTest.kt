package com.imageloader.core.cache

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import com.imageloader.core.bitmap.LruBitmapPool
import com.imageloader.core.bitmap.SharedBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RealMemoryCacheTest {

    private fun shared(width: Int = 32, height: Int = 32): SharedBitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return SharedBitmap(bitmap, pool = null)
    }

    @Test
    fun set_get_roundTrip_cacheOwnsRef() {
        val cache = MemoryCache.Builder(RuntimeEnvironment.getApplication())
            .maxSizeBytes(5L * 1024 * 1024)
            .build()
        val key = CacheKey("img-1")
        val image = shared()
        cache.set(key, MemoryCache.Value(image))
        image.release() // only cache holds a ref

        val hit = cache.get(key)
        assertNotNull(hit)
        assertTrue(hit!!.image.refCount >= 1)
        cache.clear()
    }

    @Test
    fun eviction_byByteBudget_releasesToPool() {
        val pool = LruBitmapPool(maxSize = 10L * 1024 * 1024)
        // 64*64*4 = 16_384 bytes each; budget fits one entry only.
        val cache = RealMemoryCache(maxSize = 20_000)
        val keyA = CacheKey("a")
        val keyB = CacheKey("b")
        val a = SharedBitmap(Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888), pool)
        val b = SharedBitmap(Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888), pool)

        cache.set(keyA, MemoryCache.Value(a))
        a.release() // only cache holds a
        cache.set(keyB, MemoryCache.Value(b))
        b.release()

        assertNull(cache.get(keyA))
        assertNotNull(cache.get(keyB))
        // Evicted A should have been offered to the pool.
        assertTrue(pool.currentSize > 0L)
        cache.clear()
        pool.clear()
    }

    @Test
    fun remove_and_clear() {
        val cache = RealMemoryCache(maxSize = 1024 * 1024)
        val key = CacheKey("x")
        val image = shared()
        cache.set(key, MemoryCache.Value(image))
        image.release()
        assertTrue(cache.remove(key))
        assertNull(cache.get(key))
        assertEquals(0L, cache.size)
    }

    @Test
    fun trimMemory_complete_clears() {
        val cache = RealMemoryCache(maxSize = 1024 * 1024)
        val image = shared()
        cache.set(CacheKey("t"), MemoryCache.Value(image))
        image.release()
        cache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        assertEquals(0L, cache.size)
    }
}
