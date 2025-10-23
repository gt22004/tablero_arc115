package com.example.espdisplay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SplashActivity : AppCompatActivity() {

    private lateinit var startButton: MaterialButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ocultar ActionBar
        supportActionBar?.hide()

        // Inicializar vistas
        initViews()

        // Configurar botón
        startButton.setOnClickListener {
            navigateToMain()
        }
    }

    private fun initViews() {
        startButton = findViewById(R.id.startButton)
    }

    private fun navigateToMain() {
            showWiFiInstructions()
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
                        "3️⃣ Selecciona pantalla y sube la imagen\n\n"
            )
            .setPositiveButton("¡Entendido!") { dialog, _ ->
                dialog.dismiss()
                startMainActivity()
            }
            .setCancelable(false)
            .show()
    }
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Cierra SplashActivity para que no pueda volver
    }
}