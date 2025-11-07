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
) {
    fun getBaseUrl(): String = "http://$ipAddress:$port"
}