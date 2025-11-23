package com.mangaoverlay.app.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import com.mangaoverlay.app.api.ApiConfig
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Utility class for image processing operations
 */
object ImageProcessor {

    private const val TAG = "ImageProcessor"
    private const val DEFAULT_QUALITY = 85
    private const val MIN_QUALITY = 50

    /**
     * Compress bitmap to ensure it's under the specified max size
     * @param bitmap The bitmap to compress
     * @param maxSizeBytes Maximum size in bytes (default: 2MB)
     * @return Compressed bitmap or original if already under size
     */
    fun compressBitmap(bitmap: Bitmap, maxSizeBytes: Int = ApiConfig.MAX_IMAGE_SIZE_BYTES): Bitmap {
        val outputStream = ByteArrayOutputStream()
        var quality = DEFAULT_QUALITY

        // Try compressing with decreasing quality until under size limit
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        while (outputStream.size() > maxSizeBytes && quality >= MIN_QUALITY) {
            outputStream.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            Log.d(TAG, "Compressing with quality $quality, size: ${outputStream.size()} bytes")
        }

        // If still too large, scale down the bitmap
        if (outputStream.size() > maxSizeBytes) {
            Log.d(TAG, "Image still too large, scaling down")
            val scaleFactor = kotlin.math.sqrt(maxSizeBytes.toDouble() / outputStream.size())
            val scaledWidth = (bitmap.width * scaleFactor).toInt()
            val scaledHeight = (bitmap.height * scaleFactor).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

            outputStream.reset()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY, outputStream)
            Log.d(TAG, "Scaled to ${scaledWidth}x${scaledHeight}, final size: ${outputStream.size()} bytes")

            return scaledBitmap
        }

        return bitmap
    }

    /**
     * Convert bitmap to base64 string
     * @param bitmap The bitmap to convert
     * @param format The compression format (default: JPEG)
     * @param quality The compression quality (default: 85)
     * @return Base64 encoded string
     */
    fun bitmapToBase64(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = DEFAULT_QUALITY
    ): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(format, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Compress and convert bitmap to base64
     * This is a convenience method that combines compression and base64 encoding
     * @param bitmap The bitmap to process
     * @return Base64 encoded string of compressed bitmap
     */
    fun compressAndEncode(bitmap: Bitmap): String {
        val compressed = compressBitmap(bitmap)
        return bitmapToBase64(compressed)
    }

    /**
     * Calculate MD5 hash of base64 string for caching purposes
     * @param base64String The base64 string to hash
     * @return MD5 hash string
     */
    fun calculateImageHash(base64String: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(base64String.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Rotate bitmap by specified degrees
     * @param bitmap The bitmap to rotate
     * @param degrees The rotation angle in degrees
     * @return Rotated bitmap
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Get the size of a bitmap in bytes
     * @param bitmap The bitmap to measure
     * @return Size in bytes
     */
    fun getBitmapSize(bitmap: Bitmap): Int {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY, outputStream)
        return outputStream.size()
    }
}
