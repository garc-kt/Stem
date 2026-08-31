package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.settings.data.SproutUserSettings

object TextEngineProvider {

    fun getEngine(settings: SproutUserSettings): TextEngine {
        return when (settings.engineMode) {
            EngineMode.LOCAL_RULES -> LocalRuleEngine.apply { languagePreference = settings.languagePreference }
            EngineMode.OLLAMA_AI -> OllamaRuleEngine(
                baseUrl = settings.ollamaBaseUrl,
                model = settings.ollamaModel,
                customInstruction = settings.customPromptInstruction
            )
            EngineMode.GEMINI_AI -> GeminiRuleEngine(
                apiKey = settings.geminiApiKey,
                model = settings.geminiModel,
                customInstruction = settings.customPromptInstruction
            )
            EngineMode.OPENAI_COMPATIBLE -> OpenAIRuleEngine(
                baseUrl = settings.openaiBaseUrl,
                apiKey = settings.openaiApiKey,
                model = settings.openaiModel,
                customInstruction = settings.customPromptInstruction
            )
            EngineMode.CLAUDE_AI -> ClaudeRuleEngine(
                apiKey = settings.claudeApiKey,
                model = settings.claudeModel,
                customInstruction = settings.customPromptInstruction
            )
        }
    }

    /**
     * Stable, non-secret identifier for the active engine configuration, used only as a
     * [TransformCache] partition key so switching models/hosts doesn't serve stale results.
     * Never includes API key material.
     */
    fun engineSignature(settings: SproutUserSettings): String {
        return when (settings.engineMode) {
            EngineMode.LOCAL_RULES -> "local"
            EngineMode.OLLAMA_AI -> "ollama:${settings.ollamaBaseUrl}:${settings.ollamaModel}"
            EngineMode.GEMINI_AI -> "gemini:${settings.geminiModel}"
            EngineMode.OPENAI_COMPATIBLE -> "openai:${settings.openaiBaseUrl}:${settings.openaiModel}"
            EngineMode.CLAUDE_AI -> "claude:${settings.claudeModel}"
        }
    }
}
