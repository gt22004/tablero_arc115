package com.example.espdisplay.network

import com.example.espdisplay.models.ESPConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String = ""

    /**
     * Obtiene instancia de Retrofit configurada
     */
    fun getInstance(config: ESPConfig): Retrofit {
        val baseUrl = config.getBaseUrl()

        // Recrear si la URL cambió
        if (retrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val gson = GsonBuilder()
                .setLenient()
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }

        return retrofit!!
    }

    /**
     * Obtiene servicio API
     */
    fun getApiService(config: ESPConfig): ESPApiService {
        return getInstance(config).create(ESPApiService::class.java)
    }

    /**
     * Resetea la instancia (útil al cambiar configuración)
     */
    fun reset() {
        retrofit = null
        currentBaseUrl = ""
    }
}