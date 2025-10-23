package com.example.espdisplay.models

import android.graphics.Bitmap
import java.io.Serializable

data class ImageData(
    val bitmap: Bitmap,
    val screenNumber: Int,
    val width: Int = 128,  // Resolución por defecto para ESP
    val height: Int = 128
) : Serializable

data class UploadResponse(
    val success: Boolean,
    val message: String
)

data class ESPConfig(
    var ipAddress: String = "192.168.4.1",  // IP por defecto del ESP en modo AP
    var port: Int = 80
) {
    fun getBaseUrl(): String = "http://$ipAddress:$port"
}