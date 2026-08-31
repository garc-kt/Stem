package com.veggiebit.sprout.features.enhancement.data.engine.rules

/**
 * A language's dictionaries for [com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine].
 * The structural logic (protect/restore spans, capitalization, case-preserving replacement,
 * sentence splitting) stays language-agnostic in LocalRuleEngine itself; only the word lists
 * and any language-specific punctuation fix-up live here. See [EnglishRules], [SpanishRules].
 */
interface LanguageRules {
    val typoDictionary: Map<String, String>
    val wordyPhrases: Map<String, String>
    val formalReplacements: Map<String, String>
    val friendlyReplacements: Map<String, String>
    val punchyStarters: Map<String, String>
    val punchyWords: Map<String, String>

    /** Casual words in [formalReplacements] whose replacement would break a phrasal-verb-like
     * particle construction (e.g. English "get up" -> "obtain up"). Empty when not applicable. */
    val phrasalVerbGuards: Map<String, Set<String>>

    /** Regexes matching this language's common abbreviations, protected from the
     * punctuation/capitalization pass the same way emails and URLs are. */
    val abbreviationPattern: Regex

    /** Language-specific post-processing applied once, after capitalization — e.g. Spanish
     * inverted question/exclamation marks. Identity (no-op) by default. */
    fun applyLanguageSpecificFixes(text: String): String = text
}
