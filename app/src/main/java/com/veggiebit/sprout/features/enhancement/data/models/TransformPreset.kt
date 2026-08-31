package com.veggiebit.sprout.features.enhancement.data.models

/**
 * Transformation modes supported by Sprout.
 */
enum class TransformPreset(
    val id: String,
    val title: String,
    val shortName: String,
    val description: String,
    val emoji: String
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
    );

    companion object {
        fun fromId(id: String): TransformPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FIX
        }
    }
}
