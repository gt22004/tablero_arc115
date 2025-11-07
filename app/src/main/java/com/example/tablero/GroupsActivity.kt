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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.espdisplay.adapters.GroupsAdapter
import com.example.espdisplay.models.CreateGroupRequest
import com.example.espdisplay.models.ESPConfig
import com.example.espdisplay.models.Group
import com.example.espdisplay.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class GroupsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var createGroupButton: MaterialButton
    private lateinit var adapter: GroupsAdapter
    private lateinit var espConfig: ESPConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groups)

        supportActionBar?.hide()

        espConfig = ESPConfig(
            ipAddress = intent.getStringExtra("ESP_IP") ?: "192.168.4.1",
            port = intent.getIntExtra("ESP_PORT", 80)
        )

        initViews()
        setupRecyclerView()
        loadGroups()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.groupsRecyclerView)
        emptyView = findViewById(R.id.emptyView)
        progressBar = findViewById(R.id.progressBar)
        createGroupButton = findViewById(R.id.createGroupButton)

        createGroupButton.setOnClickListener {
            showCreateGroupDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = GroupsAdapter(
            onGroupClick = { group ->
                openGroupGallery(group)
            },
            onGroupLongClick = { group ->
                showGroupOptions(group)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadGroups() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(espConfig)
                val response = apiService.getGroups()

                if (response.isSuccessful && response.body()?.success == true) {
                    val groups = response.body()?.groups ?: emptyList()
                    adapter.submitList(groups)
                    updateEmptyView(groups.isEmpty())
                } else {
                    Toast.makeText(
                        this@GroupsActivity,
                        "Error al cargar grupos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@GroupsActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showCreateGroupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val groupNameInput = dialogView.findViewById<TextInputEditText>(R.id.groupNameInput)

        AlertDialog.Builder(this)
            .setTitle("Crear Nuevo Grupo")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val groupName = groupNameInput.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    createGroup(groupName)
                } else {
                    Toast.makeText(this, "Ingresa un nombre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun createGroup(name: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(espConfig)
                val response = apiService.createGroup(CreateGroupRequest(name))

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@GroupsActivity,
                        "Grupo creado",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadGroups()
                } else {
                    Toast.makeText(
                        this@GroupsActivity,
                        "Error al crear grupo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@GroupsActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showGroupOptions(group: Group) {
        val options = arrayOf(
            "✏️ Renombrar",
            "🗑️ Eliminar grupo"
        )

        AlertDialog.Builder(this)
            .setTitle(group.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameGroupDialog(group)
                    1 -> confirmDeleteGroup(group)
                }
            }
            .show()
    }

    private fun showRenameGroupDialog(group: Group) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val groupNameInput = dialogView.findViewById<TextInputEditText>(R.id.groupNameInput)
        groupNameInput.setText(group.name)

        AlertDialog.Builder(this)
            .setTitle("Renombrar Grupo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = groupNameInput.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameGroup(group.id, newName)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun renameGroup(groupId: Int, newName: String) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(espConfig)
                val response = apiService.renameGroup(
                    mapOf(
                        "groupId" to groupId.toString(),
                        "name" to newName
                    )
                )

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@GroupsActivity,
                        "Grupo renombrado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadGroups()
                } else {
                    Toast.makeText(
                        this@GroupsActivity,
                        "Error al renombrar el grupo",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@GroupsActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmDeleteGroup(group: Group) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Grupo")
            .setMessage("¿Eliminar '${group.name}' y todas sus imágenes?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteGroup(group.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteGroup(groupId: Int) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(espConfig)
                val response = apiService.deleteGroup(mapOf("groupId" to groupId))

                if (response.isSuccessful) {
                    Toast.makeText(this@GroupsActivity, "Grupo eliminado", Toast.LENGTH_SHORT).show()
                    loadGroups()
                } else {
                    Toast.makeText(this@GroupsActivity, "Error al eliminar grupo", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@GroupsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGroupGallery(group: Group) {
        val intent = Intent(this, GroupGalleryActivity::class.java).apply {
            putExtra("GROUP_ID", group.id)
            putExtra("GROUP_NAME", group.name)
            putExtra("GROUP_NUMBER", group.groupNumber)
            putExtra("ESP_IP", espConfig.ipAddress)
            putExtra("ESP_PORT", espConfig.port)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        createGroupButton.isEnabled = !show
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
    }
}