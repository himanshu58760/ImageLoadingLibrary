package com.imageloader.core.dispatch

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Replaceable concurrency ports for fetch / decode stages.
 *
 * Leaf abstraction — Engine depends on this instead of [Dispatchers.IO] directly (DIP).
 */
interface TaskDispatcher {
    /** Dispatcher for network / source fetch work. */
    val fetch: CoroutineDispatcher

    /** Dispatcher for bitmap decode / transform work. */
    val decode: CoroutineDispatcher
}
