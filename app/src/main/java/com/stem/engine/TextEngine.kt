package com.stem.engine

import com.stem.core.models.EngineMode
import com.stem.core.models.LanguagePreference
import com.stem.core.models.StemUserSettings
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import com.stem.core.models.TransformResult



/**
 * Contract for text transformation engines. [languagePreference] is only meaningful to
 * [LocalRuleEngine] (which dictionary to apply) but is threaded through every implementation —
 * including as the local-rules fallback language for the AI engines — as an explicit per-call
 * parameter rather than shared mutable state: this ran across coroutines on different
 * dispatchers (the accessibility service, ProcessTextActivity, and an AI engine's own fallback
 * path could all call it around the same time), so a single `var` on a singleton object was a
 * genuine data race, not just a style concern.
 */
interface TextEngine {
    suspend fun transform(
        payload: TextPayload,
        preset: TransformPreset,
        languagePreference: LanguagePreference = LanguagePreference.AUTO
    ): TransformResult
}

object TextEngineProvider {

    fun getEngine(settings: StemUserSettings): TextEngine {
        return when (settings.engineMode) {
            EngineMode.LOCAL_RULES -> LocalRuleEngine
            EngineMode.OLLAMA_AI -> OllamaRuleEngine(
                baseUrl = settings.ollamaBaseUrl,
                model = settings.ollamaModel,
                customInstruction = settings.customPromptInstruction,
                temperature = settings.temperature
            )
            EngineMode.GEMINI_AI -> GeminiRuleEngine(
                apiKey = settings.geminiApiKey,
                model = settings.geminiModel,
                customInstruction = settings.customPromptInstruction,
                temperature = settings.temperature
            )
            EngineMode.OPENAI_COMPATIBLE -> OpenAIRuleEngine(
                baseUrl = settings.openaiBaseUrl,
                apiKey = settings.openaiApiKey,
                model = settings.openaiModel,
                customInstruction = settings.customPromptInstruction,
                temperature = settings.temperature
            )
            EngineMode.CLAUDE_AI -> ClaudeRuleEngine(
                apiKey = settings.claudeApiKey,
                model = settings.claudeModel,
                customInstruction = settings.customPromptInstruction,
                temperature = settings.temperature
            )
        }
    }

    /**
     * Stable, non-secret identifier for the active engine configuration, used as a
     * TransformCache partition key.
     */
    fun engineSignature(settings: StemUserSettings): String {
        val temp = String.format(java.util.Locale.US, "%.2f", settings.temperature)
        val pHash = settings.customPromptInstruction.hashCode()
        return when (settings.engineMode) {
            EngineMode.LOCAL_RULES -> "local"
            EngineMode.OLLAMA_AI -> "ollama:${settings.ollamaBaseUrl}:${settings.ollamaModel}:$temp:$pHash"
            EngineMode.GEMINI_AI -> "gemini:${settings.geminiModel}:$temp:$pHash"
            EngineMode.OPENAI_COMPATIBLE -> "openai:${settings.openaiBaseUrl}:${settings.openaiModel}:$temp:$pHash"
            EngineMode.CLAUDE_AI -> "claude:${settings.claudeModel}:$temp:$pHash"
        }
    }
}
