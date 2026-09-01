package com.stem.engine

import com.stem.engine.ClaudeMessage
import com.stem.engine.ClaudeRequest
import com.stem.engine.GeminiContent
import com.stem.engine.GeminiPart
import com.stem.engine.GeminiRequest
import com.stem.engine.OpenAIChatRequest
import com.stem.engine.OpenAIMessage
import com.stem.engine.ClaudeRuleEngine
import com.stem.engine.GeminiRuleEngine
import com.stem.engine.OpenAIRuleEngine
import com.stem.engine.TextEngineProvider
import com.stem.core.models.EngineMode
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import com.stem.core.models.StemUserSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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
        val settingsGemini = StemUserSettings(engineMode = EngineMode.GEMINI_AI)
        val engineGemini = TextEngineProvider.getEngine(settingsGemini)
        assertTrue(engineGemini is GeminiRuleEngine)

        val settingsOpenAI = StemUserSettings(engineMode = EngineMode.OPENAI_COMPATIBLE)
        val engineOpenAI = TextEngineProvider.getEngine(settingsOpenAI)
        assertTrue(engineOpenAI is OpenAIRuleEngine)

        val settingsClaude = StemUserSettings(engineMode = EngineMode.CLAUDE_AI)
        val engineClaude = TextEngineProvider.getEngine(settingsClaude)
        assertTrue(engineClaude is ClaudeRuleEngine)
    }

    @Test
    fun testMasterEnhancementDirectiveInSystemPrompt() {
        val promptWithout = AiRuleEngine.getSystemPrompt(TransformPreset.FIX, "")
        assertTrue(promptWithout.contains("master writing editor"))

        val promptWith = AiRuleEngine.getSystemPrompt(TransformPreset.FIX, "Always write in British English")
        assertTrue(promptWith.contains("ADDITIONAL MASTER DIRECTIVE: Always write in British English"))
    }

    @Test
    fun testFormatUserPromptFramesTextExplicitly() {
        val formatted = AiRuleEngine.formatUserPrompt("meeting tomorrow", TransformPreset.FIX)
        assertTrue(formatted.contains("Original text:"))
        assertTrue(formatted.contains("meeting tomorrow"))
        assertTrue(formatted.contains("Actively polish, improve, and elevate"))
    }
}

