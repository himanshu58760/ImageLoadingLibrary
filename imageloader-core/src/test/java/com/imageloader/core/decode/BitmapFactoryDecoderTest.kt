package com.imageloader.core.decode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.imageloader.core.bitmap.LruBitmapPool
import com.imageloader.core.size.Precision
import com.imageloader.core.size.Size
import com.imageloader.core.source.ImageSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BitmapFactoryDecoderTest {

    private fun pngBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.RED)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    @Test
    fun decode_samplesDownToRequestedSize() = runBlocking {
        val source = ImageSource.BytesSource(pngBytes(400, 400))
        val decoder = BitmapFactoryDecoder(
            source = source,
            options = Options(
                size = Size.pixels(100, 100),
                precision = Precision.INEXACT,
            ),
        )
        val result = decoder.decode()
        assertTrue(result.isSampled)
        assertTrue(result.bitmap.width <= 200)
        assertTrue(result.bitmap.height <= 200)
        result.bitmap.recycle()
    }

    @Test
    fun decode_original_keepsFullSize() = runBlocking {
        val source = ImageSource.BytesSource(pngBytes(64, 48))
        val decoder = BitmapFactoryDecoder(
            source = source,
            options = Options(size = Size.ORIGINAL),
        )
        val result = decoder.decode()
        assertEquals(64, result.bitmap.width)
        assertEquals(48, result.bitmap.height)
        assertEquals(false, result.isSampled)
        result.bitmap.recycle()
    }

    @Test
    fun decode_canReusePooledBitmap() = runBlocking {
        val pool = LruBitmapPool(maxSize = 5L * 1024 * 1024)
        // Warm pool with a matching buffer (sampled 400/4 = 100).
        pool.put(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        val before = pool.currentSize
        assertTrue(before > 0)

        val source = ImageSource.BytesSource(pngBytes(400, 400))
        val decoder = BitmapFactoryDecoder(
            source = source,
            options = Options(
                size = Size.pixels(100, 100),
                bitmapPool = pool,
            ),
        )
        val result = decoder.decode()
        assertTrue(result.bitmap.width > 0)
        // Pool should have been drained for inBitmap reuse (best-effort).
        assertTrue(pool.currentSize < before || result.bitmap.isMutable)
        result.bitmap.recycle()
        pool.clear()
    }
}
