package com.example.imageloadinglibarary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imageloadinglibarary.databinding.ActivityViewsGalleryBinding
import com.example.imageloadinglibarary.databinding.ItemImageBinding
import com.imageloader.core.ImageLoader
import com.imageloader.views.clear
import com.imageloader.views.load

/**
 * XML RecyclerView grid — rebind/clear cancels in-flight loads while flinging.
 */
class ViewsGalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityViewsGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.open_views_gallery)

        val loader = application.sampleImageLoader
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = ImageAdapter(loader, DemoImages.COUNT)
    }

    private class ImageAdapter(
        private val imageLoader: ImageLoader,
        private val count: Int,
    ) : RecyclerView.Adapter<ImageAdapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(DemoImages.url(position), imageLoader)
        }

        override fun onViewRecycled(holder: Holder) {
            holder.image.clear()
            super.onViewRecycled(holder)
        }

        override fun getItemCount(): Int = count

        class Holder(
            binding: ItemImageBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            val image: ImageView = binding.image

            fun bind(url: String, imageLoader: ImageLoader) {
                image.load(url, imageLoader)
            }
        }
    }
}
