package com.veggiebit.sprout.features.enhancement.data.models

/**
 * Transformation modes supported by Sprout. [isOfflineApproximate] marks presets whose
 * [LocalRuleEngine][com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine]
 * implementation is a rule-based approximation of what an AI engine would produce (e.g.
 * SUMMARIZE offline is "first sentence + key clauses", not a real summary) — the UI uses this
 * to label the result honestly rather than implying AI-quality output from local rules.
 */
enum class TransformPreset(
    val id: String,
    val title: String,
    val shortName: String,
    val description: String,
    val emoji: String,
    val isOfflineApproximate: Boolean = false
) {
    FIX(
        id = "fix",
        title = "Fix & Polish",
        shortName = "Fix",
        description = "Corrects typos, casing, grammar, and punctuation.",
        emoji = "✨"
    ),
    CONCISE(
        id = "concise",
        title = "Concise",
        shortName = "Concise",
        description = "Trims filler words and sharpens direct meaning.",
        emoji = "⚡"
    ),
    PROFESSIONAL(
        id = "professional",
        title = "Professional",
        shortName = "Formal",
        description = "Elevates tone with courteous, polished vocabulary.",
        emoji = "👔"
    ),
    PUNCHY(
        id = "punchy",
        title = "Punchy",
        shortName = "Punchy",
        description = "High-energy, engaging, and memorable phrasing.",
        emoji = "🔥"
    ),
    FRIENDLY(
        id = "friendly",
        title = "Friendly",
        shortName = "Friendly",
        description = "Warmer, more approachable everyday tone.",
        emoji = "😊"
    ),
    SUMMARIZE(
        id = "summarize",
        title = "Summarize",
        shortName = "Summary",
        description = "Reduces text to its core point.",
        emoji = "📝",
        isOfflineApproximate = true
    ),
    BULLETIZE(
        id = "bulletize",
        title = "Bulletize",
        shortName = "Bullets",
        description = "Restructures text into scannable bullet points.",
        emoji = "•",
        isOfflineApproximate = true
    ),
    EXPAND(
        id = "expand",
        title = "Expand",
        shortName = "Expand",
        description = "Adds detail and clarifying context.",
        emoji = "🔎",
        isOfflineApproximate = true
    ),
    CUSTOM(
        id = "custom",
        title = "Custom",
        shortName = "Custom",
        description = "Your own instruction, applied by the active AI engine.",
        emoji = "🎛️"
    );

    companion object {
        val defaultOrder: List<TransformPreset> = entries.toList()

        fun fromId(id: String): TransformPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FIX
        }
    }
}
