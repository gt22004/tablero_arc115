package com.example.espdisplay.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ImageProcessor(private val context: Context) {

    companion object {
        const val TARGET_WIDTH = 128
        const val TARGET_HEIGHT = 128
        const val JPEG_QUALITY = 70
        const val MAX_FILE_SIZE_KB = 15
    }

    /**
     * Procesa una imagen desde URI: redimensiona, optimiza y comprime
     */
    fun processImage(uri: Uri): Bitmap? {
        return try {
            // 1. Leer imagen con opciones para no cargar toda en memoria
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            // 2. Calcular factor de escala
            val scaleFactor = calculateScaleFactor(
                options.outWidth,
                options.outHeight,
                TARGET_WIDTH,
                TARGET_HEIGHT
            )

            // 3. Cargar imagen con escala
            val scaledOptions = BitmapFactory.Options().apply {
                inSampleSize = scaleFactor
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, scaledOptions)
            } ?: return null

            // 4. Corregir orientación
            val rotatedBitmap = correctOrientation(uri, bitmap)

            // 5. Redimensionar exacto a tamaño objetivo
            val resizedBitmap = resizeBitmap(rotatedBitmap, TARGET_WIDTH, TARGET_HEIGHT)

            // 6. Liberar bitmap temporal si es diferente
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            if (resizedBitmap != rotatedBitmap) {
                rotatedBitmap.recycle()
            }

            resizedBitmap

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calcula el factor de escala óptimo
     */
    private fun calculateScaleFactor(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var scaleFactor = 1

        while (sourceWidth / (scaleFactor * 2) >= targetWidth &&
            sourceHeight / (scaleFactor * 2) >= targetHeight) {
            scaleFactor *= 2
        }

        return scaleFactor
    }

    /**
     * Corrige la orientación de la imagen según EXIF
     */
    private fun correctOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Redimensiona bitmap al tamaño exacto
     */
    private fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * Convierte bitmap a ByteArray comprimido en JPEG
     */
    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = JPEG_QUALITY): ByteArray {
        val outputStream = ByteArrayOutputStream()
        var currentQuality = quality
        var byteArray: ByteArray

        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
            byteArray = outputStream.toByteArray()
            currentQuality -= 5
        } while (byteArray.size > MAX_FILE_SIZE_KB * 1024 && currentQuality > 10)

        return byteArray
    }

    /**
     * Obtiene información del tamaño de la imagen procesada
     */
    fun getImageSize(bitmap: Bitmap): String {
        val byteArray = bitmapToByteArray(bitmap)
        val sizeKB = byteArray.size / 1024.0
        return String.format("%.2f KB (%d x %d)", sizeKB, bitmap.width, bitmap.height)
    }
}