package com.example.espdisplay

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.espdisplay.adapters.GroupImagesAdapter
import com.example.espdisplay.models.ESPConfig
import com.example.espdisplay.models.GroupImage
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class GroupGalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var addImageFab: FloatingActionButton
    private lateinit var backFab: FloatingActionButton
    private lateinit var groupTitle: TextView
    private lateinit var groupNumberText: TextView
    private lateinit var adapter: GroupImagesAdapter
    private lateinit var espConfig: ESPConfig

    private var groupId: Int = 0
    private var groupName: String = ""
    private var groupNumber: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_gallery)

        supportActionBar?.hide()

        groupId = intent.getIntExtra("GROUP_ID", 0)
        groupName = intent.getStringExtra("GROUP_NAME") ?: "Grupo"
        groupNumber = intent.getIntExtra("GROUP_NUMBER", 0)
        espConfig = ESPConfig(
            ipAddress = intent.getStringExtra("ESP_IP") ?: "192.168.4.1",
            port = intent.getIntExtra("ESP_PORT", 80)
        )

        initViews()
        setupRecyclerView()
        loadImages()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.imagesRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        progressBar = findViewById(R.id.progressBar)
        addImageFab = findViewById(R.id.addImageFab)
        groupTitle = findViewById(R.id.groupTitle)
        groupNumberText = findViewById(R.id.groupNumber)
        backFab = findViewById(R.id.backFab)

        groupTitle.text = groupName
        groupNumberText.text = "Grupo #$groupNumber"

        addImageFab.setOnClickListener {
            openImageSelector()
        }

        backFab.setOnClickListener {
            back()
        }
    }

    private fun setupRecyclerView() {
        adapter = GroupImagesAdapter(
            espConfig = espConfig,
            onImageClick = { image ->
                showImageOptions(image)
            }
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }

    private fun loadImages() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Todo dentro de withContext IO
                val result = withContext(Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url("http://${espConfig.ipAddress}:${espConfig.port}/groups/images?groupId=$groupId")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    Pair(response.isSuccessful, responseBody)
                }

                val (isSuccessful, responseBody) = result

                if (isSuccessful && responseBody != null) {
                    android.util.Log.d("Gallery", "Response: $responseBody")

                    val json = org.json.JSONObject(responseBody)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val imagesArray = json.optJSONArray("images")
                        val images = mutableListOf<GroupImage>()

                        if (imagesArray != null) {
                            for (i in 0 until imagesArray.length()) {
                                val imgObj = imagesArray.getJSONObject(i)
                                val image = GroupImage(
                                    id = imgObj.getInt("id"),
                                    groupId = imgObj.getInt("groupId"),
                                    groupNumber = imgObj.optInt("groupNumber", groupNumber),
                                    fileName = imgObj.optString("fileName", ""),
                                    imageUrl = imgObj.getString("imageUrl"),
                                    timestamp = imgObj.getLong("timestamp")
                                )
                                images.add(image)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            adapter.submitList(images)
                            updateEmptyView(images.isEmpty())
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@GroupGalleryActivity,
                                "Error al cargar imágenes",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@GroupGalleryActivity,
                            "Error al cargar imágenes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Gallery", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@GroupGalleryActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                }
            }
        }
    }

    private fun showImageOptions(image: GroupImage) {
        val options = arrayOf(
            "👁️ Ver en grande",
            "🗑️ Eliminar"
        )

        AlertDialog.Builder(this)
            .setTitle("Opciones")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showFullImage(image)
                    1 -> confirmDeleteImage(image)
                }
            }
            .show()
    }

    private fun showFullImage(image: GroupImage) {
        Toast.makeText(this, "Ver imagen en grande (por implementar)", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDeleteImage(image: GroupImage) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Imagen")
            .setMessage("¿Estás seguro de eliminar esta imagen?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteImage(image)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteImage(image: GroupImage) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("Gallery", "Eliminando: ${image.fileName} del grupo #${image.groupNumber}")

                val client = okhttp3.OkHttpClient()

                // Crear JSON con fileName y groupNumber
                val jsonBody = """
                    {
                        "groupNumber": ${image.groupNumber},
                        "fileName": "${image.fileName}"
                    }
                """.trimIndent()

                android.util.Log.d("Gallery", "JSON: $jsonBody")

                val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

                val request = okhttp3.Request.Builder()
                    .url("http://${espConfig.ipAddress}:${espConfig.port}/groups/images")
                    .delete(requestBody)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                android.util.Log.d("Gallery", "Response: ${response.code}")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@GroupGalleryActivity,
                            "Imagen eliminada",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadImages()
                    } else {
                        Toast.makeText(
                            this@GroupGalleryActivity,
                            "Error al eliminar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Gallery", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@GroupGalleryActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openImageSelector() {
        val intent = Intent(this, ImageSelectorActivity::class.java).apply {
            putExtra("GROUP_ID", groupId)
            putExtra("GROUP_NAME", groupName)
            putExtra("GROUP_NUMBER", groupNumber)
            putExtra("ESP_IP", espConfig.ipAddress)
            putExtra("ESP_PORT", espConfig.port)
        }
        startActivity(intent)
    }

    private fun back() {
        val intent = Intent(this, GroupsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_out_right, android.R.anim.slide_in_left)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        addImageFab.isEnabled = !show
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadImages()
    }


}