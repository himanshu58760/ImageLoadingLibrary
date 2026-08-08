package com.example.imageloadinglibarary

/**
 * Stable remote demo URLs (picsum seeds) for scroll / cancel stress tests.
 */
object DemoImages {
    const val COUNT = 80

    fun url(index: Int, size: Int = 300): String =
        "https://picsum.photos/seed/imageloader$index/$size/$size"

    fun detailUrl(): String = "https://picsum.photos/seed/detail-hero/1080/720"
}
