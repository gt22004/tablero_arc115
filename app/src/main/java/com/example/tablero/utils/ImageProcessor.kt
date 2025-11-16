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
        const val TARGET_WIDTH = 130
        const val TARGET_HEIGHT = 130
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


    fun bitmapToRGB565(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val buffer = ByteArray(width * height * 2)
        var index = 0

        for (color in pixels) {
            val r = (color shr 19) and 0x1F
            val g = (color shr 10) and 0x3F
            val b = (color shr 3) and 0x1F

            val rgb565 = (r shl 11) or (g shl 5) or b

            buffer[index++] = (rgb565 shr 8).toByte()
            buffer[index++] = (rgb565 and 0xFF).toByte()
        }

        return buffer
    }

    fun bitmapToRGB565_LE(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val buffer = ByteArray(width * height * 2)
        var index = 0

        for (color in pixels) {

            val r = (color shr 19) and 0x1F
            val g = (color shr 10) and 0x3F
            val b = (color shr 3) and 0x1F

            val rgb565 = (r shl 11) or (g shl 5) or b

            buffer[index++] = (rgb565 and 0xFF).toByte()         // LOW
            buffer[index++] = ((rgb565 shr 8) and 0xFF).toByte() // HIGH
        }

        return buffer
    }
}