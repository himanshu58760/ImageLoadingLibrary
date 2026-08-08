package com.example.imageloadinglibarary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imageloadinglibarary.databinding.ActivityMainBinding
import com.imageloader.core.request.ImageResult

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnViewsGallery.setOnClickListener {
            startActivity(Intent(this, ViewsGalleryActivity::class.java))
        }
        binding.btnComposeGallery.setOnClickListener {
            startActivity(Intent(this, ComposeGalleryActivity::class.java))
        }
        binding.btnPreloadDemo.setOnClickListener {
            val loader = application.sampleImageLoader
            Toast.makeText(this, R.string.preload_status, Toast.LENGTH_SHORT).show()
            loader.preload(
                data = DemoImages.detailUrl(),
                onFinished = { result ->
                    runOnUiThread {
                        when (result) {
                            is ImageResult.Success -> {
                                startActivity(Intent(this, DetailActivity::class.java))
                            }
                            is ImageResult.Error -> {
                                Toast.makeText(
                                    this,
                                    "Preload failed: ${result.throwable.message}",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    }
                },
            ) {
                size(1080, 720)
            }
        }
    }
}
