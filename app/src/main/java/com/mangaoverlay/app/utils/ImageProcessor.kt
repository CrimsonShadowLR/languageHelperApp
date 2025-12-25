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
    private const val MIN_QUALITY = 40
    private const val MAX_DIMENSION = 1920 // Max width or height in pixels

    /**
     * Optimized compression targeting a specific size with two-pass approach
     * @param bitmap The bitmap to compress
     * @param targetSizeBytes Target size in bytes (default: 75KB)
     * @param maxSizeBytes Maximum size in bytes (default: 100KB)
     * @return Compressed bitmap
     */
    fun compressBitmapOptimized(
        bitmap: Bitmap,
        targetSizeBytes: Int = ApiConfig.TARGET_IMAGE_SIZE_BYTES,
        maxSizeBytes: Int = ApiConfig.MAX_IMAGE_SIZE_BYTES
    ): Bitmap {
        Log.d(TAG, "Starting compression: ${bitmap.width}x${bitmap.height}")

        // Step 1: Pre-scale if image is too large (reduces memory and improves compression)
        var workingBitmap = prescaleIfNeeded(bitmap)
        Log.d(TAG, "After prescale: ${workingBitmap.width}x${workingBitmap.height}")

        // Step 2: Find optimal quality for target size
        var quality = findOptimalQuality(workingBitmap, targetSizeBytes, maxSizeBytes)
        var outputStream = ByteArrayOutputStream()
        workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        // Step 3: If still too large, progressively scale down
        var scaleFactor = 1.0
        while (outputStream.size() > maxSizeBytes && scaleFactor > 0.3) {
            outputStream.reset()
            scaleFactor -= 0.1
            val newWidth = (workingBitmap.width * scaleFactor).toInt().coerceAtLeast(100)
            val newHeight = (workingBitmap.height * scaleFactor).toInt().coerceAtLeast(100)

            val scaledBitmap = Bitmap.createScaledBitmap(workingBitmap, newWidth, newHeight, true)
            if (scaledBitmap != workingBitmap && scaledBitmap != bitmap) {
                workingBitmap.recycle()
            }
            workingBitmap = scaledBitmap

            // Re-find optimal quality for new size
            quality = findOptimalQuality(workingBitmap, targetSizeBytes, maxSizeBytes)
            workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            Log.d(TAG, "Scaled to ${newWidth}x${newHeight}, quality $quality, size: ${outputStream.size()} bytes")
        }

        Log.d(TAG, "Final: ${workingBitmap.width}x${workingBitmap.height}, quality $quality, size: ${outputStream.size()} bytes")
        return workingBitmap
    }

    /**
     * Pre-scale bitmap if dimensions exceed maximum
     */
    private fun prescaleIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= MAX_DIMENSION) {
            return bitmap
        }

        val scale = MAX_DIMENSION.toFloat() / maxDim
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Find optimal quality setting to get close to target size
     */
    private fun findOptimalQuality(bitmap: Bitmap, targetSize: Int, maxSize: Int): Int {
        var low = MIN_QUALITY
        var high = DEFAULT_QUALITY
        var bestQuality = MIN_QUALITY

        // Binary search for optimal quality
        while (low <= high) {
            val mid = (low + high) / 2
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, mid, outputStream)
            val size = outputStream.size()

            if (size <= maxSize) {
                bestQuality = mid
                if (size >= targetSize) {
                    return mid // Found quality that gives size in target range
                }
                low = mid + 1 // Try higher quality
            } else {
                high = mid - 1 // Size too large, reduce quality
            }
        }

        return bestQuality
    }

    /**
     * Compress bitmap to ensure it's under the specified max size (legacy method)
     * @param bitmap The bitmap to compress
     * @param maxSizeBytes Maximum size in bytes (default: 100KB)
     * @return Compressed bitmap or original if already under size
     */
    @Deprecated("Use compressBitmapOptimized for better results", ReplaceWith("compressBitmapOptimized(bitmap, maxSizeBytes * 3/4, maxSizeBytes)"))
    fun compressBitmap(bitmap: Bitmap, maxSizeBytes: Int = ApiConfig.MAX_IMAGE_SIZE_BYTES): Bitmap {
        return compressBitmapOptimized(bitmap, maxSizeBytes * 3 / 4, maxSizeBytes)
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
     * Uses optimized compression to target 75KB with max 100KB
     * @param bitmap The bitmap to process
     * @return Base64 encoded string of compressed bitmap
     */
    fun compressAndEncode(bitmap: Bitmap): String {
        val compressed = compressBitmapOptimized(bitmap)
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
