package com.veggiebit.sprout.features.enhancement.data.ollama

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object OllamaClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Pings the Ollama instance at GET /api/tags to test connectivity and retrieve installed models.
     */
    suspend fun fetchAvailableModels(baseUrl: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val url = "$cleanUrl/api/tags"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val tagsResponse = json.decodeFromString<OllamaTagsResponse>(body)
                val modelNames = tagsResponse.models.map { it.name }
                Result.success(modelNames)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes generation on the Ollama instance at POST /api/generate with stream=false.
     */
    suspend fun generate(
        baseUrl: String,
        model: String,
        prompt: String,
        systemPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val url = "$cleanUrl/api/generate"

            val requestPayload = OllamaGenerateRequest(
                model = model,
                prompt = prompt,
                system = systemPrompt,
                stream = false
            )

            val requestBody = json.encodeToString(OllamaGenerateRequest.serializer(), requestPayload)
                .toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val genResponse = json.decodeFromString<OllamaGenerateResponse>(body)

                if (genResponse.error != null) {
                    return@withContext Result.failure(Exception(genResponse.error))
                }

                val text = genResponse.response?.trim() ?: ""
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
