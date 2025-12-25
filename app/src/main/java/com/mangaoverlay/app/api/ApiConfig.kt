package com.mangaoverlay.app.api

import com.mangaoverlay.app.BuildConfig

/**
 * Configuration for Gemini API
 */
object ApiConfig {
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"
    const val MODEL = "gemini-3-pro-image-preview"  // Using Pro model as default for better image editing
    const val TIMEOUT_SECONDS = 120L  // Increased timeout for image generation
    const val MAX_RETRIES = 1

    // Image size limits - reduced to 50-100KB for faster uploads and lower API costs
    const val MAX_IMAGE_SIZE_KB = 100
    const val TARGET_IMAGE_SIZE_KB = 75
    const val MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE_KB * 1024 // 100KB
    const val TARGET_IMAGE_SIZE_BYTES = TARGET_IMAGE_SIZE_KB * 1024 // 75KB

    // Request throttling settings
    const val MAX_CONCURRENT_REQUESTS = 2
    const val MIN_REQUEST_INTERVAL_MS = 500L // Minimum 500ms between requests

    val API_KEY: String
        get() = BuildConfig.GEMINI_API_KEY

    fun isApiKeyConfigured(): Boolean = API_KEY.isNotBlank()
}
