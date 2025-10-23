package com.example.espdisplay

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.espdisplay.models.ESPConfig
import com.example.espdisplay.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class SlotSelectorActivity : AppCompatActivity() {

    private var screenNumber: Int = 1
    private var selectedSlot: Int = 1
    private lateinit var espConfig: ESPConfig

    // Vistas para los 3 slots
    private lateinit var slot1Preview: ImageView
    private lateinit var slot2Preview: ImageView
    private lateinit var slot3Preview: ImageView
    private lateinit var slot1Button: MaterialButton
    private lateinit var slot2Button: MaterialButton
    private lateinit var slot3Button: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slot_selector)

        // Obtener datos del intent
        screenNumber = intent.getIntExtra("SCREEN_NUMBER", 1)
        espConfig = ESPConfig(
            ipAddress = intent.getStringExtra("ESP_IP") ?: "192.168.4.1",
            port = intent.getIntExtra("ESP_PORT", 80)
        )

        // Inicializar vistas
        initViews()

        // Configurar slots
        setupSlots()
    }

    private fun initViews() {
        slot1Preview = findViewById(R.id.slot1Preview)
        slot2Preview = findViewById(R.id.slot2Preview)
        slot3Preview = findViewById(R.id.slot3Preview)
        slot1Button = findViewById(R.id.slot1Button)
        slot2Button = findViewById(R.id.slot2Button)
        slot3Button = findViewById(R.id.slot3Button)
    }

    private fun setupSlots() {
        // Configurar listeners para cada slot
        slot1Button.setOnClickListener { showSlotOptions(1, slot1Preview) }
        slot2Button.setOnClickListener { showSlotOptions(2, slot2Preview) }
        slot3Button.setOnClickListener { showSlotOptions(3, slot3Preview) }

        // Cargar previews de los slots
        loadSlotPreview(1, slot1Preview)
        loadSlotPreview(2, slot2Preview)
        loadSlotPreview(3, slot3Preview)
    }

    private fun showSlotOptions(slot: Int, preview: ImageView) {
        selectedSlot = slot

        val options = arrayOf(
            "📤 Subir nueva imagen",
            "👁️ Mostrar esta imagen",
            "🗑️ Eliminar"
        )

        AlertDialog.Builder(this)
            .setTitle("Slot $slot - Pantalla $screenNumber")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectImageForSlot(slot)
                    1 -> showSlot(slot)
                    2 -> deleteSlot(slot)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selectImageForSlot(slot: Int) {
        val intent = Intent(this, ImageSelectorActivity::class.java).apply {
            putExtra("SCREEN_NUMBER", screenNumber)
            putExtra("SLOT_NUMBER", slot)
            putExtra("ESP_IP", espConfig.ipAddress)
            putExtra("ESP_PORT", espConfig.port)
        }
        startActivity(intent)
    }

    private fun deleteSlot(slot: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar imagen")
            .setMessage("¿Estás seguro?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val apiService = RetrofitClient.getApiService(espConfig)
                        val response = apiService.deleteSlot(
                            mapOf("screen" to screenNumber, "slot" to slot)
                        )

                        if (response.isSuccessful) {
                            Toast.makeText(this@SlotSelectorActivity, "Eliminado", Toast.LENGTH_SHORT).show()

                            val preview = when (slot) {
                                1 -> slot1Preview
                                2 -> slot2Preview
                                3 -> slot3Preview
                                else -> slot1Preview
                            }
                            loadSlotPreview(slot, preview)  // Sin delays
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@SlotSelectorActivity, "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showSlot(slot: Int) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(espConfig)
                val response = apiService.changeSlot(mapOf("screen" to screenNumber, "slot" to slot))

                if (response.isSuccessful) {
                    Toast.makeText(this@SlotSelectorActivity, "Activo", Toast.LENGTH_SHORT).show()

                    // Recargar todos
                    loadSlotPreview(1, slot1Preview)
                    loadSlotPreview(2, slot2Preview)
                    loadSlotPreview(3, slot3Preview)
                }
            } catch (e: Exception) {
                Toast.makeText(this@SlotSelectorActivity, "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSlotPreview(slot: Int, preview: ImageView) {
        val timestamp = System.currentTimeMillis()  // ← Agregar esto
        val imageUrl = "http://${espConfig.ipAddress}:${espConfig.port}" +
                "/imagen?screen=$screenNumber&slot=$slot&t=$timestamp"  // ← Agregar &t=$timestamp

        preview.load(imageUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_dialog_alert)
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar previews cuando vuelve a esta pantalla
        loadSlotPreview(1, slot1Preview)
        loadSlotPreview(2, slot2Preview)
        loadSlotPreview(3, slot3Preview)
    }
}