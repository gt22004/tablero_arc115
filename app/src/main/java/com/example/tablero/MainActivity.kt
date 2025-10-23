package com.example.espdisplay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.espdisplay.models.ESPConfig
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var screen1Card: MaterialCardView
    private lateinit var screen2Card: MaterialCardView
    private lateinit var screen3Card: MaterialCardView
    private lateinit var screen4Card: MaterialCardView

    private lateinit var espConfig: ESPConfig

    companion object {
        private const val PREFS_NAME = "ESPDisplayPrefs"
        private const val KEY_ESP_IP = "esp_ip"
        private const val KEY_ESP_PORT = "esp_port"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ocultar ActionBar
        supportActionBar?.hide()

        // Cargar configuración guardada
        loadConfig()

        // Inicializar vistas
        initViews()

        // Configurar listeners
        setupListeners()
    }

    private fun initViews() {
        screen1Card = findViewById(R.id.screen1Card)
        screen2Card = findViewById(R.id.screen2Card)
        screen3Card = findViewById(R.id.screen3Card)
        screen4Card = findViewById(R.id.screen4Card)
    }

    private fun setupListeners() {
        screen1Card.setOnClickListener { navigateToSlotSelector(1) }
        screen2Card.setOnClickListener { navigateToSlotSelector(2) }
        screen3Card.setOnClickListener { navigateToSlotSelector(3) }
        screen4Card.setOnClickListener { navigateToSlotSelector(4) }
    }

    private fun navigateToSlotSelector(screenNumber: Int) {
        val intent = Intent(this, SlotSelectorActivity::class.java).apply {
            putExtra("SCREEN_NUMBER", screenNumber)
            putExtra("ESP_IP", espConfig.ipAddress)
            putExtra("ESP_PORT", espConfig.port)
        }
        startActivity(intent)
    }
    
    private fun loadConfig() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        espConfig = ESPConfig(
            ipAddress = prefs.getString(KEY_ESP_IP, "192.168.4.1") ?: "192.168.4.1",
            port = prefs.getInt(KEY_ESP_PORT, 80)
        )
    }

}