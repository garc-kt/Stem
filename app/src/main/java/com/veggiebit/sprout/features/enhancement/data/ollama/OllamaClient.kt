package com.veggiebit.sprout.features.enhancement.data.ollama

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
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
     * The app permits cleartext HTTP at the OS level (see network_security_config.xml) because
     * Android's NetworkSecurityConfig can't express "any private-LAN IP" declaratively. This is
     * the actual enforcement point: a cleartext request is only ever sent to localhost, a .local
     * mDNS host, or a loopback/site-local/link-local address — never a public host. Ollama is the
     * only engine that ever builds an http:// URL; Gemini/OpenAI/Claude are HTTPS-only already.
     */
    private fun isCleartextAllowed(url: String): Boolean {
        val httpUrl = url.toHttpUrlOrNull() ?: return false
        if (httpUrl.scheme == "https") return true
        if (httpUrl.scheme != "http") return false

        val host = httpUrl.host
        if (host.equals("localhost", ignoreCase = true) || host.endsWith(".local", ignoreCase = true)) {
            return true
        }

        return try {
            val address = InetAddress.getByName(host)
            address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Pings the Ollama instance at GET /api/tags to test connectivity and retrieve installed models.
     */
    suspend fun fetchAvailableModels(baseUrl: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val url = "$cleanUrl/api/tags"

            if (!isCleartextAllowed(url)) {
                return@withContext Result.failure(Exception("Ollama host must be on your local network (localhost/LAN IP/.local), or use https://"))
            }

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
        systemPrompt: String? = null,
        temperature: Float = 0.3f
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = baseUrl.trim().removeSuffix("/")
            val url = "$cleanUrl/api/generate"

            if (!isCleartextAllowed(url)) {
                return@withContext Result.failure(Exception("Ollama host must be on your local network (localhost/LAN IP/.local), or use https://"))
            }

            val requestPayload = OllamaGenerateRequest(
                model = model,
                prompt = prompt,
                system = systemPrompt,
                stream = false,
                options = OllamaOptions(temperature = temperature)
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
