package com.imageloader.core.size

/**
 * How strictly decoded output must match the requested [Size].
 */
enum class Precision {
    /**
     * Prefer power-of-two subsample; output may be slightly larger/smaller
     * than requested. Best for lists / grids.
     */
    INEXACT,

    /** Subsample then scale so output matches requested size as closely as possible. */
    EXACT,
}
