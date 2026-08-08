package com.imageloader.core.bitmap

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SharedBitmapTest {

    @Test
    fun release_toZero_offersBitmapToPool() {
        val pool = LruBitmapPool(maxSize = 10L * 1024 * 1024)
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        val shared = SharedBitmap(bitmap, pool)

        shared.acquire()
        assertEquals(2, shared.refCount)
        shared.release()
        assertEquals(1, shared.refCount)
        assertEquals(0L, pool.currentSize)

        shared.release()
        assertEquals(0, shared.refCount)
        assertTrue(pool.currentSize > 0)

        val reused = pool.get(40, 40, Bitmap.Config.ARGB_8888)
        assertSame(bitmap, reused)
    }

    @Test(expected = IllegalStateException::class)
    fun acquire_afterFullyReleased_throws() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val shared = SharedBitmap(bitmap, pool = null)
        shared.release()
        shared.acquire()
    }

    @Test
    fun releaseWithoutPool_recyclesMutableBitmap() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val shared = SharedBitmap(bitmap, pool = null)
        shared.release()
        assertTrue(bitmap.isRecycled)
    }
}
