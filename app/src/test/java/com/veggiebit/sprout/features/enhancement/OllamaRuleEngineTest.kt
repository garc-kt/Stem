package com.veggiebit.sprout.features.enhancement

import com.veggiebit.sprout.features.enhancement.data.engine.OllamaRuleEngine
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.ollama.OllamaGenerateRequest
import com.veggiebit.sprout.features.enhancement.data.ollama.OllamaGenerateResponse
import com.veggiebit.sprout.features.enhancement.data.ollama.OllamaTagsResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OllamaRuleEngineTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSerializationModels() {
        val request = OllamaGenerateRequest(
            model = "llama3.2",
            prompt = "fix this sentence",
            system = "system prompt",
            stream = false
        )
        val encoded = json.encodeToString(OllamaGenerateRequest.serializer(), request)
        assertTrue(encoded.contains("llama3.2"))
        assertTrue(encoded.contains("fix this sentence"))

        val rawTagsJson = """
            {
              "models": [
                {
                  "name": "llama3.2:latest",
                  "model": "llama3.2:latest",
                  "size": 2019393152
                },
                {
                  "name": "mistral:latest",
                  "model": "mistral:latest",
                  "size": 4109851221
                }
              ]
            }
        """.trimIndent()

        val tagsResponse = json.decodeFromString<OllamaTagsResponse>(rawTagsJson)
        assertEquals(2, tagsResponse.models.size)
        assertEquals("llama3.2:latest", tagsResponse.models[0].name)
    }

    @Test
    fun testOllamaEngineFallbackWhenServerOffline() = runBlocking {
        // Points to an intentionally offline port
        val engine = OllamaRuleEngine(baseUrl = "http://127.0.0.1:59999", model = "llama3.2")
        val payload = TextPayload("teh meeting is tommorow")
        val result = engine.transform(payload, TransformPreset.FIX)

        // Should gracefully fall back to local rule engine without crashing
        assertNotNull(result)
        assertTrue(result.transformedText.contains("The"))
        assertTrue(result.transformedText.contains("tomorrow"))
        assertTrue(result.summaryNote?.contains("Local fallback") == true)
    }
}
