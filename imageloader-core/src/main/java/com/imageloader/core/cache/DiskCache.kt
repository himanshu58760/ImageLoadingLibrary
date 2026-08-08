package com.imageloader.core.cache

import java.io.File

/**
 * On-disk L2 cache of **encoded** image bytes (JPEG/PNG/WebP), not bitmaps.
 *
 * Leaf port — Engine writes full bodies before interruptible decode.
 */
interface DiskCache {
    val directory: File
    val maxSize: Long
    val size: Long

    /** Open a read snapshot, or null on miss. Caller must [Snapshot.close]. */
    fun openSnapshot(key: CacheKey): Snapshot?

    /** Open an editor for [key], or null if editing is not possible. */
    fun openEditor(key: CacheKey): Editor?

    fun remove(key: CacheKey): Boolean

    fun clear()

    interface Snapshot {
        val data: File
        fun close()
    }

    interface Editor {
        /** Temp file to write encoded bytes into before [commit]. */
        val data: File
        fun commit()
        fun abort()
    }

    class Builder {
        private var directory: File? = null
        private var maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES

        fun directory(directory: File) = apply {
            this.directory = directory
        }

        fun maxSizeBytes(size: Long) = apply {
            require(size >= 0) { "maxSizeBytes must be >= 0" }
            maxSizeBytes = size
        }

        fun build(): DiskCache {
            val dir = checkNotNull(directory) { "directory == null" }
            return RealDiskCache(directory = dir, maxSize = maxSizeBytes)
        }

        private companion object {
            const val DEFAULT_MAX_SIZE_BYTES = 250L * 1024L * 1024L
        }
    }
}
