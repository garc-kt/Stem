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
    private val customInstruction: String = "",
    private val temperature: Float = 0.3f
) : TextEngine {

    companion object {
        /**
         * Shared by Gemini, OpenAI-compatible, Claude, and Ollama — all four follow the same prompt
         * shapes. [customInstruction] provides a master directive that elevates and refines all AI
         * transformations, or acts as the primary instruction for [TransformPreset.CUSTOM].
         */
        fun getSystemPrompt(preset: TransformPreset, customInstruction: String = ""): String {
            val masterDirective = if (customInstruction.isNotBlank() && preset != TransformPreset.CUSTOM) {
                "\nADDITIONAL MASTER DIRECTIVE: ${customInstruction.trim()}. Ensure the transformed text strictly satisfies this directive."
            } else ""

            val basePrompt = when (preset) {
                TransformPreset.FIX ->
                    "You are a master writing editor and proofreader. Actively elevate and polish the text: fix all grammar, spelling, typos, awkward phrasing, and punctuation while enhancing vocabulary, rhythm, and clarity. Maintain the original core meaning and voice. Return ONLY the enhanced text without quotes, preambles, or explanations."

                TransformPreset.CONCISE ->
                    "You are an expert concise editor. Aggressively streamline the text to be sharp, crystal-clear, and concise: eliminate fluff, redundancy, and passive constructions while maximizing clarity and punch. Return ONLY the rewritten text without quotes."

                TransformPreset.PROFESSIONAL ->
                    "You are an executive communications strategist. Transform the text into articulate, polished, high-status, professional business language with confident authority and diplomatic courtesy. Return ONLY the rewritten text without quotes."

                TransformPreset.PUNCHY ->
                    "You are a high-impact copywriter. Rewrite the text to be active, energetic, punchy, and compelling with strong action verbs and crisp cadence. Return ONLY the rewritten text without quotes."

                TransformPreset.FRIENDLY ->
                    "You are a warm, charismatic communicator. Rewrite the text in a delightful, warm, empathetic, and conversational tone while ensuring effortless readability. Return ONLY the rewritten text without quotes."

                TransformPreset.SUMMARIZE ->
                    "You are a precision summarizer. Distill the text to its critical insights in as few sentences as possible without losing key meaning. Return ONLY the summary without quotes."

                TransformPreset.BULLETIZE ->
                    "You are an expert information designer. Transform the prose into a crisp, scannable bullet point list (one distinct thought per line, prefixed with \"• \"). Return ONLY the bullet points without quotes or introductory text."

                TransformPreset.EXPAND ->
                    "You are an eloquent writer. Elaborate and flesh out the ideas with rich supporting context, vivid phrasing, and smooth transitions while preserving intent. Return ONLY the expanded text without quotes."

                TransformPreset.CUSTOM -> {
                    val instruction = customInstruction.trim().ifBlank {
                        "Actively enhance the text with polished grammar, superior vocabulary, and natural flow."
                    }
                    "You are an expert AI writing assistant. Follow this instruction exactly: $instruction. Return ONLY the rewritten text without quotes or explanations."
                }
            }

            return basePrompt + masterDirective
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
