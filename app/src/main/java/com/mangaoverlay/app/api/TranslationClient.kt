package com.mangaoverlay.app.api

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mangaoverlay.app.api.models.*
import com.mangaoverlay.app.utils.ImageProcessor
import com.mangaoverlay.app.utils.TranslationError
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Client for translating manga images using Gemini API
 */
class TranslationClient {

    private val apiService: GeminiApiService
    private val gson = Gson()

    companion object {
        private const val TAG = "TranslationClient"
        private const val TRANSLATION_PROMPT = """
Extract Japanese text with furigana and English translation from this manga panel.
Return ONLY a valid JSON object with this exact structure (no markdown, no code blocks):
{
  "japanese": "the extracted Japanese text",
  "furigana": "the furigana reading in hiragana",
  "english": "the English translation",
  "confidence": 0.95
}

If there is no text in the image, return:
{
  "japanese": "",
  "furigana": "",
  "english": "No text found",
  "confidence": 0.0
}

Important: Return ONLY the JSON object, nothing else.
"""
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
     * Translate image using Gemini API with retry logic
     * @param bitmap The cropped manga panel bitmap
     * @return TranslationResponse with extracted text and translation
     * @throws TranslationError on API errors
     */
    suspend fun translateImage(bitmap: Bitmap): TranslationResponse {
        if (!ApiConfig.isApiKeyConfigured()) {
            throw TranslationError.ApiKeyNotConfigured("Gemini API key not configured. Please add GEMINI_API_KEY to local.properties")
        }

        // Compress and encode image
        Log.d(TAG, "Processing image: ${bitmap.width}x${bitmap.height}")
        val base64Image = ImageProcessor.compressAndEncode(bitmap)
        Log.d(TAG, "Image encoded to base64, size: ${base64Image.length} chars")

        // Retry logic
        var lastException: Exception? = null
        repeat(ApiConfig.MAX_RETRIES) { attempt ->
            try {
                Log.d(TAG, "Attempt ${attempt + 1}/${ApiConfig.MAX_RETRIES}")
                return executeTranslation(base64Image)
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

    /**
     * Execute a single translation request
     */
    private suspend fun executeTranslation(base64Image: String): TranslationResponse {
        // Build request
        val request = GeminiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(
                        RequestPart(text = TRANSLATION_PROMPT),
                        RequestPart(
                            inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            )
                        )
                    )
                )
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

        // Extract text from response
        val textResponse = geminiResponse.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()
            ?.text
            ?: throw TranslationError.InvalidResponse("No text in API response")

        Log.d(TAG, "API response text: $textResponse")

        // Parse JSON from response
        return parseTranslationResponse(textResponse)
    }

    /**
     * Parse translation response from API text
     * Handles both plain JSON and markdown-wrapped JSON
     */
    private fun parseTranslationResponse(text: String): TranslationResponse {
        try {
            // Try parsing directly first
            try {
                return gson.fromJson(text, TranslationResponse::class.java)
            } catch (e: JsonSyntaxException) {
                Log.d(TAG, "Direct parse failed, trying to extract JSON from text")
            }

            // Try extracting JSON from markdown code blocks
            val jsonPattern = """```json\s*(\{.*?\})\s*```""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = jsonPattern.find(text)
            if (match != null) {
                val jsonText = match.groupValues[1]
                return gson.fromJson(jsonText, TranslationResponse::class.java)
            }

            // Try extracting any JSON object
            val objectPattern = """\{.*?\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val objectMatch = objectPattern.find(text)
            if (objectMatch != null) {
                return gson.fromJson(objectMatch.value, TranslationResponse::class.java)
            }

            throw TranslationError.InvalidResponse("Could not parse JSON from response: $text")
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parse error: ${e.message}")
            throw TranslationError.InvalidResponse("Invalid JSON in response: ${e.message}")
        }
    }
}
