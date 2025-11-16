package com.example.espdisplay

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SplashActivity : AppCompatActivity() {

    private lateinit var startButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        supportActionBar?.hide()

        initViews()

        startButton.setOnClickListener {
            showWiFiInstructions()
        }
    }

    private fun initViews() {
        startButton = findViewById(R.id.startButton)
    }

    private fun showWiFiInstructions() {
        AlertDialog.Builder(this)
            .setTitle("📡 Conexión con ESP32")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setMessage(
                "Para usar la aplicación:\n\n" +
                        "1️⃣ Enciende el Tablero \n\n" +
                        "2️⃣ Conecta tu teléfono al WiFi:\n" +
                        "   • Nombre: TableroV0.1\n" +
                        "   • Contraseña: tableroarc\n\n" +
                        "3️⃣ Agrega tus imágenes al tablero\n\n"
            )
            .setPositiveButton("¡Entendido!") { dialog, _ ->
                dialog.dismiss()
                startGalleryActivity()
            }
            .setCancelable(false)
            .show()
    }

    private fun startGalleryActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("ESP_IP", "192.168.4.1")
            putExtra("ESP_PORT", 80)
        }
        startActivity(intent)
        finish()
    }
}