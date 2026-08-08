package com.imageloader.core.fetch

import com.imageloader.core.decode.Options
import com.imageloader.core.source.ImageSource
import java.io.File

/**
 * Fetches from a local [File] or file-path [String].
 */
class FileFetcher(
    private val file: File,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        if (!file.isFile) throw java.io.FileNotFoundException("Not a file: $file")
        return FetchResult(
            source = ImageSource.FileSource(file, deleteOnClose = false),
            mimeType = null,
            contentLength = file.length(),
        )
    }

    class Factory : Fetcher.Factory {
        override fun create(data: Any, options: Options): Fetcher? {
            val file = when (data) {
                is File -> data
                is String -> {
                    if (data.startsWith("http://") || data.startsWith("https://")) return null
                    val f = File(data)
                    if (f.isFile) f else return null
                }
                else -> return null
            }
            return FileFetcher(file)
        }
    }
}
