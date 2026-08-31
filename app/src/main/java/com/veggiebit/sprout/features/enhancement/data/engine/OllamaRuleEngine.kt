package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.enhancement.data.ollama.OllamaClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Text Engine powered by a local Ollama server running on a PC / local LAN.
 * Gracefully falls back to [LocalRuleEngine] if the Ollama server is offline or unreachable.
 */
class OllamaRuleEngine(
    private val baseUrl: String,
    private val model: String,
    private val customInstruction: String = "",
    private val temperature: Float = 0.3f
) : TextEngine {

    override suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult = withContext(Dispatchers.IO) {
        val original = payload.text
        if (original.isBlank()) {
            return@withContext TransformResult(original, original, preset, emptyList())
        }

        // Reuses GeminiRuleEngine's prompt set (previously duplicated here near-verbatim) so
        // all four AI engines stay in sync as presets are added.
        val systemPrompt = GeminiRuleEngine.getSystemPrompt(preset, customInstruction)
        val formattedPrompt = GeminiRuleEngine.formatUserPrompt(original, preset, customInstruction)
        val result = OllamaClient.generate(
            baseUrl = baseUrl,
            model = model,
            prompt = formattedPrompt,
            systemPrompt = systemPrompt,
            temperature = temperature
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

    override suspend fun generateAllSuggestions(payload: TextPayload): Map<TransformPreset, TransformResult> = coroutineScope {
        // Each preset issues its own network call; fan them out in parallel instead of
        // awaiting one at a time (4x latency for no benefit — this is never on the
        // per-keystroke overlay path, only the sandbox/multi-suggestion views).
        TransformPreset.entries
            .map { preset -> preset to async { transform(payload, preset) } }
            .associate { (preset, deferred) -> preset to deferred.await() }
    }
}
