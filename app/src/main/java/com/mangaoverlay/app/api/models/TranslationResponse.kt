package com.mangaoverlay.app.api.models

import android.graphics.Bitmap
import com.google.gson.annotations.SerializedName

/**
 * Response from translation API
 * Now includes the edited image bitmap with Spanish text overlaid
 */
data class TranslationResponse(
    @SerializedName("japanese")
    val japanese: String,

    @SerializedName("furigana")
    val furigana: String,

    @SerializedName("english")
    val english: String,

    @SerializedName("confidence")
    val confidence: Float = 0f,

    // The edited image with translated text overlaid (from Gemini Pro image editing)
    val editedImage: Bitmap? = null,

    // Optional text response from the API
    val apiTextResponse: String? = null
)

/**
 * Gemini API response structure
 */
data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<Candidate>
)

data class Candidate(
    @SerializedName("content")
    val content: Content
)

data class Content(
    @SerializedName("parts")
    val parts: List<Part>
)

data class Part(
    @SerializedName("text")
    val text: String? = null,

    @SerializedName("inline_data")
    val inlineData: InlineData? = null,

    @SerializedName("inlineData")  // Gemini sometimes uses camelCase
    val inlineDataCamelCase: InlineData? = null
)

/**
 * Request structure for Gemini API
 */
data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<RequestContent>,

    @SerializedName("generationConfig")
    val generationConfig: GenerationConfig? = null
)

/**
 * Generation configuration for image/text modalities
 */
data class GenerationConfig(
    @SerializedName("response_modalities")
    val responseModalities: List<String>
)

data class RequestContent(
    @SerializedName("parts")
    val parts: List<RequestPart>
)

data class RequestPart(
    @SerializedName("text")
    val text: String? = null,

    @SerializedName("inline_data")
    val inlineData: InlineData? = null
)

data class InlineData(
    @SerializedName("mime_type")
    val mimeType: String,

    @SerializedName("data")
    val data: String
)
