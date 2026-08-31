package com.veggiebit.sprout.features.enhancement

import com.veggiebit.sprout.features.enhancement.data.api.ClaudeMessage
import com.veggiebit.sprout.features.enhancement.data.api.ClaudeRequest
import com.veggiebit.sprout.features.enhancement.data.api.ClaudeResponse
import com.veggiebit.sprout.features.enhancement.data.api.GeminiContent
import com.veggiebit.sprout.features.enhancement.data.api.GeminiPart
import com.veggiebit.sprout.features.enhancement.data.api.GeminiRequest
import com.veggiebit.sprout.features.enhancement.data.api.GeminiResponse
import com.veggiebit.sprout.features.enhancement.data.api.OpenAIChatRequest
import com.veggiebit.sprout.features.enhancement.data.api.OpenAIChatResponse
import com.veggiebit.sprout.features.enhancement.data.api.OpenAIMessage
import com.veggiebit.sprout.features.enhancement.data.engine.ClaudeRuleEngine
import com.veggiebit.sprout.features.enhancement.data.engine.GeminiRuleEngine
import com.veggiebit.sprout.features.enhancement.data.engine.OpenAIRuleEngine
import com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider
import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIEnginesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testGeminiSerialization() {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = "Hello Gemini")))
            )
        )
        val encoded = json.encodeToString(GeminiRequest.serializer(), request)
        assertTrue(encoded.contains("Hello Gemini"))
    }

    @Test
    fun testOpenAISerialization() {
        val request = OpenAIChatRequest(
            model = "gpt-4o-mini",
            messages = listOf(OpenAIMessage(role = "user", content = "Test"))
        )
        val encoded = json.encodeToString(OpenAIChatRequest.serializer(), request)
        assertTrue(encoded.contains("Test"))
    }

    @Test
    fun testClaudeSerialization() {
        val request = ClaudeRequest(
            model = "claude-4.5-sonnet",
            messages = listOf(ClaudeMessage(role = "user", content = "Test"))
        )
        val encoded = json.encodeToString(ClaudeRequest.serializer(), request)
        assertTrue(encoded.contains("claude-4.5-sonnet"))
    }

    @Test
    fun testGeminiEngineGracefulOfflineFallback() = runBlocking {
        val engine = GeminiRuleEngine(apiKey = "dummy_key", model = "gemini-3.7-flash")
        val payload = TextPayload("teh meeting is tommorow in order to make a decision")
        val result = engine.transform(payload, TransformPreset.FIX)

        assertNotNull(result)
        assertTrue(result.transformedText.contains("The"))
        assertTrue(result.transformedText.contains("tomorrow"))
        assertTrue(result.summaryNote?.contains("Local fallback") == true)
    }

    @Test
    fun testOpenAIEngineGracefulOfflineFallback() = runBlocking {
        val engine = OpenAIRuleEngine(baseUrl = "http://127.0.0.1:59999", apiKey = "dummy", model = "gpt-5-mini")
        val payload = TextPayload("teh meeting is tommorow")
        val result = engine.transform(payload, TransformPreset.FIX)

        assertNotNull(result)
        assertTrue(result.transformedText.contains("The"))
        assertTrue(result.transformedText.contains("tomorrow"))
        assertTrue(result.summaryNote?.contains("Local fallback") == true)
    }

    @Test
    fun testClaudeEngineGracefulOfflineFallback() = runBlocking {
        val engine = ClaudeRuleEngine(apiKey = "dummy_key", model = "claude-4.5-sonnet")
        val payload = TextPayload("teh meeting is tommorow")
        val result = engine.transform(payload, TransformPreset.FIX)

        assertNotNull(result)
        assertTrue(result.summaryNote?.contains("Local fallback") == true)
    }

    @Test
    fun testTextEngineProviderRouting() {
        val settingsGemini = SproutUserSettings(engineMode = EngineMode.GEMINI_AI)
        val engineGemini = TextEngineProvider.getEngine(settingsGemini)
        assertTrue(engineGemini is GeminiRuleEngine)

        val settingsOpenAI = SproutUserSettings(engineMode = EngineMode.OPENAI_COMPATIBLE)
        val engineOpenAI = TextEngineProvider.getEngine(settingsOpenAI)
        assertTrue(engineOpenAI is OpenAIRuleEngine)

        val settingsClaude = SproutUserSettings(engineMode = EngineMode.CLAUDE_AI)
        val engineClaude = TextEngineProvider.getEngine(settingsClaude)
        assertTrue(engineClaude is ClaudeRuleEngine)
    }

    @Test
    fun testMasterEnhancementDirectiveInSystemPrompt() {
        val promptWithout = GeminiRuleEngine.getSystemPrompt(TransformPreset.FIX, "")
        assertTrue(promptWithout.contains("master writing editor"))

        val promptWith = GeminiRuleEngine.getSystemPrompt(TransformPreset.FIX, "Always write in British English")
        assertTrue(promptWith.contains("ADDITIONAL MASTER DIRECTIVE: Always write in British English"))
    }

    @Test
    fun testFormatUserPromptFramesTextExplicitly() {
        val formatted = GeminiRuleEngine.formatUserPrompt("meeting tomorrow", TransformPreset.FIX)
        assertTrue(formatted.contains("Original text:"))
        assertTrue(formatted.contains("meeting tomorrow"))
        assertTrue(formatted.contains("Actively polish, improve, and elevate"))
    }
}
