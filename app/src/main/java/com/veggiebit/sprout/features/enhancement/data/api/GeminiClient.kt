package com.veggiebit.sprout.features.enhancement.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiContent(val parts: List<GeminiPart>, val role: String? = null)

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction")
    val systemInstruction: GeminiContent? = null
)

@Serializable
data class GeminiCandidate(val content: GeminiContent? = null)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class GeminiError(val message: String? = null, val code: Int? = null)

object GeminiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun generate(
        apiKey: String,
        model: String = "gemini-1.5-flash",
        prompt: String,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is required"))
        }

        val cleanModel = model.ifBlank { "gemini-1.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        val requestBodyData = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        val bodyJson = json.encodeToString(GeminiRequest.serializer(), requestBodyData)
        val body = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gemini API Error (${response.code}): $respString"))
                }

                val parsed = json.decodeFromString(GeminiResponse.serializer(), respString)
                val output = parsed.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!output.isNullOrBlank()) {
                    Result.success(output.trim())
                } else {
                    Result.failure(Exception(parsed.error?.message ?: "Empty response from Gemini"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
