package com.imageloader.core.fetch

import com.imageloader.core.source.ImageSource

/**
 * Result of a [Fetcher.fetch] call. Caller owns closing [source].
 */
data class FetchResult(
    val source: ImageSource,
    val mimeType: String? = null,
    val contentLength: Long = -1L,
)
