package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.api.GeminiClient
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRuleEngine(
    private val apiKey: String,
    private val model: String
) : TextEngine {

    companion object {
        fun getSystemPrompt(preset: TransformPreset): String {
            return when (preset) {
                TransformPreset.FIX ->
                    "You are an expert text proofreader. Correct all spelling mistakes, grammar errors, casing, and punctuation. Return ONLY the polished text without any quotes or explanations."

                TransformPreset.CONCISE ->
                    "You are a concise editor. Rewrite the text to be clear and concise by eliminating unnecessary words while preserving full meaning. Return ONLY the rewritten text without quotes."

                TransformPreset.PROFESSIONAL ->
                    "You are an executive communications assistant. Rewrite the text into polite, articulate, professional business language. Return ONLY the rewritten text without quotes."

                TransformPreset.PUNCHY ->
                    "You are a high-impact copywriter. Rewrite the text to be active, punchy, energetic, and engaging. Return ONLY the rewritten text without quotes."
            }
        }
    }

    override suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult = withContext(Dispatchers.IO) {
        val original = payload.text
        if (original.isBlank()) {
            return@withContext TransformResult(original, original, preset, emptyList())
        }

        val systemPrompt = getSystemPrompt(preset)
        val result = GeminiClient.generate(
            apiKey = apiKey,
            model = model,
            prompt = original,
            systemPrompt = systemPrompt
        )

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
                TransformResult(
                    originalText = original,
                    transformedText = cleaned,
                    preset = preset,
                    diffTokens = diff,
                    summaryNote = "Gemini ($model) • ${preset.title}"
                )
            },
            onFailure = { _ ->
                val fallback = LocalRuleEngine.transform(payload, preset)
                fallback.copy(
                    summaryNote = "${fallback.summaryNote ?: "Polished"} (Local fallback)"
                )
            }
        )
    }

    override suspend fun generateAllSuggestions(payload: TextPayload): Map<TransformPreset, TransformResult> = withContext(Dispatchers.IO) {
        TransformPreset.entries.associateWith { preset ->
            transform(payload, preset)
        }
    }
}
