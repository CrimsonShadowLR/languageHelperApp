package com.mangaoverlay.app.api

import com.mangaoverlay.app.BuildConfig

/**
 * Configuration for Gemini API
 */
object ApiConfig {
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"
    const val MODEL = "gemini-2.0-flash-exp"
    const val TIMEOUT_SECONDS = 30L
    const val MAX_RETRIES = 3
    const val MAX_IMAGE_SIZE_MB = 2
    const val MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE_MB * 1024 * 1024

    val API_KEY: String
        get() = BuildConfig.GEMINI_API_KEY

    fun isApiKeyConfigured(): Boolean = API_KEY.isNotBlank()
}
