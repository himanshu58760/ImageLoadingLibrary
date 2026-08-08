package com.imageloader.core.size

/**
 * A single axis size used when resolving decode targets.
 *
 * Leaf type — no dependencies on loaders, caches, or UI.
 */
sealed interface Dimension {
    /** Exact pixel size. */
    data class Pixels(val px: Int) : Dimension {
        init {
            require(px > 0) { "Pixels must be > 0, was $px" }
        }
    }

    /**
     * Size is not known yet (e.g. wait for View/Compose measurement)
     * or should follow the other axis / source.
     */
    data object Undefined : Dimension

    /** Decode using the image's intrinsic size on this axis. */
    data object Original : Dimension
}
