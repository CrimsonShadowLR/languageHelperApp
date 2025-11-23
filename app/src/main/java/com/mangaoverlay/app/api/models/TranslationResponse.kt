package com.mangaoverlay.app.api.models

import com.google.gson.annotations.SerializedName

/**
 * Response from translation API
 */
data class TranslationResponse(
    @SerializedName("japanese")
    val japanese: String,

    @SerializedName("furigana")
    val furigana: String,

    @SerializedName("english")
    val english: String,

    @SerializedName("confidence")
    val confidence: Float = 0f
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
    val text: String
)

/**
 * Request structure for Gemini API
 */
data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<RequestContent>
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
