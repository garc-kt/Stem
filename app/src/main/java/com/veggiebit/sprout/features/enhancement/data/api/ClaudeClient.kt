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
data class ClaudeOutputConfig(val effort: String = "low")

@Serializable
data class ClaudeRequest(
    val model: String,
    val system: String? = null,
    val messages: List<ClaudeMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int = 1024,
    @SerialName("output_config")
    val outputConfig: ClaudeOutputConfig = ClaudeOutputConfig()
)

@Serializable
data class ClaudeResponse(
    val content: List<ClaudeContentBlock>? = null,
    @SerialName("stop_reason")
    val stopReason: String? = null,
    val error: ClaudeError? = null
)

@Serializable
data class ClaudeError(val message: String? = null, val type: String? = null)

object ClaudeClient {

    const val DEFAULT_MODEL = "claude-haiku-4-5"

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
        model: String = DEFAULT_MODEL,
        prompt: String,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Claude API key is required"))
        }

        val cleanModel = model.ifBlank { DEFAULT_MODEL }
        val url = "https://api.anthropic.com/v1/messages"

        // Scale the output ceiling with input size instead of a flat 1024, which truncated
        // longer rewrites; keep a sane floor/ceiling for the inline-overlay use case.
        val maxTokens = (prompt.length / 2).coerceIn(512, 8192)

        val requestData = ClaudeRequest(
            model = cleanModel,
            system = systemPrompt,
            messages = listOf(
                ClaudeMessage(role = "user", content = prompt)
            ),
            maxTokens = maxTokens,
            // GA, no beta header required. Models in the Opus/Sonnet 5 family think by default;
            // "low" effort keeps latency reasonable on this inline-overlay path.
            outputConfig = ClaudeOutputConfig(effort = "low")
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
                    // Prefer the structured error message when the body parses; fall back to
                    // the raw response only if it doesn't, instead of always dumping raw JSON.
                    val friendlyMessage = runCatching {
                        json.decodeFromString(ClaudeResponse.serializer(), respString).error?.message
                    }.getOrNull()
                    return@withContext Result.failure(
                        Exception("Claude API Error (${response.code}): ${friendlyMessage ?: respString}")
                    )
                }

                val parsed = json.decodeFromString(ClaudeResponse.serializer(), respString)

                // stop_reason == "refusal" is a normal HTTP 200 — Claude declined the request
                // rather than erroring. Surface it as a failure so the caller's fallback path
                // (LocalRuleEngine) kicks in instead of injecting an empty/partial response.
                if (parsed.stopReason == "refusal") {
                    return@withContext Result.failure(Exception("Claude declined this request"))
                }

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
