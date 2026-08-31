package com.veggiebit.sprout.features.enhancement.data.models

/**
 * Result of a transformation containing the enhanced text, calculated diff tokens, and summary statistics.
 */
data class TransformResult(
    val originalText: String,
    val transformedText: String,
    val preset: TransformPreset,
    val diffTokens: List<DiffToken> = emptyList(),
    val summaryNote: String? = null
) {
    val hasChanges: Boolean get() = originalText != transformedText

    val originalWordCount: Int get() = if (originalText.isBlank()) 0 else originalText.trim().split(Regex("\\s+")).size
    val transformedWordCount: Int get() = if (transformedText.isBlank()) 0 else transformedText.trim().split(Regex("\\s+")).size

    val wordsDelta: Int get() = transformedWordCount - originalWordCount
    val charDelta: Int get() = transformedText.length - originalText.length

    val wordsSaved: Int get() = if (wordsDelta < 0) -wordsDelta else 0
}
