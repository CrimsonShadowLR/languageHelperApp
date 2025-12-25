package com.mangaoverlay.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import java.nio.ByteBuffer

/**
 * Manages screen capture using MediaProjection API
 * Handles the lifecycle of screen recording and capturing screenshots
 */
class ScreenCaptureManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handlerThread = HandlerThread("ScreenCaptureThread").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            cleanup()
        }
    }

    /**
     * Initialize MediaProjection with permission data
     */
    fun initializeProjection(resultCode: Int, data: Intent) {
        // Create initial MediaProjection
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        // Register callback as required by Android API
        mediaProjection?.registerCallback(projectionCallback, handler)
    }

    /**
     * Capture the screen and return a bitmap
     * @param callback Callback with captured bitmap or error
     */
    fun captureScreen(callback: (Bitmap?) -> Unit) {
        if (mediaProjection == null) {
            android.util.Log.e("ScreenCaptureManager", "MediaProjection is null")
            callback(null)
            return
        }

        try {
            // Get screen dimensions
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            // Create VirtualDisplay only if it doesn't exist yet
            if (virtualDisplay == null) {
                android.util.Log.d("ScreenCaptureManager", "Creating VirtualDisplay: ${width}x${height}")
                
                // Create ImageReader for capturing the screen
                imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

                // Create VirtualDisplay for screen mirroring (only once per MediaProjection)
                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "ScreenCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader?.surface,
                    null,
                    handler
                )
            }

            // Set up one-time listener for image capture
            imageReader?.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        android.util.Log.d("ScreenCaptureManager", "Image captured successfully")
                        val bitmap = convertImageToBitmap(image, width, height)
                        image.close()
                        
                        // Clear the listener after capturing
                        reader.setOnImageAvailableListener(null, null)
                        callback(bitmap)
                    } else {
                        android.util.Log.w("ScreenCaptureManager", "Image is null")
                        reader.setOnImageAvailableListener(null, null)
                        callback(null)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ScreenCaptureManager", "Error capturing image", e)
                    reader.setOnImageAvailableListener(null, null)
                    callback(null)
                }
            }, handler)

            // Add a timeout in case image never arrives
            handler.postDelayed({
                if (imageReader?.acquireLatestImage() == null) {
                    android.util.Log.e("ScreenCaptureManager", "Timeout waiting for image")
                    imageReader?.setOnImageAvailableListener(null, null)
                    callback(null)
                }
            }, 3000) // 3 second timeout

        } catch (e: Exception) {
            android.util.Log.e("ScreenCaptureManager", "Error setting up screen capture", e)
            callback(null)
        }
    }

    /**
     * Convert Image to Bitmap
     */
    private fun convertImageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        // Create bitmap from buffer
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // Crop to exact dimensions if there's padding
        return if (rowPadding > 0) {
            Bitmap.createBitmap(bitmap, 0, 0, width, height)
        } else {
            bitmap
        }
    }

    /**
     * Clean up resources
     */
    private fun cleanup() {
        virtualDisplay?.release()
        imageReader?.close()
        virtualDisplay = null
        imageReader = null
    }

    /**
     * Stop the MediaProjection and release all resources
     */
    fun stop() {
        cleanup()
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
        handlerThread.quitSafely()
    }

    /**
     * Check if MediaProjection is initialized
     */
    fun isInitialized(): Boolean = mediaProjection != null
}
