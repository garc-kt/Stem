package com.stem.core.models

import android.graphics.Rect

private val WHITESPACE_REGEX = Regex("\\s+")

enum class DiffType {
    ADDED,
    DELETED,
    UNMODIFIED
}

/**
 * Represents a token or substring segment within a visual diff comparison.
 */
data class DiffToken(
    val text: String,
    val type: DiffType
)

/**
 * AI / Rule engine selection mode.
 */
enum class EngineMode(
    val id: String,
    val title: String,
    val description: String,
    val isCloud: Boolean
) {
    LOCAL_RULES(
        id = "local_rules",
        title = "On-Device Rules (Instant)",
        description = "100% offline, 0ms latency in local RAM. Zero network.",
        isCloud = false
    ),
    OLLAMA_AI(
        id = "ollama_ai",
        title = "Ollama Local AI (PC / LAN)",
        description = "Connects over Wi-Fi to Ollama on your PC (Llama 3.3, Mistral, Gemma 2).",
        isCloud = false
    ),
    GEMINI_AI(
        id = "gemini_ai",
        title = "Google Gemini",
        description = "Ultra-fast text enhancement using Google Gemini 3.7 Flash and newer.",
        isCloud = true
    ),
    OPENAI_COMPATIBLE(
        id = "openai_compatible",
        title = "OpenAI / OpenRouter / DeepSeek",
        description = "GPT-5, DeepSeek, Groq, or any OpenAI-compatible API endpoint.",
        isCloud = true
    ),
    CLAUDE_AI(
        id = "claude_ai",
        title = "Anthropic Claude",
        description = "High-precision writing enhancement using Claude Sonnet 5 and newer.",
        isCloud = true
    );

    companion object {
        fun fromId(id: String): EngineMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: LOCAL_RULES
        }
    }
}

/**
 * Which dictionary LocalRuleEngine uses.
 */
enum class LanguagePreference(val id: String, val label: String) {
    AUTO(id = "auto", label = "Auto-detect"),
    ENGLISH(id = "english", label = "English"),
    SPANISH(id = "spanish", label = "Español");

    companion object {
        fun fromId(id: String): LanguagePreference =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO
    }
}

/**
 * Encapsulates the text buffer and context captured from an active AccessibilityNode.
 */
data class TextPayload(
    val text: String,
    val selectionStart: Int = -1,
    val selectionEnd: Int = -1,
    val boundsInScreen: Rect? = null,
    val packageName: String? = null,
    val className: String? = null,
    val nodeHashCode: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isValid: Boolean get() = text.isNotBlank() && text.length >= 2
    // Computed on access, not at construction: a TextPayload is built on every accessibility
    // event, but nothing on that hot path reads wordCount today.
    val wordCount: Int get() = if (text.isBlank()) 0 else text.trim().split(WHITESPACE_REGEX).size
    val charCount: Int get() = text.length
}

/**
 * Transformation modes supported by Stem.
 */
enum class TransformPreset(
    val id: String,
    val title: String,
    val shortName: String,
    val description: String,
    val emoji: String,
    val useDiff: Boolean = true,
    val isOfflineApproximate: Boolean = false
) {
    FIX(
        id = "fix",
        title = "Fix & Polish",
        shortName = "Fix",
        description = "Corrects typos, casing, grammar, and punctuation.",
        emoji = "✨",
        useDiff = true
    ),
    CONCISE(
        id = "concise",
        title = "Concise",
        shortName = "Concise",
        description = "Trims filler words and sharpens direct meaning.",
        emoji = "⚡",
        useDiff = true
    ),
    PROFESSIONAL(
        id = "professional",
        title = "Professional",
        shortName = "Formal",
        description = "Elevates tone with courteous, polished vocabulary.",
        emoji = "👔",
        useDiff = true
    ),
    PUNCHY(
        id = "punchy",
        title = "Punchy",
        shortName = "Punchy",
        description = "High-energy, engaging, and memorable phrasing.",
        emoji = "🔥",
        useDiff = true
    ),
    FRIENDLY(
        id = "friendly",
        title = "Friendly",
        shortName = "Friendly",
        description = "Warmer, more approachable everyday tone.",
        emoji = "😊",
        useDiff = true
    ),
    SUMMARIZE(
        id = "summarize",
        title = "Summarize",
        shortName = "Summarize",
        description = "Reduces text to its core point.",
        emoji = "📝",
        useDiff = false,
        isOfflineApproximate = true
    ),
    BULLETIZE(
        id = "bulletize",
        title = "Bulletize",
        shortName = "Bulletize",
        description = "Restructures text into scannable bullet points.",
        emoji = "•",
        useDiff = false,
        isOfflineApproximate = true
    ),
    EXPAND(
        id = "expand",
        title = "Expand",
        shortName = "Expand",
        description = "Adds detail and clarifying context.",
        emoji = "🔎",
        useDiff = false,
        isOfflineApproximate = true
    ),
    CUSTOM(
        id = "custom",
        title = "Custom",
        shortName = "Custom",
        description = "Your own instruction, applied by the active AI engine.",
        emoji = "🎛️",
        useDiff = true
    );

    companion object {
        fun fromId(id: String): TransformPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FIX
        }
    }
}

/**
 * Result of a transformation containing the enhanced text, calculated diff tokens, and summary statistics.
 */
data class TransformResult(
    val originalText: String,
    val transformedText: String,
    val preset: TransformPreset,
    val diffTokens: List<DiffToken> = emptyList(),
    val summaryNote: String? = null,
    /** Set only when the requested engine failed and this result is the local-rules fallback —
     * lets callers tell the user their AI request didn't actually run instead of silently
     * showing a successful-looking result. Null on every other path. */
    val errorMessage: String? = null
) {
    val hasChanges: Boolean get() = originalText != transformedText

    // Computed on access, not at construction: a TransformResult is built on every transform
    // (including the accessibility-service inline-command hot path), but only ProcessTextActivity
    // ever reads these word-count stats.
    val originalWordCount: Int get() = if (originalText.isBlank()) 0 else originalText.trim().split(WHITESPACE_REGEX).size
    val transformedWordCount: Int get() = if (transformedText.isBlank()) 0 else transformedText.trim().split(WHITESPACE_REGEX).size

    val wordsDelta: Int get() = transformedWordCount - originalWordCount
    val charDelta: Int get() = transformedText.length - originalText.length

    val wordsSaved: Int get() = if (wordsDelta < 0) -wordsDelta else 0
}
