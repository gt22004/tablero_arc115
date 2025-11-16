package com.example.espdisplay.models

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val groupNumber: Int,
    val fileName: String
)

data class ESPConfig(
    var ipAddress: String = "192.168.4.1",
    var port: Int = 80
){
    fun getBaseUrl(): String = "http://$ipAddress:$port"
}

data class ImageItem(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val fileName: String,
    val category: Int,
    val subcategory: Int,
    val timestamp: Long
)