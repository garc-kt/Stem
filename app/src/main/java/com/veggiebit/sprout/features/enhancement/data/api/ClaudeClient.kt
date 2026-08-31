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
data class ClaudeContentBlock(val type: String = "text", val text: String)

@Serializable
data class ClaudeMessage(val role: String, val content: String)

@Serializable
data class ClaudeRequest(
    val model: String,
    val system: String? = null,
    val messages: List<ClaudeMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int = 1024
)

@Serializable
data class ClaudeResponse(
    val content: List<ClaudeContentBlock>? = null,
    val error: ClaudeError? = null
)

@Serializable
data class ClaudeError(val message: String? = null, val type: String? = null)

object ClaudeClient {

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
        model: String = "claude-3-7-sonnet-20250219",
        prompt: String,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Claude API key is required"))
        }

        val cleanModel = model.ifBlank { "claude-3-7-sonnet-20250219" }
        val url = "https://api.anthropic.com/v1/messages"

        val requestData = ClaudeRequest(
            model = cleanModel,
            system = systemPrompt,
            messages = listOf(
                ClaudeMessage(role = "user", content = prompt)
            ),
            maxTokens = 1024
        )

        val bodyJson = json.encodeToString(ClaudeRequest.serializer(), requestData)
        val body = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Claude API Error (${response.code}): $respString"))
                }

                val parsed = json.decodeFromString(ClaudeResponse.serializer(), respString)
                val output = parsed.content?.firstOrNull()?.text
                if (!output.isNullOrBlank()) {
                    Result.success(output.trim())
                } else {
                    Result.failure(Exception(parsed.error?.message ?: "Empty response from Claude"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
