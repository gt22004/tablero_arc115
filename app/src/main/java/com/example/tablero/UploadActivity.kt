package com.example.espdisplay

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
import com.example.espdisplay.network.RetrofitClient
import com.example.espdisplay.utils.ImageProcessor
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class UploadActivity : AppCompatActivity() {

    private lateinit var uploadImagePreview: ImageView
    private lateinit var statusText: TextView
    private lateinit var uploadProgress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var doneButton: MaterialButton

    private lateinit var imageProcessor: ImageProcessor
    private var screenNumber: Int = 1
    private var slotNumber: Int = 1
    private lateinit var espConfig: ESPConfig
    private var bitmap: Bitmap? = null

    companion object {
        // Variable estática temporal para pasar bitmap entre activities
        // En producción es mejor usar ViewModel o guardar en archivo
        var tempBitmap: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        // Obtener datos
        screenNumber = intent.getIntExtra("SCREEN_NUMBER", 1)
        slotNumber = intent.getIntExtra("SLOT_NUMBER", 1)
        espConfig = ESPConfig(
            ipAddress = intent.getStringExtra("ESP_IP") ?: "192.168.4.1",
            port = intent.getIntExtra("ESP_PORT", 80)
        )
        bitmap = tempBitmap

        // Inicializar
        imageProcessor = ImageProcessor(this)
        initViews()

        // Mostrar preview
        bitmap?.let { uploadImagePreview.load(it) }

        // Iniciar subida automáticamente
        startUpload()
    }

    private fun initViews() {
        uploadImagePreview = findViewById(R.id.uploadImagePreview)
        statusText = findViewById(R.id.statusText)
        uploadProgress = findViewById(R.id.uploadProgress)
        progressText = findViewById(R.id.progressText)
        doneButton = findViewById(R.id.doneButton)

        doneButton.setOnClickListener {
            finish()
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

                // Convertir imagen a bytes
                val imageBytes = withContext(Dispatchers.IO) {
                    imageProcessor.bitmapToByteArray(bitmapToUpload)
                }

                updateProgress(25, "Conectando...")

                // Simular delay de conexión
                delay(500)

                // Intentar subir imagen
                val success = uploadToESP(imageBytes)

                if (success) {
                    updateProgress(100, getString(R.string.upload_success))
                    showSuccess()
                } else {
                    showError("Error al subir imagen")
                }

            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private suspend fun uploadToESP(imageBytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val slotNumber = intent.getIntExtra("SLOT_NUMBER", 1)

                val baseUrl = "http://${espConfig.ipAddress}:${espConfig.port}"
                val uploadUrl = "$baseUrl/upload-slot"

                withContext(Dispatchers.Main) {
                    updateProgress(50, "Comprimiendo imagen...")
                }

                // Convertir a Base64
                val base64Image = android.util.Base64.encodeToString(
                    imageBytes,
                    android.util.Base64.NO_WRAP
                )

                withContext(Dispatchers.Main) {
                    updateProgress(60, "Enviando...")
                }

                // Crear JSON con los datos
                val jsonBody = """{
                "screen": $screenNumber,
                "slot": $slotNumber,
                "image": "$base64Image"
            }""".trimIndent()

                val client = okhttp3.OkHttpClient()

                val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

                val request = okhttp3.Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                android.util.Log.d("Upload", "Response code: ${response.code}")
                val responseBody = response.body?.string()
                android.util.Log.d("Upload", "Response: $responseBody")

                withContext(Dispatchers.Main) {
                    updateProgress(75, "Procesando En Tablero...")
                }
                delay(1000)

                response.isSuccessful

            } catch (e: Exception) {
                android.util.Log.e("Upload", "Error: ${e.message}", e)
                false
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

        Toast.makeText(
            this,
            "Imagen enviada correctamente a Pantalla $screenNumber",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showError(message: String) {
        statusText.text = message
        statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        uploadProgress.progress = 0
        progressText.text = "Error"
        doneButton.visibility = View.VISIBLE
        doneButton.text = "Reintentar"

        doneButton.setOnClickListener {
            // Reiniciar UI
            statusText.setTextColor(getColor(android.R.color.black))
            doneButton.visibility = View.GONE
            doneButton.text = "Finalizar"

            // Reintentar subida
            startUpload()
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar bitmap temporal
        tempBitmap?.recycle()
        tempBitmap = null
    }
}