package com.mangaoverlay.app.api

import com.mangaoverlay.app.api.models.GeminiRequest
import com.mangaoverlay.app.api.models.GeminiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit service interface for Gemini API
 */
interface GeminiApiService {

    @POST("models/{model}:generateContent")
    suspend fun generateContent(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
