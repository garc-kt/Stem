package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.enhancement.data.ollama.OllamaClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Text Engine powered by a local Ollama server running on a PC / local LAN.
 * Gracefully falls back to [LocalRuleEngine] if the Ollama server is offline or unreachable.
 */
class OllamaRuleEngine(
    private val baseUrl: String,
    private val model: String
) : TextEngine {

    companion object {
        private fun getSystemPrompt(preset: TransformPreset): String {
            return when (preset) {
                TransformPreset.FIX ->
                    "You are an expert text proofreader. Correct all spelling mistakes, grammar errors, casing, and punctuation in the user text. Return ONLY the polished text without any quotes, conversational filler, or explanations."

                TransformPreset.CONCISE ->
                    "You are a concise editor. Rewrite the user text to be concise, direct, and clear by removing filler words while keeping the core meaning. Return ONLY the rewritten text without quotes or preamble."

                TransformPreset.PROFESSIONAL ->
                    "You are an executive communications assistant. Rewrite the user text into polite, polished, and professional language suitable for business communication. Return ONLY the rewritten text without quotes or preamble."

                TransformPreset.PUNCHY ->
                    "You are a high-impact copywriter. Rewrite the user text to be punchy, energetic, active, and engaging. Return ONLY the rewritten text without quotes or preamble."
            }
        }
    }

    override suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult = withContext(Dispatchers.IO) {
        val original = payload.text
        if (original.isBlank()) {
            return@withContext TransformResult(original, original, preset, emptyList())
        }

        val systemPrompt = getSystemPrompt(preset)
        val result = OllamaClient.generate(
            baseUrl = baseUrl,
            model = model,
            prompt = original,
            systemPrompt = systemPrompt
        )

        result.fold(
            onSuccess = { rawOutput ->
                // Clean potential wrapper quotes or markdown code blocks
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
                    summaryNote = "Ollama ($model) • ${preset.title}"
                )
            },
            onFailure = { _ ->
                // Fall back gracefully to on-device rule engine
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
