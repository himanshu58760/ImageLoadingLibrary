package com.imageloader.core.dispatch

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Default [TaskDispatcher] that shares the process [Dispatchers.IO] pool via
 * [CoroutineDispatcher.limitedParallelism], capping concurrent image work.
 */
class IoTaskDispatcher(
    maxParallelism: Int = defaultMaxParallelism(),
    maxFetchParallelism: Int = maxParallelism,
    maxDecodeParallelism: Int = maxParallelism,
) : TaskDispatcher {

    init {
        require(maxFetchParallelism >= 1) { "maxFetchParallelism must be >= 1" }
        require(maxDecodeParallelism >= 1) { "maxDecodeParallelism must be >= 1" }
    }

    override val fetch: CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(maxFetchParallelism, "ImageLoader-Fetch")

    override val decode: CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(maxDecodeParallelism, "ImageLoader-Decode")
}

/**
 * Suitable default for mobile decode-heavy IO: `clamp(2 * CPUs, 4..8)`.
 */
fun defaultMaxParallelism(): Int =
    (2 * Runtime.getRuntime().availableProcessors()).coerceIn(4, 8)
