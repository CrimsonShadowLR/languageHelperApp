package com.mangaoverlay.app.utils

/**
 * Custom exception types for translation errors
 */
sealed class TranslationError(message: String) : Exception(message) {

    /**
     * Network-related errors
     */
    class NetworkError(message: String) : TranslationError(message)

    /**
     * API key not configured or invalid
     */
    class ApiKeyNotConfigured(message: String) : TranslationError(message)

    /**
     * Request timeout
     */
    class Timeout(message: String) : TranslationError(message)

    /**
     * Invalid or unparseable API response
     */
    class InvalidResponse(message: String) : TranslationError(message)

    /**
     * Rate limit exceeded
     */
    class RateLimitExceeded(message: String) : TranslationError(message)

    /**
     * Unknown error
     */
    class Unknown(message: String) : TranslationError(message)

    companion object {
        /**
         * Get user-friendly error message from exception
         */
        fun getErrorMessage(error: Throwable): String {
            return when (error) {
                is ApiKeyNotConfigured -> "API key not configured. Please add your Gemini API key to local.properties"
                is NetworkError -> "Network error. Please check your internet connection."
                is Timeout -> "Request timed out. Please try again."
                is InvalidResponse -> "Received invalid response from server. Please try again."
                is RateLimitExceeded -> "Too many requests. Please wait a moment and try again."
                is Unknown -> "An unknown error occurred: ${error.message}"
                else -> "An error occurred: ${error.message ?: "Unknown error"}"
            }
        }

        /**
         * Check if error is retryable
         */
        fun isRetryable(error: Throwable): Boolean {
            return when (error) {
                is NetworkError, is Timeout, is RateLimitExceeded, is Unknown -> true
                is ApiKeyNotConfigured, is InvalidResponse -> false
                else -> false
            }
        }
    }
}
