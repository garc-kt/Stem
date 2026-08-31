package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.api.ClaudeClient
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class ClaudeRuleEngine(
    private val apiKey: String,
    private val model: String,
    private val customInstruction: String = "",
    private val temperature: Float = 0.3f
) : TextEngine {

    override suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult = withContext(Dispatchers.IO) {
        val original = payload.text
        if (original.isBlank()) {
            return@withContext TransformResult(original, original, preset, emptyList())
        }

        val systemPrompt = GeminiRuleEngine.getSystemPrompt(preset, customInstruction)
        val result = ClaudeClient.generate(
            apiKey = apiKey,
            model = model,
            prompt = original,
            systemPrompt = systemPrompt,
            temperature = temperature
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
                    summaryNote = "Claude ($model) • ${preset.title}"
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

    override suspend fun generateAllSuggestions(payload: TextPayload): Map<TransformPreset, TransformResult> = coroutineScope {
        // Each preset issues its own network call; fan them out in parallel instead of
        // awaiting one at a time (4x latency for no benefit — this is never on the
        // per-keystroke overlay path, only the sandbox/multi-suggestion views).
        TransformPreset.entries
            .map { preset -> preset to async { transform(payload, preset) } }
            .associate { (preset, deferred) -> preset to deferred.await() }
    }
}
