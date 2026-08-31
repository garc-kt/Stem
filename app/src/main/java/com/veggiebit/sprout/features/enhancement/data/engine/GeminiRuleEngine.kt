package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.api.GeminiClient
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class GeminiRuleEngine(
    private val apiKey: String,
    private val model: String,
    private val customInstruction: String = ""
) : TextEngine {

    companion object {
        /**
         * Shared by Gemini, OpenAI-compatible, and Claude — all three follow the same prompt
         * shapes. [customInstruction] is only consulted for [TransformPreset.CUSTOM]; it comes
         * from the user's own "Custom Prompt" settings field.
         */
        fun getSystemPrompt(preset: TransformPreset, customInstruction: String = ""): String {
            return when (preset) {
                TransformPreset.FIX ->
                    "You are an expert text proofreader. Correct all spelling mistakes, grammar errors, casing, and punctuation. Return ONLY the polished text without any quotes or explanations."

                TransformPreset.CONCISE ->
                    "You are a concise editor. Rewrite the text to be clear and concise by eliminating unnecessary words while preserving full meaning. Return ONLY the rewritten text without quotes."

                TransformPreset.PROFESSIONAL ->
                    "You are an executive communications assistant. Rewrite the text into polite, articulate, professional business language. Return ONLY the rewritten text without quotes."

                TransformPreset.PUNCHY ->
                    "You are a high-impact copywriter. Rewrite the text to be active, punchy, energetic, and engaging. Return ONLY the rewritten text without quotes."

                TransformPreset.FRIENDLY ->
                    "You are a warm, approachable communications assistant. Rewrite the text in a friendly, conversational tone while keeping it clear and readable. Return ONLY the rewritten text without quotes."

                TransformPreset.SUMMARIZE ->
                    "You are a precise summarizer. Reduce the text to its essential point(s) in as few sentences as possible without losing critical meaning. Return ONLY the summary without quotes."

                TransformPreset.BULLETIZE ->
                    "You are an editor turning prose into a scannable bullet list. Rewrite the text as concise bullet points (one idea per line, prefixed with \"• \"). Return ONLY the bullet list without quotes or preamble."

                TransformPreset.EXPAND ->
                    "You are a thoughtful writer expanding on brief notes. Add clarifying detail and context to the text while keeping the same intent and tone. Return ONLY the expanded text without quotes."

                TransformPreset.CUSTOM -> {
                    val instruction = customInstruction.trim().ifBlank {
                        "Lightly polish the text for grammar and clarity."
                    }
                    "You are a writing assistant. Follow this instruction exactly: $instruction. Return ONLY the rewritten text without quotes or explanations."
                }
            }
        }
    }

    override suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult = withContext(Dispatchers.IO) {
        val original = payload.text
        if (original.isBlank()) {
            return@withContext TransformResult(original, original, preset, emptyList())
        }

        val systemPrompt = getSystemPrompt(preset, customInstruction)
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

    override suspend fun generateAllSuggestions(payload: TextPayload): Map<TransformPreset, TransformResult> = coroutineScope {
        // Each preset issues its own network call; fan them out in parallel instead of
        // awaiting one at a time (4x latency for no benefit — this is never on the
        // per-keystroke overlay path, only the sandbox/multi-suggestion views).
        TransformPreset.entries
            .map { preset -> preset to async { transform(payload, preset) } }
            .associate { (preset, deferred) -> preset to deferred.await() }
    }
}
