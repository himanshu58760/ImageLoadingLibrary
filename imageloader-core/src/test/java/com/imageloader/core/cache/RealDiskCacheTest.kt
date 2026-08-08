package com.imageloader.core.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RealDiskCacheTest {

    private fun tempDir(prefix: String): File =
        File.createTempFile(prefix, null).apply {
            delete()
            mkdirs()
        }

    private fun newCache(maxSize: Long = 1024 * 1024): Pair<File, DiskCache> {
        val dir = tempDir("disk-cache-")
        val cache = DiskCache.Builder()
            .directory(dir)
            .maxSizeBytes(maxSize)
            .build()
        return dir to cache
    }

    @Test
    fun commit_then_snapshot_roundTrip() {
        val (dir, cache) = newCache()
        val key = CacheKey("https://example.com/a.png")
        val editor = cache.openEditor(key)
        assertNotNull(editor)
        editor!!.data.writeBytes(byteArrayOf(1, 2, 3, 4))
        editor.commit()

        val snapshot = cache.openSnapshot(key)
        assertNotNull(snapshot)
        assertEquals(listOf<Byte>(1, 2, 3, 4), snapshot!!.data.readBytes().toList())
        snapshot.close()
        assertTrue(cache.size >= 4)
        dir.deleteRecursively()
    }

    @Test
    fun abort_doesNotPublishEntry() {
        val (dir, cache) = newCache()
        val key = CacheKey("abort-me")
        val editor = cache.openEditor(key)!!
        editor.data.writeText("partial")
        editor.abort()

        assertNull(cache.openSnapshot(key))
        assertFalse(editor.data.exists())
        dir.deleteRecursively()
    }

    @Test
    fun cancelledDirtyFile_doesNotCorruptOnReopen() {
        val dir = tempDir("disk-cache-reopen-")
        val key = CacheKey("reopen")
        val cache1 = DiskCache.Builder().directory(dir).maxSizeBytes(1024 * 1024).build()
        val editor = cache1.openEditor(key)!!
        editor.data.writeText("dirty-only")
        // Simulate crash: dirty file left uncommitted + corrupt journal.
        File(dir, "journal").writeText("NOT_A_VALID_JOURNAL\n")

        val cache2 = DiskCache.Builder().directory(dir).maxSizeBytes(1024 * 1024).build()
        assertNull(cache2.openSnapshot(key))
        val leftoverDirty = dir.listFiles()
            ?.any { it.name.endsWith(".tmp") && it.name != "journal.tmp" }
            ?: false
        assertFalse(leftoverDirty)
        dir.deleteRecursively()
    }

    @Test
    fun eviction_respectsMaxSize() {
        val (dir, cache) = newCache(maxSize = 64)
        val payload = ByteArray(50) { 1 }

        fun put(name: String) {
            val editor = cache.openEditor(CacheKey(name))!!
            editor.data.writeBytes(payload)
            editor.commit()
        }

        put("one")
        put("two") // should evict "one"
        assertNull(cache.openSnapshot(CacheKey("one")))
        val two = cache.openSnapshot(CacheKey("two"))
        assertNotNull(two)
        two!!.close()
        assertTrue(cache.size <= 64)
        dir.deleteRecursively()
    }

    @Test
    fun remove_deletesEntry() {
        val (dir, cache) = newCache()
        val key = CacheKey("gone")
        cache.openEditor(key)!!.run {
            data.writeText("x")
            commit()
        }
        assertTrue(cache.remove(key))
        assertNull(cache.openSnapshot(key))
        dir.deleteRecursively()
    }
}
