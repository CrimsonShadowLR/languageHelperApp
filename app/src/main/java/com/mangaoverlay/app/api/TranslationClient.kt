package com.mangaoverlay.app.api

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mangaoverlay.app.api.models.*
import com.mangaoverlay.app.utils.ImageProcessor
import com.mangaoverlay.app.utils.TranslationError
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Client for translating manga images using Gemini API
 */
class TranslationClient {

    private val apiService: GeminiApiService
    private val gson = Gson()

    companion object {
        private const val TAG = "TranslationClient"
        // Hardcoded prompt for manga translation - matches pythonPOC functionality
        private const val TRANSLATION_PROMPT = """
CRITICAL: You must GENERATE and RETURN an EDITED IMAGE. Do NOT just provide text translation.

Task: Image Editing - Text Replacement
Using the provided manga/comic image, perform the following image editing operation:

1. Identify all Japanese text elements (speech bubbles, dialogue, sound effects, text overlays)
2. Translate each text element to Spanish
3. EDIT THE IMAGE by overlaying the Spanish translation on top of or in place of the original Japanese text
4. Use similar font styling (bold, size, color) that matches the original comic style
5. Position the translated text in the same locations as the original
6. Preserve all artwork, characters, backgrounds, and visual elements exactly as they are
7. OUTPUT: Return the MODIFIED IMAGE with Spanish text overlaid

DO NOT respond with only text. You MUST return an edited image with the text replaced.
"""

        // Request throttling mechanism
        private val requestSemaphore = Semaphore(ApiConfig.MAX_CONCURRENT_REQUESTS)
        private val lastRequestTime = AtomicLong(0L)
    }

    init {
        // Create OkHttp client with logging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Enforce rate limiting between requests
     */
    private suspend fun enforceRateLimit() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime.get()

        if (timeSinceLastRequest < ApiConfig.MIN_REQUEST_INTERVAL_MS) {
            val delayMs = ApiConfig.MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest
            Log.d(TAG, "Rate limiting: waiting ${delayMs}ms before next request")
            delay(delayMs)
        }

        lastRequestTime.set(System.currentTimeMillis())
    }

    /**
     * Translate image using Gemini API with retry logic and request throttling
     * @param bitmap The cropped manga panel bitmap
     * @return TranslationResponse with extracted text and translation
     * @throws TranslationError on API errors
     */
    suspend fun translateImage(bitmap: Bitmap): TranslationResponse {
        if (!ApiConfig.isApiKeyConfigured()) {
            throw TranslationError.ApiKeyNotConfigured("Gemini API key not configured. Please add GEMINI_API_KEY to local.properties")
        }

        // Use semaphore to limit concurrent requests
        return requestSemaphore.withPermit {
            Log.d(TAG, "Acquired request permit (${ApiConfig.MAX_CONCURRENT_REQUESTS - requestSemaphore.availablePermits}/${ApiConfig.MAX_CONCURRENT_REQUESTS} in use)")

            // Enforce minimum interval between requests
            enforceRateLimit()

            // Compress and encode image
            Log.d(TAG, "Processing image: ${bitmap.width}x${bitmap.height}")
            val base64Image = ImageProcessor.compressAndEncode(bitmap)
            Log.d(TAG, "Image encoded to base64, size: ${base64Image.length} chars")

            // Retry logic
            var lastException: Exception? = null
            repeat(ApiConfig.MAX_RETRIES) { attempt ->
                try {
                    Log.d(TAG, "Attempt ${attempt + 1}/${ApiConfig.MAX_RETRIES}")
                    return@withPermit executeTranslation(base64Image)
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")

                    // Don't retry on certain errors
                    if (e is TranslationError.ApiKeyNotConfigured ||
                        e is TranslationError.InvalidResponse) {
                        throw e
                    }

                    // Exponential backoff before retry
                    if (attempt < ApiConfig.MAX_RETRIES - 1) {
                        val delayMs = (1000L * (attempt + 1) * (attempt + 1)) // 1s, 4s, 9s
                        Log.d(TAG, "Waiting ${delayMs}ms before retry...")
                        delay(delayMs)
                    }
                }
            }

            // All retries failed
            throw lastException ?: TranslationError.Unknown("Translation failed after ${ApiConfig.MAX_RETRIES} attempts")
        }
    }

    /**
     * Execute a single translation request
     */
    private suspend fun executeTranslation(base64Image: String): TranslationResponse {
        // Build request with generationConfig for image + text response
        val request = GeminiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(
                        RequestPart(
                            inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            )
                        ),
                        RequestPart(text = TRANSLATION_PROMPT)
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        // Make API call
        val response = apiService.generateContent(
            model = ApiConfig.MODEL,
            apiKey = ApiConfig.API_KEY,
            request = request
        )

        // Check response
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "API error: ${response.code()} - $errorBody")
            throw when (response.code()) {
                401, 403 -> TranslationError.ApiKeyNotConfigured("Invalid API key")
                429 -> TranslationError.RateLimitExceeded("Rate limit exceeded")
                408, 504 -> TranslationError.Timeout("Request timed out")
                else -> TranslationError.NetworkError("API error: ${response.code()}")
            }
        }

        val geminiResponse = response.body()
            ?: throw TranslationError.InvalidResponse("Empty response from API")

        // Extract content from response parts
        val parts = geminiResponse.candidates.firstOrNull()
            ?.content?.parts
            ?: throw TranslationError.InvalidResponse("No parts in API response")

        Log.d(TAG, "Response parts count: ${parts.size}")

        // Look for image data in response parts
        var imageData: String? = null
        var textResponse: String? = null

        for ((index, part) in parts.withIndex()) {
            Log.d(TAG, "Part $index - has text: ${part.text != null}, has inlineData: ${part.inlineData != null}, has inlineDataCamelCase: ${part.inlineDataCamelCase != null}")

            // Extract image data (check both snake_case and camelCase)
            val inline = part.inlineData ?: part.inlineDataCamelCase
            if (inline != null) {
                imageData = inline.data
                Log.d(TAG, "Found image data in part $index (size: ${imageData?.length ?: 0} chars)")
            }

            // Extract text response
            if (part.text != null) {
                textResponse = part.text
                Log.d(TAG, "Found text in part $index: ${textResponse?.take(100)}")
            }
        }

        if (imageData == null) {
            // API returned text but no image - this is an error for our use case
            val errorMsg = "API returned no image. Text response: ${textResponse?.take(200)}"
            Log.e(TAG, errorMsg)
            throw TranslationError.InvalidResponse("API did not return an edited image. Try again or adjust the prompt.")
        }

        Log.d(TAG, "Successfully extracted image data from API response")

        // Decode base64 image to bitmap
        return decodeImageResponse(imageData, textResponse)
    }

    /**
     * Decode base64 image data from API response to TranslationResponse
     * The API returns an edited image with Spanish text overlaid on the manga
     */
    private fun decodeImageResponse(imageData: String, textResponse: String?): TranslationResponse {
        try {
            // Decode base64 image data to Bitmap
            val decodedBytes = android.util.Base64.decode(imageData, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                ?: throw TranslationError.InvalidResponse("Failed to decode image bitmap from API response")

            Log.d(TAG, "Successfully decoded edited image: ${bitmap.width}x${bitmap.height}")

            // Return response with the edited image
            // The editedImage field contains the manga with Spanish text overlaid
            return TranslationResponse(
                japanese = "",
                furigana = "",
                english = "Translated to Spanish (see edited image)",
                confidence = 1.0f,
                editedImage = bitmap,
                apiTextResponse = textResponse
            )
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid base64 data: ${e.message}")
            throw TranslationError.InvalidResponse("Invalid image data from API: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding image response: ${e.message}")
            throw TranslationError.InvalidResponse("Failed to decode image: ${e.message}")
        }
    }
}
