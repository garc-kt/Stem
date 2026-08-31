package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.settings.data.SproutUserSettings

object TextEngineProvider {

    fun getEngine(settings: SproutUserSettings): TextEngine {
        return when (settings.engineMode) {
            EngineMode.LOCAL_RULES -> LocalRuleEngine
            EngineMode.OLLAMA_AI -> OllamaRuleEngine(
                baseUrl = settings.ollamaBaseUrl,
                model = settings.ollamaModel
            )
            EngineMode.GEMINI_AI -> GeminiRuleEngine(
                apiKey = settings.geminiApiKey,
                model = settings.geminiModel
            )
            EngineMode.OPENAI_COMPATIBLE -> OpenAIRuleEngine(
                baseUrl = settings.openaiBaseUrl,
                apiKey = settings.openaiApiKey,
                model = settings.openaiModel
            )
            EngineMode.CLAUDE_AI -> ClaudeRuleEngine(
                apiKey = settings.claudeApiKey,
                model = settings.claudeModel
            )
        }
    }
}
