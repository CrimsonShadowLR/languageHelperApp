package com.mangaoverlay.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Foreground service that displays a floating overlay button
 * The button can be dragged around the screen and clicked to trigger actions
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var overlayButton: FloatingActionButton

    // For tracking drag gestures
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isMoving = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "overlay_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY // Restart service if killed by system
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the overlay view when service is destroyed
        overlayView?.let {
            windowManager.removeView(it)
        }
    }

    /**
     * Create and configure the floating overlay button
     */
    private fun setupOverlayView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate the overlay layout with app theme applied
        val themedContext = ContextThemeWrapper(this, R.style.Theme_LanguageApp)
        overlayView = LayoutInflater.from(themedContext).inflate(R.layout.overlay_button, null)
        overlayButton = overlayView!!.findViewById(R.id.overlayButton)

        // Configure window layout parameters
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Position button in top-right corner with 20dp margin
        val margin = (20 * resources.displayMetrics.density).toInt()
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = margin
        layoutParams.y = margin

        // Add the view to window manager
        windowManager.addView(overlayView, layoutParams)

        // Set up touch listener for drag and click handling
        setupTouchListener(layoutParams)
    }

    /**
     * Configure touch listener for dragging and clicking the button
     */
    private fun setupTouchListener(layoutParams: WindowManager.LayoutParams) {
        overlayButton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Store initial positions when touch starts
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false

                    // Set full opacity when touched
                    overlayButton.alpha = 1.0f
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    // Calculate movement
                    val deltaX = initialTouchX - event.rawX
                    val deltaY = event.rawY - initialTouchY

                    // If user moved more than a threshold, consider it a drag
                    if (!isMoving && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                        isMoving = true
                    }

                    if (isMoving) {
                        // Update button position
                        layoutParams.x = (initialX + deltaX).toInt()
                        layoutParams.y = (initialY + deltaY).toInt()
                        windowManager.updateViewLayout(overlayView, layoutParams)
                    }
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_UP -> {
                    // Restore semi-transparency
                    overlayButton.alpha = 0.8f

                    // If not moving, treat as a click
                    if (!isMoving) {
                        onButtonClicked()
                    }
                    return@setOnTouchListener true
                }

                else -> return@setOnTouchListener false
            }
        }
    }

    /**
     * Handle button click - shows toast for now (capture feature coming in Phase 2)
     */
    private fun onButtonClicked() {
        Toast.makeText(this, "Capture feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Manga Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows overlay status"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground service notification
     */
    private fun createNotification(): Notification {
        // Intent to open MainActivity when notification is tapped
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Manga Overlay Active")
            .setContentText("Tap to open app")
            .setSmallIcon(R.drawable.ic_translate)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Non-dismissible while service is running
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
