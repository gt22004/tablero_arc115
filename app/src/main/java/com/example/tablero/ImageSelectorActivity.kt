package com.example.espdisplay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSelectorActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var noImageText: TextView
    private lateinit var processingProgress: ProgressBar
    private lateinit var selectImageButton: MaterialButton
    private lateinit var uploadButton: MaterialButton
    private lateinit var titleInput: TextInputEditText
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var subcategoryDropdown: AutoCompleteTextView

    private lateinit var imageProcessor: ImageProcessor
    private var selectedBitmap: Bitmap? = null
    private lateinit var espConfig: ESPConfig
    private var isProcessingImage = false

    // Mapeo de categorías (0-11)
    private val categories = mapOf(
        "HOGAR" to 0,
        "COMIDA" to 1,
        "HIGIENE (Cuidado Personal)" to 2,
        "VESTIR (La Ropa)" to 3,
        "PERSONAS (Roles y Relaciones)" to 4,
        "SENTIMIENTOS (Emociones y Salud)" to 5,
        "COMUNICACIÓN (Verbos Esenciales)" to 6,
        "CONCEPTOS (Abstractos y Cualidades)" to 7,
        "JUEGO (Actividades de Ocio)" to 8,
        "LUGARES" to 9,
        "ACCIONES" to 10,
        "TIEMPO" to 11
    )

    // Subcategorías por cada categoría
    private val subcategoriesByCategory = mapOf(
        0 to listOf("Muebles", "Aparatos Eléctricos", "Textiles", "Objetos Pequeños", "Iluminación",
            "Lugares de Guardado", "Tareas/Rutinas", "Zona de Descanso", "Zona de Estudio",
            "Zona de Juego Interior", "Materiales de Reparación", "Puertas/Ventanas"),
        1 to listOf("Desayuno", "Almuerzo", "Cena", "Meriendas", "Postres", "Bebidas Frías",
            "Bebidas Calientes", "Frutas y Verduras", "Comida Envasada", "Herramientas de Cocina",
            "Ingredientes Crudos", "Comida Fuera de Casa"),
        2 to listOf("Cara y Pelo", "Cuerpo y Piel", "Boca y Dientes", "Uso del Baño",
            "Artículos de Limpieza Personal", "Bañarse/Ducharse", "Productos Médicos",
            "Olores y Aromas", "Herramientas de Higiene", "Accesorios para el Pelo",
            "Ayuda/Supervisión", "Higiene de Manos"),
        3 to listOf("Parte Superior", "Parte Inferior", "Calzado", "Ropa de Dormir", "Ropa de Abrigo",
            "Ropa Interior", "Accesorios", "Ropa de Estaciones", "Ropa para Eventos",
            "Ropa Deportiva", "Cuidar la Ropa", "Ponerse/Quitarse"),
        4 to listOf("Familiares Directos", "Familia Extendida", "Maestros/Educadores", "Amigos/Compañeros",
            "Médicos/Personal Sanitario", "Profesionales de Ayuda", "Gente Nueva",
            "Personajes Ficticios", "Roles Comunitarios", "Animales Domésticos",
            "Grupos/Parejas", "Yo Mismo"),
        5 to listOf("Placer/Alegría", "Disgusto/Frustración", "Miedo/Ansiedad", "Calma/Tranquilidad",
            "Dolor Físico", "Sensaciones Corporales", "Necesidad de Consuelo", "Aburrimiento",
            "Confusión", "Emociones de Otros", "Salud General", "Estar Enfermo"),
        6 to listOf("Pedir/Solicitar", "Responder/Aceptar", "Negar/Rechazar", "Preguntar", "Contar/Informar",
            "Describir", "Conversación", "Expresar Deseos", "Mandatos", "Saludos/Despedidas",
            "Turnos", "Sentido del Humor"),
        7 to listOf("Colores", "Formas", "Tamaños", "Números", "Letras/Sonidos", "Direcciones",
            "Opuestos", "Cualidades", "Tiempo", "Intensidad", "Adjetivos de Estado",
            "Conceptos Abstractos"),
        8 to listOf("Juguetes de Manipulación", "Juegos de Construcción", "Juegos de Mesa", "Arte",
            "Libros/Lectura", "Música/Sonidos", "Juegos al Aire Libre", "Deportes/Ejercicio",
            "Videojuegos", "Actividades Sensoriales", "Jugar Solos", "Jugar Juntos"),
        9 to listOf("Escuela", "Tiendas", "Servicios", "Ocio", "Transporte", "Naturaleza",
            "Lugares de Emergencia", "Espacios de Trabajo", "Caminar/Moverse",
            "Lugares de Culto", "Lugares para Comer", "Lugares de Descanso"),
        10 to listOf("Movimiento Física", "Posición", "Interacción Social", "Tareas Domésticas",
            "Habilidades Motoras", "Aprender", "Sensoriales", "Comunicación", "Cuidado",
            "Creación", "Empezar/Terminar", "Querer/Necesitar"),
        11 to listOf("Días de la Semana", "Partes del Día", "Meses y Estaciones", "Hoy/Ayer/Mañana",
            "Horas y Reloj", "Secuencias y Rutinas", "Esperar", "Frecuencia",
            "Momentos Específicos", "Fechas Especiales", "El Calendario", "Predecir/Adivinar")
    )

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
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_selector)

        supportActionBar?.hide()

        espConfig = ESPConfig(
            ipAddress = intent.getStringExtra("ESP_IP") ?: "192.168.4.1",
            port = intent.getIntExtra("ESP_PORT", 80)
        )

        imageProcessor = ImageProcessor(this)
        initViews()
        setupDropdowns()
        setupListeners()
        updateUI()
    }

    private fun initViews() {
        imagePreview = findViewById(R.id.imagePreview)
        noImageText = findViewById(R.id.noImageText)
        processingProgress = findViewById(R.id.processingProgress)
        selectImageButton = findViewById(R.id.selectImageButton)
        uploadButton = findViewById(R.id.uploadButton)
        titleInput = findViewById(R.id.titleInput)
        categoryDropdown = findViewById(R.id.categoryDropdown)
        subcategoryDropdown = findViewById(R.id.subcategoryDropdown)
    }

    private fun setupDropdowns() {
        // Configurar dropdown de categorías
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories.keys.toList()
        )
        categoryDropdown.setAdapter(categoryAdapter)

        // Listener para cambiar subcategorías según categoría seleccionada
        categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedCategoryName = categories.keys.toList()[position]
            val categoryNumber = categories[selectedCategoryName] ?: 0

            // Actualizar subcategorías
            val subcategoryList = subcategoriesByCategory[categoryNumber] ?: emptyList()
            val subcategoryAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                subcategoryList
            )
            subcategoryDropdown.setAdapter(subcategoryAdapter)
            subcategoryDropdown.text.clear()

            updateUI()
        }

        subcategoryDropdown.setOnItemClickListener { _, _, _, _ ->
            updateUI()
        }
    }

    private fun setupListeners() {
        selectImageButton.setOnClickListener {
            if (!isProcessingImage) {
                checkPermissionAndOpenGallery()
            }
        }

        uploadButton.setOnClickListener {
            validateAndUpload()
        }
    }

    private fun validateAndUpload() {
        val title = titleInput.text.toString().trim()
        val categoryText = categoryDropdown.text.toString()
        val subcategoryText = subcategoryDropdown.text.toString()

        when {
            title.isEmpty() -> {
                Toast.makeText(this, "Ingresa un título", Toast.LENGTH_SHORT).show()
            }
            categoryText.isEmpty() -> {
                Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show()
            }
            subcategoryText.isEmpty() -> {
                Toast.makeText(this, "Selecciona una subcategoría", Toast.LENGTH_SHORT).show()
            }
            selectedBitmap == null -> {
                Toast.makeText(this, "Selecciona una imagen", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val categoryNum = categories[categoryText] ?: 0

                // Obtener índice de subcategoría
                val subcategoryList = subcategoriesByCategory[categoryNum] ?: emptyList()
                val subcategoryNum = subcategoryList.indexOf(subcategoryText)

                selectedBitmap?.let { bitmap ->
                    navigateToUpload(bitmap, title, categoryNum, subcategoryNum)
                }
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

            val hasTitle = titleInput.text.toString().trim().isNotEmpty()
            val hasCategory = categoryDropdown.text.toString().isNotEmpty()
            val hasSubcategory = subcategoryDropdown.text.toString().isNotEmpty()

            uploadButton.isEnabled = hasTitle && hasCategory && hasSubcategory
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permiso necesario")
                    .setMessage("La app necesita acceso a tus fotos.")
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
            Toast.makeText(this, "Error al abrir galería: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            "Error al procesar imagen",
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

    private fun navigateToUpload(bitmap: Bitmap, title: String, category: Int, subcategory: Int) {
        val intent = Intent(this, UploadActivity::class.java).apply {
            putExtra("TITLE", title)
            putExtra("CATEGORY", category)
            putExtra("SUBCATEGORY", subcategory)
            putExtra("ESP_IP", espConfig.ipAddress)
            putExtra("ESP_PORT", espConfig.port)
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
}