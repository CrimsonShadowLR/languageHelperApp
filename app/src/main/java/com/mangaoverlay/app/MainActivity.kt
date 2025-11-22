package com.mangaoverlay.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mangaoverlay.app.databinding.ActivityMainBinding
import com.mangaoverlay.app.utils.PermissionHelper

/**
 * Main activity for controlling the overlay service
 * Handles overlay permission requests and service lifecycle
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Activity result launcher for overlay permission
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()

        // Start service if permission was granted
        if (PermissionHelper.canDrawOverlays(this)) {
            startOverlayService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    /**
     * Set up button click listeners
     */
    private fun setupClickListeners() {
        // Enable Overlay button - request permission
        binding.enableOverlayButton.setOnClickListener {
            requestOverlayPermission()
        }

        // Stop Overlay button - stop the service
        binding.stopOverlayButton.setOnClickListener {
            stopOverlayService()
            updateUI()
        }
    }

    /**
     * Update UI based on current state (permission status and service status)
     */
    private fun updateUI() {
        val hasPermission = PermissionHelper.canDrawOverlays(this)
        val isServiceRunning = isOverlayServiceRunning()

        when {
            !hasPermission -> {
                // Permission not granted
                binding.statusText.text = getString(R.string.status_permission_needed)
                binding.enableOverlayButton.visibility = View.VISIBLE
                binding.stopOverlayButton.visibility = View.GONE
            }
            isServiceRunning -> {
                // Service is running
                binding.statusText.text = getString(R.string.status_overlay_active)
                binding.enableOverlayButton.visibility = View.GONE
                binding.stopOverlayButton.visibility = View.VISIBLE
            }
            else -> {
                // Permission granted but service not running
                binding.statusText.text = getString(R.string.status_permission_granted)
                binding.enableOverlayButton.visibility = View.VISIBLE
                binding.enableOverlayButton.text = getString(R.string.start_overlay)
                binding.stopOverlayButton.visibility = View.GONE
            }
        }
    }

    /**
     * Request overlay permission
     */
    private fun requestOverlayPermission() {
        if (PermissionHelper.canDrawOverlays(this)) {
            // Permission already granted, start service
            startOverlayService()
            updateUI()
        } else {
            // Launch permission settings
            overlayPermissionLauncher.launch(
                PermissionHelper.getOverlayPermissionIntent(this)
            )
        }
    }

    /**
     * Start the overlay service
     */
    private fun startOverlayService() {
        if (PermissionHelper.canDrawOverlays(this)) {
            val intent = Intent(this, OverlayService::class.java)
            startForegroundService(intent)
            updateUI()
        }
    }

    /**
     * Stop the overlay service
     */
    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
    }

    /**
     * Check if the overlay service is currently running
     */
    @Suppress("DEPRECATION")
    private fun isOverlayServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (OverlayService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
