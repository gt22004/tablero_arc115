package com.example.espdisplay

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.espdisplay.models.ESPConfig
import com.example.espdisplay.utils.ImageProcessor
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class UploadActivity : AppCompatActivity() {

    private lateinit var uploadImagePreview: ImageView
    private lateinit var statusText: TextView
    private lateinit var uploadProgress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var doneButton: MaterialButton

    private lateinit var imageProcessor: ImageProcessor
    private var title: String = ""
    private var category: Int = 0
    private var subcategory: Int = 0
    private lateinit var espConfig: ESPConfig
    private var bitmap: Bitmap? = null

    companion object {
        var tempBitmap: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        supportActionBar?.hide()

        title = intent.getStringExtra("TITLE") ?: ""
        category = intent.getIntExtra("CATEGORY", 0)
        subcategory = intent.getIntExtra("SUBCATEGORY", 0)
        espConfig = ESPConfig(
            ipAddress = intent.getStringExtra("ESP_IP") ?: "192.168.4.1",
            port = intent.getIntExtra("ESP_PORT", 80)
        )
        bitmap = tempBitmap

        imageProcessor = ImageProcessor(this)
        initViews()

        bitmap?.let { uploadImagePreview.load(it) }

        startUpload()
    }

    private fun initViews() {
        uploadImagePreview = findViewById(R.id.uploadImagePreview)
        statusText = findViewById(R.id.statusText)
        uploadProgress = findViewById(R.id.uploadProgress)
        progressText = findViewById(R.id.progressText)
        doneButton = findViewById(R.id.doneButton)

        doneButton.setOnClickListener {
            goToGallery()
        }
    }

    private fun startUpload() {
        val bitmapToUpload = bitmap

        if (bitmapToUpload == null) {
            showError("No hay imagen para subir")
            return
        }

        lifecycleScope.launch {
            try {
                updateProgress(0, "Preparando imagen...")

                val imageBytes = withContext(Dispatchers.IO) {
                    imageProcessor.bitmapToRGB565_LE(bitmapToUpload)
                }

                updateProgress(25, "Conectando...")

                val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)

                val jsonBody = """{
                "image": "$base64Image",
                "title": "$title",
                "category": $category,
                "subcategory": $subcategory
            }""".trimIndent()

                android.util.Log.d("Upload", "Enviando: title=$title, cat=$category, subcat=$subcategory")

                val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

                val response = withContext(Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val request = okhttp3.Request.Builder()
                        .url("http://${espConfig.ipAddress}:${espConfig.port}/upload")
                        .post(body)
                        .build()

                    client.newCall(request).execute()
                }

                android.util.Log.d("Upload", "Response: ${response.code}")

                if (response.isSuccessful) {
                    updateProgress(100, "¡Imagen enviada!")
                    showSuccess()
                } else {
                    showError("Error al subir: ${response.code}")
                }

            } catch (e: Exception) {
                android.util.Log.e("Upload", "Error: ${e.message}", e)
                showError("Error: ${e.message}")
            }
        }
    }

    private fun updateProgress(progress: Int, message: String) {
        uploadProgress.progress = progress
        progressText.text = "$progress%"
        statusText.text = message
    }

    private fun showSuccess() {
        statusText.setTextColor(getColor(android.R.color.holo_green_dark))
        doneButton.visibility = View.VISIBLE
        doneButton.text = "Ir a galería"

        Toast.makeText(this, "Imagen '$title' agregada", Toast.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        statusText.text = message
        statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        uploadProgress.progress = 0
        progressText.text = "Error"
        doneButton.visibility = View.VISIBLE
        doneButton.text = "Reintentar"

        doneButton.setOnClickListener {
            statusText.setTextColor(getColor(android.R.color.black))
            doneButton.visibility = View.GONE
            doneButton.text = "Finalizar"
            startUpload()
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        tempBitmap?.recycle()
        tempBitmap = null
    }

    private fun goToGallery() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }
}