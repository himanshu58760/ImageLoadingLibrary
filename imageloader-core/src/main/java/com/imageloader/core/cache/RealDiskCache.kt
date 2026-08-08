package com.imageloader.core.cache

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.security.MessageDigest
import java.util.LinkedHashMap

/**
 * Simple encoded-byte LRU disk cache with atomic commit (write `.tmp` then rename).
 *
 * Journal lines: `CLEAN keyHash size` / `REMOVE keyHash`. Corrupt journal falls back
 * to scanning the directory so a cancelled write cannot poison the cache forever.
 */
internal class RealDiskCache(
    override val directory: File,
    override val maxSize: Long,
) : DiskCache {

    init {
        require(maxSize >= 0L) { "maxSize must be >= 0" }
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create disk cache directory: $directory")
        }
        require(directory.isDirectory) { "directory must be a directory: $directory" }
    }

    private val lock = Any()
    private val journalFile = File(directory, JOURNAL_NAME)
    private val map = LinkedHashMap<String, Entry>(32, 0.75f, true)
    private var currentSize = 0L
    private var journalWriter: BufferedWriter? = null

    init {
        synchronized(lock) {
            readJournalOrRebuild()
            journalWriter = openJournalWriter(append = true)
            evictTo(maxSize)
        }
    }

    override val size: Long
        get() = synchronized(lock) { currentSize }

    override fun openSnapshot(key: CacheKey): DiskCache.Snapshot? = synchronized(lock) {
        val hash = keyHash(key)
        val entry = map[hash] ?: return null
        if (!entry.cleanFile.isFile) {
            map.remove(hash)
            return null
        }
        // Access-order touch
        map[hash] = entry
        SnapshotImpl(entry.cleanFile)
    }

    override fun openEditor(key: CacheKey): DiskCache.Editor? = synchronized(lock) {
        val hash = keyHash(key)
        val entry = map[hash] ?: Entry(hash).also { map[hash] = it }
        if (entry.editing) return null
        entry.editing = true
        entry.dirtyFile.parentFile?.mkdirs()
        if (entry.dirtyFile.exists()) entry.dirtyFile.delete()
        EditorImpl(entry)
    }

    override fun remove(key: CacheKey): Boolean = synchronized(lock) {
        removeHash(keyHash(key))
    }

    override fun clear() = synchronized(lock) {
        map.keys.toList().forEach { removeHash(it) }
        rewriteJournal()
    }

    private fun removeHash(hash: String): Boolean {
        val entry = map.remove(hash) ?: return false
        if (entry.editing) {
            entry.zombie = true
            return true
        }
        if (entry.cleanFile.exists()) {
            currentSize -= entry.sizeBytes
            entry.cleanFile.delete()
        }
        entry.dirtyFile.delete()
        appendJournal("REMOVE $hash")
        if (currentSize < 0L) currentSize = 0L
        return true
    }

    private fun commitEditor(entry: Entry) {
        synchronized(lock) {
            check(entry.editing) { "editor not active" }
            entry.editing = false
            if (entry.zombie) {
                entry.dirtyFile.delete()
                map.remove(entry.hash)
                return
            }
            val dirty = entry.dirtyFile
            if (!dirty.isFile) {
                dirty.delete()
                return
            }
            val newSize = dirty.length()
            if (entry.cleanFile.exists()) {
                currentSize -= entry.sizeBytes
                entry.cleanFile.delete()
            }
            if (!dirty.renameTo(entry.cleanFile)) {
                dirty.copyTo(entry.cleanFile, overwrite = true)
                dirty.delete()
            }
            entry.sizeBytes = entry.cleanFile.length()
            currentSize += entry.sizeBytes
            appendJournal("CLEAN ${entry.hash} ${entry.sizeBytes}")
            evictTo(maxSize)
        }
    }

    private fun abortEditor(entry: Entry) {
        synchronized(lock) {
            entry.editing = false
            entry.dirtyFile.delete()
            if (entry.zombie || !entry.cleanFile.exists()) {
                map.remove(entry.hash)
            }
        }
    }

    private fun evictTo(target: Long) {
        val iterator = map.entries.iterator()
        while (currentSize > target && iterator.hasNext()) {
            val entry = iterator.next().value
            if (entry.editing) continue
            iterator.remove()
            if (entry.cleanFile.exists()) {
                currentSize -= entry.sizeBytes
                entry.cleanFile.delete()
            }
            entry.dirtyFile.delete()
            appendJournal("REMOVE ${entry.hash}")
        }
        if (currentSize < 0L) currentSize = 0L
    }

    private fun readJournalOrRebuild() {
        if (!journalFile.exists()) {
            rebuildFromDirectory()
            rewriteJournal()
            return
        }
        try {
            journalFile.forEachLine(Charsets.UTF_8) { line ->
                if (line.isBlank()) return@forEachLine
                val parts = line.split(' ')
                when (parts[0]) {
                    "CLEAN" -> {
                        require(parts.size >= 3) { "bad CLEAN line: $line" }
                        val hash = parts[1]
                        val size = parts[2].toLongOrNull()
                            ?: throw IOException("bad CLEAN size: $line")
                        val entry = map.getOrPut(hash) { Entry(hash) }
                        entry.sizeBytes = size
                    }
                    "REMOVE" -> {
                        require(parts.size >= 2) { "bad REMOVE line: $line" }
                        map.remove(parts[1])
                    }
                    else -> throw IOException("unrecognized journal line: $line")
                }
            }
            // Drop entries whose files vanished; recompute size from disk.
            currentSize = 0L
            val missing = mutableListOf<String>()
            map.forEach { (hash, entry) ->
                if (entry.cleanFile.isFile) {
                    entry.sizeBytes = entry.cleanFile.length()
                    currentSize += entry.sizeBytes
                } else {
                    missing += hash
                }
            }
            missing.forEach { map.remove(it) }
            deleteOrphanDirtyFiles()
        } catch (_: Exception) {
            map.clear()
            currentSize = 0L
            rebuildFromDirectory()
            rewriteJournal()
        }
    }

    private fun deleteOrphanDirtyFiles() {
        directory.listFiles()?.forEach { file ->
            if (file.name.endsWith(DIRTY_SUFFIX) && file.name != JOURNAL_TMP_NAME) {
                file.delete()
            }
        }
    }

    private fun rebuildFromDirectory() {
        map.clear()
        currentSize = 0L
        directory.listFiles()?.forEach { file ->
            when {
                file.name == JOURNAL_NAME || file.name == JOURNAL_TMP_NAME -> Unit
                file.name.endsWith(DIRTY_SUFFIX) -> file.delete()
                file.isFile -> {
                    val hash = file.name
                    val entry = Entry(hash).also { it.sizeBytes = file.length() }
                    map[hash] = entry
                    currentSize += entry.sizeBytes
                }
            }
        }
    }

    private fun rewriteJournal() {
        val tmp = File(directory, JOURNAL_TMP_NAME)
        tmp.bufferedWriter(Charsets.UTF_8).use { writer ->
            map.values.forEach { entry ->
                if (entry.cleanFile.isFile) {
                    writer.appendLine("CLEAN ${entry.hash} ${entry.sizeBytes}")
                }
            }
        }
        if (journalFile.exists()) journalFile.delete()
        if (!tmp.renameTo(journalFile)) {
            tmp.copyTo(journalFile, overwrite = true)
            tmp.delete()
        }
        journalWriter?.close()
        journalWriter = openJournalWriter(append = true)
    }

    private fun openJournalWriter(append: Boolean): BufferedWriter =
        BufferedWriter(OutputStreamWriter(FileOutputStream(journalFile, append), Charsets.UTF_8))

    private fun appendJournal(line: String) {
        val writer = journalWriter ?: return
        writer.appendLine(line)
        writer.flush()
    }

    private fun keyHash(key: CacheKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }

    private inner class Entry(val hash: String) {
        val cleanFile: File = File(directory, hash)
        val dirtyFile: File = File(directory, hash + DIRTY_SUFFIX)
        var sizeBytes: Long = 0L
        var editing: Boolean = false
        var zombie: Boolean = false
    }

    private inner class SnapshotImpl(override val data: File) : DiskCache.Snapshot {
        override fun close() = Unit
    }

    private inner class EditorImpl(private val entry: Entry) : DiskCache.Editor {
        override val data: File = entry.dirtyFile

        override fun commit() = commitEditor(entry)

        override fun abort() = abortEditor(entry)
    }

    private companion object {
        const val JOURNAL_NAME = "journal"
        const val JOURNAL_TMP_NAME = "journal.tmp"
        const val DIRTY_SUFFIX = ".tmp"
    }
}
