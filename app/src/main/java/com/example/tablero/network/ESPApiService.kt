package com.example.espdisplay.network

import com.example.espdisplay.models.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.RequestBody

interface ESPApiService {
    //  GRUPOS
    /**
     * Obtener lista de todos los grupos
     */
    @GET("/groups")
    suspend fun getGroups(): Response<GroupsResponse>

    /**
     * Crear nuevo grupo
     */
    @POST("/groups")
    @Headers("Content-Type: application/json")
    suspend fun createGroup(
        @Body request: CreateGroupRequest
    ): Response<CreateGroupResponse>

    /**
     * Renombrar grupo
     */
    @PUT("/groups/rename")
    @Headers("Content-Type: application/json")
    suspend fun renameGroup(
        @Body data: Map<String, String>
    ): Response<DeleteResponse>


    /**
     * Eliminar grupo y todas sus imágenes
     */
    @HTTP(method = "DELETE", path = "/groups", hasBody = true)
    suspend fun deleteGroup(
        @Body data: Map<String, Int>
    ): Response<DeleteResponse>


    //  IMÁGENES DE GRUPOS

    /**
     * Subir imagen a un grupo específico
     */
    @POST("/groups/upload")
    suspend fun uploadImage(
        @Body body: RequestBody
    ): Response<UploadResponse>

    /**
     * Eliminar imagen de un grupo
     */
    @HTTP(method = "DELETE", path = "/groups/images", hasBody = true)
    suspend fun deleteGroupImage(
        @Body data: Map<String, Int>
    ): Response<DeleteResponse>
}