package com.imageloader.core.source

import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Encoded image bytes available to a [com.imageloader.core.decode.Decoder].
 */
sealed interface ImageSource : Closeable {
    /** Open a fresh stream; caller must close it. */
    fun openStream(): InputStream

    /**
     * File-backed source. Prefer for [android.graphics.BitmapFactory] / ImageDecoder.
     *
     * @param deleteOnClose when true, deletes [file] in [close] (temp downloads).
     */
    class FileSource(
        val file: File,
        private val deleteOnClose: Boolean = false,
    ) : ImageSource {
        private var closed = false

        override fun openStream(): InputStream = FileInputStream(file)

        override fun close() {
            if (closed) return
            closed = true
            if (deleteOnClose) file.delete()
        }
    }

    /** In-memory bytes (small assets / tests). */
    class BytesSource(
        private val bytes: ByteArray,
    ) : ImageSource {
        override fun openStream(): InputStream = bytes.inputStream()

        override fun close() = Unit
    }
}
