package com.imageloader.core.cache

/**
 * Per-request (or default) read/write policy for a cache tier.
 *
 * Leaf strategy enum — used later by memory/disk interceptors.
 */
enum class CachePolicy {
    /** Read and write. */
    ENABLED,

    /** Neither read nor write. */
    DISABLED,

    /** Read only; do not write new entries. */
    READ_ONLY,

    /** Write only; do not read existing entries. */
    WRITE_ONLY,
    ;

    val readEnabled: Boolean
        get() = this == ENABLED || this == READ_ONLY

    val writeEnabled: Boolean
        get() = this == ENABLED || this == WRITE_ONLY
}
