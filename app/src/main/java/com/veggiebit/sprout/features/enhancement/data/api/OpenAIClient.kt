package com.veggiebit.sprout.features.enhancement.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class OpenAIMessage(val role: String, val content: String)

@Serializable
data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.3
)

@Serializable
data class OpenAIChoice(val message: OpenAIMessage? = null)

@Serializable
data class OpenAIChatResponse(
    val choices: List<OpenAIChoice>? = null,
    val error: OpenAIError? = null
)

@Serializable
data class OpenAIError(val message: String? = null, val type: String? = null)

object OpenAIClient {

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
        baseUrl: String = "https://api.openai.com/v1",
        apiKey: String,
        model: String = "gpt-4o-mini",
        prompt: String,
        systemPrompt: String,
        temperature: Float = 0.3f
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API key is required"))
        }

        val cleanBaseUrl = baseUrl.ifBlank { "https://api.openai.com/v1" }.trimEnd('/')
        val url = "$cleanBaseUrl/chat/completions"
        val cleanModel = model.ifBlank { "gpt-4o-mini" }

        val requestData = OpenAIChatRequest(
            model = cleanModel,
            messages = listOf(
                OpenAIMessage(role = "system", content = systemPrompt),
                OpenAIMessage(role = "user", content = prompt)
            ),
            temperature = temperature.toDouble()
        )

        val bodyJson = json.encodeToString(OpenAIChatRequest.serializer(), requestData)
        val body = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("OpenAI API Error (${response.code}): $respString"))
                }

                val parsed = json.decodeFromString(OpenAIChatResponse.serializer(), respString)
                val output = parsed.choices?.firstOrNull()?.message?.content
                if (!output.isNullOrBlank()) {
                    Result.success(output.trim())
                } else {
                    Result.failure(Exception(parsed.error?.message ?: "Empty response from OpenAI"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
