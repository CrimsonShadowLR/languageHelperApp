package com.mangaoverlay.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Helper class for checking and requesting overlay permissions
 */
object PermissionHelper {

    /**
     * Check if the app has permission to draw overlays
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            // Permission is automatically granted on Android versions below M
            true
        }
    }

    /**
     * Create an intent to open the overlay permission settings
     */
    fun getOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }
}
