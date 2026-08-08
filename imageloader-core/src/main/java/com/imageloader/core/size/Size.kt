package com.imageloader.core.size

/**
 * Target pixel size for decoding and memory-cache keys.
 *
 * Leaf type — independent of pipeline / UI.
 */
data class Size(
    val width: Dimension,
    val height: Dimension,
) {
    companion object {
        /** Both axes original (full intrinsic size). */
        val ORIGINAL = Size(Dimension.Original, Dimension.Original)

        /** Both axes undefined (resolve from Target measurement). */
        val AUTOMATIC = Size(Dimension.Undefined, Dimension.Undefined)

        /** Convenience for fixed pixel dimensions. */
        fun pixels(width: Int, height: Int): Size =
            Size(Dimension.Pixels(width), Dimension.Pixels(height))
    }
}
