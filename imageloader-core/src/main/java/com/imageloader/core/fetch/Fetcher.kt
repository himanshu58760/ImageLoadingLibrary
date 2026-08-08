package com.imageloader.core.fetch

import com.imageloader.core.decode.Options

/**
 * Loads encoded image bytes for a single data model (URL, File, …).
 */
fun interface Fetcher {
    suspend fun fetch(): FetchResult

    fun interface Factory {
        /** Return a fetcher for [data], or null if this factory does not handle it. */
        fun create(data: Any, options: Options): Fetcher?
    }
}
