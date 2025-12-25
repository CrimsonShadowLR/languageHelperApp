package com.mangaoverlay.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Custom view for cropping images
 * Allows user to define a rectangular crop area by dragging corners and edges
 */
class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private val cropRect = RectF()
    private val bitmapRect = Rect()

    private val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = 180
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val gridPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 128
    }

    private var activeHandle: Handle? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val handleSize = 80f
    private val minCropSize = 100f

    private var showCropUI = true

    enum class Handle {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP, BOTTOM, LEFT, RIGHT, CENTER
    }

    /**
     * Set the bitmap to crop
     */
    fun setBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        post {
            calculateInitialCropRect()
            invalidate()
        }
    }

    /**
     * Show or hide the crop UI (grid lines, handles, overlay)
     * Set to false to display the image without crop controls
     */
    fun setShowCropUI(show: Boolean) {
        showCropUI = show
        invalidate()
    }

    /**
     * Calculate initial crop rectangle (80% of image centered)
     */
    private fun calculateInitialCropRect() {
        bitmap?.let { bmp ->
            val scale = min(
                width.toFloat() / bmp.width,
                height.toFloat() / bmp.height
            )

            val scaledWidth = bmp.width * scale
            val scaledHeight = bmp.height * scale
            val left = (width - scaledWidth) / 2
            val top = (height - scaledHeight) / 2

            bitmapRect.set(
                left.toInt(),
                top.toInt(),
                (left + scaledWidth).toInt(),
                (top + scaledHeight).toInt()
            )

            // Set crop rect to 80% of bitmap in center
            val margin = scaledWidth * 0.1f
            cropRect.set(
                left + margin,
                top + margin,
                left + scaledWidth - margin,
                top + scaledHeight - margin
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        bitmap?.let { bmp ->
            // Draw the bitmap
            canvas.drawBitmap(bmp, null, bitmapRect, null)

            // Only draw crop UI if enabled
            if (showCropUI) {
                // Draw dark overlay
                val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
                canvas.drawRect(cropRect, clearPaint)
                canvas.restoreToCount(layerId)

                // Draw crop rectangle border
                canvas.drawRect(cropRect, borderPaint)

                // Draw grid lines (rule of thirds)
                val gridWidth = cropRect.width() / 3
                val gridHeight = cropRect.height() / 3

                // Vertical lines
                canvas.drawLine(
                    cropRect.left + gridWidth, cropRect.top,
                    cropRect.left + gridWidth, cropRect.bottom, gridPaint
                )
                canvas.drawLine(
                    cropRect.left + gridWidth * 2, cropRect.top,
                    cropRect.left + gridWidth * 2, cropRect.bottom, gridPaint
                )

                // Horizontal lines
                canvas.drawLine(
                    cropRect.left, cropRect.top + gridHeight,
                    cropRect.right, cropRect.top + gridHeight, gridPaint
                )
                canvas.drawLine(
                    cropRect.left, cropRect.top + gridHeight * 2,
                    cropRect.right, cropRect.top + gridHeight * 2, gridPaint
                )

                // Draw corner handles
                drawHandle(canvas, cropRect.left, cropRect.top)
                drawHandle(canvas, cropRect.right, cropRect.top)
                drawHandle(canvas, cropRect.left, cropRect.bottom)
                drawHandle(canvas, cropRect.right, cropRect.bottom)

                // Draw edge handles
                drawHandle(canvas, (cropRect.left + cropRect.right) / 2, cropRect.top)
                drawHandle(canvas, (cropRect.left + cropRect.right) / 2, cropRect.bottom)
                drawHandle(canvas, cropRect.left, (cropRect.top + cropRect.bottom) / 2)
                drawHandle(canvas, cropRect.right, (cropRect.top + cropRect.bottom) / 2)
            }
        }
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, 15f, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Don't handle touch events if crop UI is hidden
        if (!showCropUI) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = getHandleAt(event.x, event.y)
                lastTouchX = event.x
                lastTouchY = event.y
                return activeHandle != null
            }

            MotionEvent.ACTION_MOVE -> {
                activeHandle?.let { handle ->
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    updateCropRect(handle, dx, dy)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = null
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getHandleAt(x: Float, y: Float): Handle? {
        // Check corners first
        if (isNear(x, cropRect.left) && isNear(y, cropRect.top)) return Handle.TOP_LEFT
        if (isNear(x, cropRect.right) && isNear(y, cropRect.top)) return Handle.TOP_RIGHT
        if (isNear(x, cropRect.left) && isNear(y, cropRect.bottom)) return Handle.BOTTOM_LEFT
        if (isNear(x, cropRect.right) && isNear(y, cropRect.bottom)) return Handle.BOTTOM_RIGHT

        // Check edges
        val centerX = (cropRect.left + cropRect.right) / 2
        val centerY = (cropRect.top + cropRect.bottom) / 2
        if (isNear(x, centerX) && isNear(y, cropRect.top)) return Handle.TOP
        if (isNear(x, centerX) && isNear(y, cropRect.bottom)) return Handle.BOTTOM
        if (isNear(x, cropRect.left) && isNear(y, centerY)) return Handle.LEFT
        if (isNear(x, cropRect.right) && isNear(y, centerY)) return Handle.RIGHT

        // Check if inside crop rect for moving
        if (cropRect.contains(x, y)) return Handle.CENTER

        return null
    }

    private fun isNear(a: Float, b: Float): Boolean {
        return kotlin.math.abs(a - b) < handleSize
    }

    private fun updateCropRect(handle: Handle, dx: Float, dy: Float) {
        val newRect = RectF(cropRect)

        when (handle) {
            Handle.TOP_LEFT -> {
                newRect.left += dx
                newRect.top += dy
            }
            Handle.TOP_RIGHT -> {
                newRect.right += dx
                newRect.top += dy
            }
            Handle.BOTTOM_LEFT -> {
                newRect.left += dx
                newRect.bottom += dy
            }
            Handle.BOTTOM_RIGHT -> {
                newRect.right += dx
                newRect.bottom += dy
            }
            Handle.TOP -> newRect.top += dy
            Handle.BOTTOM -> newRect.bottom += dy
            Handle.LEFT -> newRect.left += dx
            Handle.RIGHT -> newRect.right += dx
            Handle.CENTER -> {
                newRect.offset(dx, dy)
            }
        }

        // Constrain to bitmap bounds
        newRect.left = max(newRect.left, bitmapRect.left.toFloat())
        newRect.top = max(newRect.top, bitmapRect.top.toFloat())
        newRect.right = min(newRect.right, bitmapRect.right.toFloat())
        newRect.bottom = min(newRect.bottom, bitmapRect.bottom.toFloat())

        // Ensure minimum size
        if (newRect.width() >= minCropSize && newRect.height() >= minCropSize) {
            cropRect.set(newRect)
        }
    }

    /**
     * Get the cropped bitmap
     */
    fun getCroppedBitmap(): Bitmap? {
        bitmap?.let { bmp ->
            // Calculate the crop rect in bitmap coordinates
            val scaleX = bmp.width.toFloat() / bitmapRect.width()
            val scaleY = bmp.height.toFloat() / bitmapRect.height()

            val cropX = ((cropRect.left - bitmapRect.left) * scaleX).toInt()
            val cropY = ((cropRect.top - bitmapRect.top) * scaleY).toInt()
            val cropWidth = (cropRect.width() * scaleX).toInt()
            val cropHeight = (cropRect.height() * scaleY).toInt()

            // Ensure coordinates are within bounds
            val x = max(0, min(cropX, bmp.width - 1))
            val y = max(0, min(cropY, bmp.height - 1))
            val w = min(cropWidth, bmp.width - x)
            val h = min(cropHeight, bmp.height - y)

            return if (w > 0 && h > 0) {
                Bitmap.createBitmap(bmp, x, y, w, h)
            } else {
                null
            }
        }
        return null
    }
}
