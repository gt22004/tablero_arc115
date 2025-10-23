package com.example.espdisplay.network

import com.example.espdisplay.models.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ESPApiService {

    /**
     * Endpoint para subir imagen como multipart
     */
    @Multipart
    @POST("/upload")
    suspend fun uploadImage(
        @Part("screen") screen: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<UploadResponse>

    /**
     * Endpoint alternativo con Base64
     */
    @POST("/upload-base64")
    @Headers("Content-Type: application/json")
    suspend fun uploadImageBase64(
        @Body data: Map<String, Any>
    ): Response<UploadResponse>

    /**
     * Endpoint para verificar conexión con ESP
     */
    @GET("/status")
    suspend fun checkStatus(): Response<Map<String, Any>>

    /**
     * Endpoint para limpiar pantalla específica
     */
    @POST("/clear")
    suspend fun clearScreen(
        @Body data: Map<String, Int>
    ): Response<UploadResponse>

    /**
     * NUEVO: Endpoint para cambiar slot activo
     */
    @POST("/change-slot")
    @Headers("Content-Type: application/json")
    suspend fun changeSlot(
        @Body data: Map<String, Int>
    ): Response<UploadResponse>

    /**
     * NUEVO: Endpoint para eliminar imagen de un slot
     */
    @POST("/delete-slot")
    @Headers("Content-Type: application/json")
    suspend fun deleteSlot(
        @Body data: Map<String, Int>
    ): Response<UploadResponse>

    /**
     * NUEVO: Endpoint para subir imagen a un slot específico
     */
    @Multipart
    @POST("/upload-slot")
    suspend fun uploadImageToSlot(
        @Part("screen") screen: RequestBody,
        @Part("slot") slot: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<UploadResponse>

    /**
     * NUEVO: Endpoint para obtener estado de todos los slots
     */
    @GET("/slots-status")
    suspend fun getSlotsStatus(): Response<Map<String, Any>>
}