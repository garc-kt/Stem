package com.veggiebit.sprout.features.enhancement.data.models

import com.veggiebit.sprout.app.theme.StemIconType

/**
 * Transformation modes supported by Stem / Sprout.
 */
enum class TransformPreset(
    val id: String,
    val title: String,
    val shortName: String,
    val description: String,
    val emoji: String,
    val iconType: StemIconType,
    val useDiff: Boolean = true,
    val isOfflineApproximate: Boolean = false
) {
    FIX(
        id = "fix",
        title = "Fix & Polish",
        shortName = "Fix",
        description = "Corrects typos, casing, grammar, and punctuation.",
        emoji = "✨",
        iconType = StemIconType.SQUARE_OUTLINE,
        useDiff = true
    ),
    CONCISE(
        id = "concise",
        title = "Concise",
        shortName = "Concise",
        description = "Trims filler words and sharpens direct meaning.",
        emoji = "⚡",
        iconType = StemIconType.BAR,
        useDiff = true
    ),
    PROFESSIONAL(
        id = "professional",
        title = "Professional",
        shortName = "Formal",
        description = "Elevates tone with courteous, polished vocabulary.",
        emoji = "👔",
        iconType = StemIconType.SQUARE_FILLED,
        useDiff = true
    ),
    PUNCHY(
        id = "punchy",
        title = "Punchy",
        shortName = "Punchy",
        description = "High-energy, engaging, and memorable phrasing.",
        emoji = "🔥",
        iconType = StemIconType.TRIANGLE,
        useDiff = true
    ),
    FRIENDLY(
        id = "friendly",
        title = "Friendly",
        shortName = "Friendly",
        description = "Warmer, more approachable everyday tone.",
        emoji = "😊",
        iconType = StemIconType.CIRCLE_OUTLINE,
        useDiff = true
    ),
    SUMMARIZE(
        id = "summarize",
        title = "Summarize",
        shortName = "Summarize",
        description = "Reduces text to its core point.",
        emoji = "📝",
        iconType = StemIconType.LINES,
        useDiff = false,
        isOfflineApproximate = true
    ),
    BULLETIZE(
        id = "bulletize",
        title = "Bulletize",
        shortName = "Bulletize",
        description = "Restructures text into scannable bullet points.",
        emoji = "•",
        iconType = StemIconType.DOTS,
        useDiff = false,
        isOfflineApproximate = true
    ),
    EXPAND(
        id = "expand",
        title = "Expand",
        shortName = "Expand",
        description = "Adds detail and clarifying context.",
        emoji = "🔎",
        iconType = StemIconType.DIAMOND,
        useDiff = false,
        isOfflineApproximate = true
    ),
    CUSTOM(
        id = "custom",
        title = "Custom",
        shortName = "Custom",
        description = "Your own instruction, applied by the active AI engine.",
        emoji = "🎛️",
        iconType = StemIconType.PLUS,
        useDiff = true
    );

    companion object {
        val defaultOrder: List<TransformPreset> = entries.toList()

        fun fromId(id: String): TransformPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FIX
        }
    }
}

