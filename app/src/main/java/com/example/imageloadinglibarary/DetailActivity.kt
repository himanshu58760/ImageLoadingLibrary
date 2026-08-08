package com.example.imageloadinglibarary

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.imageloader.views.load

/**
 * Shows a detail image after [MainActivity] preload — should hit memory/disk cache.
 */
class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.detail_title)
        val imageView = ImageView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            adjustViewBounds = true
        }
        setContentView(imageView)
        imageView.load(DemoImages.detailUrl(), application.sampleImageLoader) {
            size(1080, 720)
        }
    }
}
