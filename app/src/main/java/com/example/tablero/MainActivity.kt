package com.example.espdisplay

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.espdisplay.adapters.ImagesAdapter
import com.example.espdisplay.models.ESPConfig
import com.example.espdisplay.models.ImageItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var addImageFab: FloatingActionButton
    private lateinit var titleText: TextView
    private lateinit var adapter: ImagesAdapter
    private lateinit var espConfig: ESPConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        espConfig = ESPConfig(
            ipAddress = intent.getStringExtra("ESP_IP") ?: "192.168.4.1",
            port = intent.getIntExtra("ESP_PORT", 80)
        )

        initViews()
        setupRecyclerView()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.imagesRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        progressBar = findViewById(R.id.progressBar)
        addImageFab = findViewById(R.id.addImageFab)
        titleText = findViewById(R.id.titleText)

        addImageFab.setOnClickListener {
            openAddImage()
        }
    }

    private fun setupRecyclerView() {
        adapter = ImagesAdapter(
            espConfig = espConfig,
            onImageClick = { image ->
                // Sin opciones, solo ver
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }


    private fun openAddImage() {
        val intent = Intent(this, ImageSelectorActivity::class.java).apply {
            putExtra("ESP_IP", espConfig.ipAddress)
            putExtra("ESP_PORT", espConfig.port)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        addImageFab.isEnabled = !show
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        titleText.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
    }
}