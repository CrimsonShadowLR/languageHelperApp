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
            callback(null)
            return
        }

        // Create VirtualDisplay only if it doesn't exist yet
        if (virtualDisplay == null) {
            // Get screen dimensions
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

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

        // Wait a bit for the display to be ready, then capture
        handler.postDelayed({
            try {
                val image = imageReader?.acquireLatestImage()
                if (image != null) {
                    val metrics = DisplayMetrics()
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.getRealMetrics(metrics)
                    val bitmap = convertImageToBitmap(image, metrics.widthPixels, metrics.heightPixels)
                    image.close()
                    callback(bitmap)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
            // Don't cleanup VirtualDisplay - keep it alive for next capture
        }, 100)
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
