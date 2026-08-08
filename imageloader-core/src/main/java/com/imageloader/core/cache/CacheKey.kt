package com.imageloader.core.cache

/**
 * Stable identity for a cached entry (memory or disk).
 *
 * Leaf value type — equality is by [value] only.
 */
@JvmInline
value class CacheKey(val value: String) {
    init {
        require(value.isNotBlank()) { "CacheKey value must not be blank" }
    }

    override fun toString(): String = value
}
