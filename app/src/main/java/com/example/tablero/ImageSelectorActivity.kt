package com.example.espdisplay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.espdisplay.models.ESPConfig
import com.example.espdisplay.utils.ImageProcessor
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSelectorActivity : AppCompatActivity() {

    private lateinit var groupTitle: TextView
    private lateinit var imagePreview: ImageView
    private lateinit var noImageText: TextView
    private lateinit var processingProgress: ProgressBar
    private lateinit var selectImageButton: MaterialButton
    private lateinit var uploadButton: MaterialButton

    private lateinit var imageProcessor: ImageProcessor
    private var selectedBitmap: Bitmap? = null
    private var groupId: Int = 0
    private var groupName: String = ""
    private var groupNumber: Int = 0
    private lateinit var espConfig: ESPConfig

    private var isProcessingImage = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && !isProcessingImage) {
            isProcessingImage = true
            handleImageSelection(uri)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(
                this,
                "Permiso denegado. No se puede acceder a la galería.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_selector)

        groupId = intent.getIntExtra("GROUP_ID", 0)
        groupName = intent.getStringExtra("GROUP_NAME") ?: "Grupo"
        groupNumber = intent.getIntExtra("GROUP_NUMBER", 0)
        espConfig = ESPConfig(
            ipAddress = intent.getStringExtra("ESP_IP") ?: "192.168.4.1",
            port = intent.getIntExtra("ESP_PORT", 80)
        )

        imageProcessor = ImageProcessor(this)
        initViews()
        setupListeners()
        updateUI()
    }

    private fun initViews() {
        groupTitle = findViewById(R.id.screenTitle)
        imagePreview = findViewById(R.id.imagePreview)
        noImageText = findViewById(R.id.noImageText)
        processingProgress = findViewById(R.id.processingProgress)
        selectImageButton = findViewById(R.id.selectImageButton)
        uploadButton = findViewById(R.id.uploadButton)

        groupTitle.text = groupName
    }

    private fun setupListeners() {
        selectImageButton.setOnClickListener {
            if (!isProcessingImage) {
                checkPermissionAndOpenGallery()
            }
        }

        uploadButton.setOnClickListener {
            selectedBitmap?.let { bitmap ->
                navigateToUpload(bitmap)
            }
        }
    }

    private fun updateUI() {
        if (selectedBitmap == null) {
            imagePreview.visibility = View.GONE
            noImageText.visibility = View.VISIBLE
            uploadButton.isEnabled = false
        } else {
            imagePreview.visibility = View.VISIBLE
            noImageText.visibility = View.GONE
            uploadButton.isEnabled = true
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permiso necesario")
                    .setMessage("La app necesita acceso a tus fotos para seleccionar imágenes.")
                    .setPositiveButton("Aceptar") { _, _ ->
                        requestPermissionLauncher.launch(permission)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        try {
            pickImageLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al abrir galería: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            isProcessingImage = false
        }
    }

    private fun handleImageSelection(uri: Uri) {
        showProcessing(true)

        lifecycleScope.launch {
            try {
                val processedBitmap = withContext(Dispatchers.IO) {
                    imageProcessor.processImage(uri)
                }

                withContext(Dispatchers.Main) {
                    if (processedBitmap != null) {
                        selectedBitmap?.recycle()
                        selectedBitmap = processedBitmap
                        imagePreview.load(processedBitmap)

                        updateUI()
                    } else {
                        Toast.makeText(
                            this@ImageSelectorActivity,
                            "Error al procesar la imagen",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    showProcessing(false)
                    isProcessingImage = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ImageSelectorActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showProcessing(false)
                    isProcessingImage = false
                }
            }
        }
    }

    private fun showProcessing(show: Boolean) {
        processingProgress.visibility = if (show) View.VISIBLE else View.GONE
        selectImageButton.isEnabled = !show
        uploadButton.isEnabled = !show && selectedBitmap != null
    }

    private fun navigateToUpload(bitmap: Bitmap) {
        val intent = Intent(this, UploadActivity::class.java).apply {
            putExtra("GROUP_ID", groupId)
            putExtra("GROUP_NAME", groupName)
            putExtra("GROUP_NUMBER", groupNumber)
            putExtra("ESP_IP", espConfig.ipAddress)
            putExtra("ESP_PORT", espConfig.port)
            putExtra("IMAGE_WIDTH", bitmap.width)
            putExtra("IMAGE_HEIGHT", bitmap.height)
        }

        UploadActivity.tempBitmap = bitmap
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (selectedBitmap != null && UploadActivity.tempBitmap != selectedBitmap) {
            selectedBitmap?.recycle()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("IS_PROCESSING", isProcessingImage)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isProcessingImage = savedInstanceState.getBoolean("IS_PROCESSING", false)
    }
}