package com.stem.engine

import com.stem.core.models.OllamaGenerateRequest
import com.stem.core.models.OllamaGenerateResponse
import com.stem.core.models.OllamaOptions
import com.stem.core.models.OllamaTagsResponse
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import com.stem.core.models.TransformResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
import java.util.concurrent.TimeUnit



// --- Gemini DTOs ---
@Serializable data class GeminiPart(val text: String)
@Serializable data class GeminiContent(val parts: List<GeminiPart>, val role: String? = null)
@Serializable data class GeminiGenerationConfig(val temperature: Float? = null, @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null)
@Serializable data class GeminiRequest(val contents: List<GeminiContent>, @SerialName("system_instruction") val systemInstruction: GeminiContent? = null, @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null)
@Serializable data class GeminiCandidate(val content: GeminiContent? = null)
@Serializable data class GeminiResponse(val candidates: List<GeminiCandidate>? = null, val error: GeminiError? = null)
@Serializable data class GeminiError(val message: String? = null, val code: Int? = null)

// --- Claude DTOs ---
@Serializable data class ClaudeContentBlock(val type: String = "text", val text: String)
@Serializable data class ClaudeMessage(val role: String, val content: String)
@Serializable data class ClaudeRequest(val model: String, val system: String? = null, val messages: List<ClaudeMessage>, @SerialName("max_tokens") val maxTokens: Int = 1024)
@Serializable data class ClaudeResponse(val content: List<ClaudeContentBlock>? = null, @SerialName("stop_reason") val stopReason: String? = null, val error: ClaudeError? = null)
@Serializable data class ClaudeError(val message: String? = null, val type: String? = null)

// --- OpenAI DTOs ---
@Serializable data class OpenAIMessage(val role: String, val content: String)
@Serializable data class OpenAIChatRequest(val model: String, val messages: List<OpenAIMessage>, val temperature: Double = 0.3)
@Serializable data class OpenAIChoice(val message: OpenAIMessage? = null)
@Serializable data class OpenAIChatResponse(val choices: List<OpenAIChoice>? = null, val error: OpenAIError? = null)
@Serializable data class OpenAIError(val message: String? = null, val type: String? = null)

// --- HTTP Clients ---

object HttpClientFactory {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

object GeminiClient {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    private val httpClient get() = HttpClientFactory.client

    suspend fun generate(apiKey: String, model: String = "gemini-3.7-flash", prompt: String, systemPrompt: String, temperature: Float = 0.3f): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Gemini API key is required"))
        val cleanModel = model.ifBlank { "gemini-3.7-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
        val requestBodyData = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = temperature, maxOutputTokens = 1024)
        )
        val body = json.encodeToString(GeminiRequest.serializer(), requestBodyData).toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(body).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Gemini API Error (${response.code}): $respString"))
                val parsed = json.decodeFromString(GeminiResponse.serializer(), respString)
                val output = parsed.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!output.isNullOrBlank()) Result.success(output.trim()) else Result.failure(Exception(parsed.error?.message ?: "Empty response from Gemini"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}

object ClaudeClient {
    const val DEFAULT_MODEL = "claude-3-7-sonnet-latest"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    private val httpClient get() = HttpClientFactory.client

    suspend fun generate(apiKey: String, model: String = DEFAULT_MODEL, prompt: String, systemPrompt: String, temperature: Float = 0.3f): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Claude API key is required"))
        val cleanModel = model.ifBlank { DEFAULT_MODEL }
        val url = "https://api.anthropic.com/v1/messages"
        val maxTokens = (prompt.length / 2).coerceIn(512, 8192)
        val requestData = ClaudeRequest(model = cleanModel, system = systemPrompt, messages = listOf(ClaudeMessage(role = "user", content = prompt)), maxTokens = maxTokens)
        val body = json.encodeToString(ClaudeRequest.serializer(), requestData).toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).addHeader("x-api-key", apiKey).addHeader("anthropic-version", "2023-06-01").post(body).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val friendlyMessage = runCatching { json.decodeFromString(ClaudeResponse.serializer(), respString).error?.message }.getOrNull()
                    return@withContext Result.failure(Exception("Claude API Error (${response.code}): ${friendlyMessage ?: respString}"))
                }
                val parsed = json.decodeFromString(ClaudeResponse.serializer(), respString)
                if (parsed.stopReason == "refusal") return@withContext Result.failure(Exception("Claude declined this request"))
                val output = parsed.content?.firstOrNull()?.text
                if (!output.isNullOrBlank()) Result.success(output.trim()) else Result.failure(Exception(parsed.error?.message ?: "Empty response from Claude"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}

object OpenAIClient {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    private val httpClient get() = HttpClientFactory.client

    suspend fun generate(baseUrl: String = "https://api.openai.com/v1", apiKey: String, model: String = "gpt-4o-mini", prompt: String, systemPrompt: String, temperature: Float = 0.3f): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("API key is required"))
        val cleanBaseUrl = baseUrl.ifBlank { "https://api.openai.com/v1" }.trimEnd('/')
        val url = "$cleanBaseUrl/chat/completions"
        val cleanModel = model.ifBlank { "gpt-4o-mini" }
        val requestData = OpenAIChatRequest(model = cleanModel, messages = listOf(OpenAIMessage(role = "system", content = systemPrompt), OpenAIMessage(role = "user", content = prompt)), temperature = temperature.toDouble())
        val body = json.encodeToString(OpenAIChatRequest.serializer(), requestData).toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $apiKey").post(body).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val respString = response.body?.string() ?: ""
                if (!response.isSuccessful) return@withContext Result.failure(Exception("OpenAI API Error (${response.code}): $respString"))
                val parsed = json.decodeFromString(OpenAIChatResponse.serializer(), respString)
                val output = parsed.choices?.firstOrNull()?.message?.content
                if (!output.isNullOrBlank()) Result.success(output.trim()) else Result.failure(Exception(parsed.error?.message ?: "Empty response from OpenAI"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}

object OllamaClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val httpClient get() = HttpClientFactory.client
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun isCleartextAllowed(url: String): Boolean {
        val httpUrl = url.toHttpUrlOrNull() ?: return false
        if (httpUrl.scheme == "https") return true
        if (httpUrl.scheme != "http") return false
        val host = httpUrl.host
        if (host.equals("localhost", ignoreCase = true) || host.endsWith(".local", ignoreCase = true)) return true
        return try {
            val address = InetAddress.getByName(host)
            address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress
        } catch (_: Exception) { false }
    }

    suspend fun fetchAvailableModels(baseUrl: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val url = "$cleanUrl/api/tags"
            if (!isCleartextAllowed(url)) return@withContext Result.failure(Exception("Ollama host must be on your local network (localhost/LAN IP/.local), or use https://"))
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val tagsResponse = json.decodeFromString<OllamaTagsResponse>(body)
                Result.success(tagsResponse.models.map { it.name })
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun generate(baseUrl: String, model: String, prompt: String, systemPrompt: String? = null, temperature: Float = 0.3f): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val url = "$cleanUrl/api/generate"
            if (!isCleartextAllowed(url)) return@withContext Result.failure(Exception("Ollama host must be on your local network (localhost/LAN IP/.local), or use https://"))
            val requestPayload = OllamaGenerateRequest(model = model, prompt = prompt, system = systemPrompt, stream = false, options = OllamaOptions(temperature = temperature))
            val requestBody = json.encodeToString(OllamaGenerateRequest.serializer(), requestPayload).toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(requestBody).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val genResponse = json.decodeFromString<OllamaGenerateResponse>(body)
                if (genResponse.error != null) return@withContext Result.failure(Exception(genResponse.error))
                Result.success(genResponse.response?.trim() ?: "")
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}

// --- Unified AI Rule Engine ---

open class AiRuleEngine(
    val engineName: String,
    val model: String,
    val customInstruction: String = "",
    val temperature: Float = 0.3f,
    private val generator: suspend (prompt: String, systemPrompt: String) -> Result<String>
) : TextEngine {

    companion object {
        fun getSystemPrompt(preset: TransformPreset, customInstruction: String = ""): String {
            val masterDirective = if (customInstruction.isNotBlank() && preset != TransformPreset.CUSTOM) {
                "\nADDITIONAL MASTER DIRECTIVE: ${customInstruction.trim()}. Ensure the transformed text strictly satisfies this directive."
            } else ""

            val basePrompt = when (preset) {
                TransformPreset.FIX -> "You are a master writing editor and proofreader. Actively elevate and polish the text: fix all grammar, spelling, typos, awkward phrasing, and punctuation while enhancing vocabulary, rhythm, and clarity. Maintain the original core meaning and voice. Return ONLY the enhanced text without quotes, preambles, or explanations."
                TransformPreset.CONCISE -> "You are an expert concise editor. Aggressively streamline the text to be sharp, crystal-clear, and concise: eliminate fluff, redundancy, and passive constructions while maximizing clarity and punch. Return ONLY the rewritten text without quotes."
                TransformPreset.PROFESSIONAL -> "You are an executive communications strategist. Transform the text into articulate, polished, high-status, professional business language with confident authority and diplomatic courtesy. Return ONLY the rewritten text without quotes."
                TransformPreset.PUNCHY -> "You are a high-impact copywriter. Rewrite the text to be active, energetic, punchy, and compelling with strong action verbs and crisp cadence. Return ONLY the rewritten text without quotes."
                TransformPreset.FRIENDLY -> "You are a warm, charismatic communicator. Rewrite the text in a delightful, warm, empathetic, and conversational tone while ensuring effortless readability. Return ONLY the rewritten text without quotes."
                TransformPreset.SUMMARIZE -> "You are a precision summarizer. Distill the text to its critical insights in as few sentences as possible without losing key meaning. Return ONLY the summary without quotes."
                TransformPreset.BULLETIZE -> "You are an expert information designer. Transform the prose into a crisp, scannable bullet point list (one distinct thought per line, prefixed with \"• \"). Return ONLY the bullet points without quotes or introductory text."
                TransformPreset.EXPAND -> "You are an eloquent writer. Elaborate and flesh out the ideas with rich supporting context, vivid phrasing, and smooth transitions while preserving intent. Return ONLY the expanded text without quotes."
                TransformPreset.CUSTOM -> {
                    val instruction = customInstruction.trim().ifBlank { "Actively enhance the text with polished grammar, superior vocabulary, and natural flow." }
                    "You are an expert AI writing assistant. Follow this instruction exactly: $instruction. Return ONLY the rewritten text without quotes or explanations."
                }
            }
            return basePrompt + masterDirective
        }

        fun formatUserPrompt(original: String, preset: TransformPreset, customInstruction: String = ""): String {
            val action = when (preset) {
                TransformPreset.FIX -> "Actively polish, improve, and elevate the text below. Fix all grammar, typos, awkward phrasing, and rhythm while enriching vocabulary. Make sure the output is enhanced and not left identical."
                TransformPreset.CONCISE -> "Rewrite the text below to be significantly more concise, direct, and clear. Eliminate all wordiness, filler, and redundancy while preserving full meaning."
                TransformPreset.PROFESSIONAL -> "Transform the text below into articulate, polished, high-status executive business phrasing with confidence and diplomatic authority."
                TransformPreset.PUNCHY -> "Rewrite the text below with high energy, active verbs, compelling rhythm, and strong cadence."
                TransformPreset.FRIENDLY -> "Rewrite the text below in a delightful, warm, empathetic, and approachable conversational tone with natural flow."
                TransformPreset.SUMMARIZE -> "Distill the text below into a clear, high-impact summary capturing the core takeaways in as few words as possible."
                TransformPreset.BULLETIZE -> "Convert the text below into a scannable bullet point list (one key point per line with • prefix)."
                TransformPreset.EXPAND -> "Elaborate and enrich the text below with vivid detail, smooth transitions, and depth while preserving intent."
                TransformPreset.CUSTOM -> {
                    val inst = customInstruction.trim().ifBlank { "Actively enhance, polish, and elevate the text" }
                    "Apply this instruction to the text: $inst"
                }
            }
            return "$action\n\nOriginal text:\n\"\"\"\n$original\n\"\"\"\n\nEnhanced rewritten version (output ONLY the enhanced text, with no quotes, markdown backticks, or preamble):"
        }
    }

    val engineSignature: String = "$engineName:$model:$temperature:${customInstruction.hashCode()}"

    override suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult = withContext(Dispatchers.IO) {
        val original = payload.text
        if (original.isBlank()) {
            return@withContext TransformResult(original, original, preset, emptyList())
        }

        val cached = TransformCache.get(original, preset, engineSignature)
        if (cached != null) {
            return@withContext cached
        }

        val systemPrompt = getSystemPrompt(preset, customInstruction)
        val formattedPrompt = formatUserPrompt(original, preset, customInstruction)
        val result = generator(formattedPrompt, systemPrompt)

        result.fold(
            onSuccess = { rawOutput ->
                var cleaned = rawOutput.trim()
                if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length > 1) {
                    cleaned = cleaned.substring(1, cleaned.length - 1).trim()
                }
                if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
                    cleaned = cleaned.removeSurrounding("```").trim()
                }

                val diff = DiffCalculator.calculateDiff(original, cleaned)
                val transformRes = TransformResult(
                    originalText = original,
                    transformedText = cleaned,
                    preset = preset,
                    diffTokens = diff,
                    summaryNote = "$engineName ($model) • ${preset.title}"
                )
                TransformCache.put(original, preset, engineSignature, transformRes)
                transformRes
            },
            onFailure = { _ ->
                val fallback = LocalRuleEngine.transform(payload, preset)
                fallback.copy(
                    summaryNote = "${fallback.summaryNote ?: "Polished"} (Local fallback)"
                )
            }
        )
    }
}

class GeminiRuleEngine(
    apiKey: String,
    model: String,
    customInstruction: String = "",
    temperature: Float = 0.3f
) : AiRuleEngine(
    engineName = "Gemini",
    model = model,
    customInstruction = customInstruction,
    temperature = temperature,
    generator = { prompt, sys -> GeminiClient.generate(apiKey, model, prompt, sys, temperature) }
)

class ClaudeRuleEngine(
    apiKey: String,
    model: String,
    customInstruction: String = "",
    temperature: Float = 0.3f
) : AiRuleEngine(
    engineName = "Claude",
    model = model,
    customInstruction = customInstruction,
    temperature = temperature,
    generator = { prompt, sys -> ClaudeClient.generate(apiKey, model, prompt, sys, temperature) }
)

class OpenAIRuleEngine(
    baseUrl: String = "https://api.openai.com/v1",
    apiKey: String,
    model: String = "gpt-4o-mini",
    customInstruction: String = "",
    temperature: Float = 0.3f
) : AiRuleEngine(
    engineName = "OpenAI",
    model = model,
    customInstruction = customInstruction,
    temperature = temperature,
    generator = { prompt, sys -> OpenAIClient.generate(baseUrl, apiKey, model, prompt, sys, temperature) }
)

class OllamaRuleEngine(
    baseUrl: String,
    model: String,
    customInstruction: String = "",
    temperature: Float = 0.3f
) : AiRuleEngine(
    engineName = "Ollama",
    model = model,
    customInstruction = customInstruction,
    temperature = temperature,
    generator = { prompt, sys -> OllamaClient.generate(baseUrl, model, prompt, sys, temperature) }
)
