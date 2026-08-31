package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.engine.rules.DetectedLanguage
import com.veggiebit.sprout.features.enhancement.data.engine.rules.EnglishRules
import com.veggiebit.sprout.features.enhancement.data.engine.rules.LanguageDetector
import com.veggiebit.sprout.features.enhancement.data.engine.rules.LanguageRules
import com.veggiebit.sprout.features.enhancement.data.engine.rules.SpanishRules
import com.veggiebit.sprout.features.enhancement.data.models.LanguagePreference
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance, 100% on-device rule and dictionary-based transformation engine.
 */
object LocalRuleEngine : TextEngine {

    /**
     * Set by [TextEngineProvider] right before each call. LocalRuleEngine is a stateless
     * singleton reused everywhere in the app, so this is the least invasive way to thread the
     * user's language preference through without changing the [TextEngine] interface (which
     * all five engines and the existing test suite depend on) — every caller resolves the
     * engine and immediately calls transform()/generateAllSuggestions() within the same
     * coroutine, so there's no real race in practice. AUTO detects per-call from the text.
     */
    var languagePreference: LanguagePreference = LanguagePreference.AUTO

    private fun resolveRules(text: String): LanguageRules {
        val effective = when (languagePreference) {
            LanguagePreference.AUTO -> LanguageDetector.detect(text)
            LanguagePreference.ENGLISH -> DetectedLanguage.ENGLISH
            LanguagePreference.SPANISH -> DetectedLanguage.SPANISH
        }
        return if (effective == DetectedLanguage.SPANISH) SpanishRules else EnglishRules
    }

    // Marks the start/end of a substring that must survive the transformation pipeline
    // untouched (emails, URLs, handles, decimals, common abbreviations) so punctuation-spacing
    // and capitalization rules don't mangle them (e.g. "user@example.com" -> "user@example. Com").
    private const val PROTECT_OPEN = '\uE000'
    private const val PROTECT_CLOSE = '\uE001'

    // Language-agnostic character-class patterns; each language's abbreviation set is appended
    // per-call via LanguageRules.abbreviationPattern.
    private val sharedProtectedPatterns = listOf(
        Regex("[\\w.+-]+@[\\w-]+\\.[\\w.-]+"), // email addresses
        Regex("https?://\\S+", RegexOption.IGNORE_CASE), // URLs with scheme
        Regex("\\bwww\\.[\\w-]+(?:\\.[\\w-]+)+\\S*", RegexOption.IGNORE_CASE), // URLs without scheme
        Regex("[@#][A-Za-z0-9_]+"), // @handles and #hashtags
        Regex("\\d+\\.\\d+") // decimals
    )

    private fun protectSpans(input: String, rules: LanguageRules): Pair<String, List<String>> {
        val saved = mutableListOf<String>()
        var text = input
        for (pattern in sharedProtectedPatterns + rules.abbreviationPattern) {
            text = pattern.replace(text) { match ->
                saved.add(match.value)
                "$PROTECT_OPEN${saved.size - 1}$PROTECT_CLOSE"
            }
        }
        return text to saved
    }

    private fun restoreSpans(input: String, saved: List<String>): String {
        if (saved.isEmpty()) return input
        val regex = Regex("$PROTECT_OPEN(\\d+)$PROTECT_CLOSE")
        return regex.replace(input) { match ->
            match.groupValues[1].toIntOrNull()?.let { idx -> saved.getOrNull(idx) } ?: match.value
        }
    }

    override suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult = withContext(Dispatchers.Default) {
        val original = payload.text
        if (original.isBlank()) {
            return@withContext TransformResult(original, original, preset, emptyList())
        }

        val rules = resolveRules(original)
        val transformed = when (preset) {
            TransformPreset.FIX -> applyFixAndPolish(original, rules)
            TransformPreset.CONCISE -> applyConcise(original, rules)
            TransformPreset.PROFESSIONAL -> applyProfessional(original, rules)
            TransformPreset.PUNCHY -> applyPunchy(original, rules)
            TransformPreset.FRIENDLY -> applyFriendly(original, rules)
            TransformPreset.SUMMARIZE -> applySummarize(original, rules)
            TransformPreset.BULLETIZE -> applyBulletize(original, rules)
            TransformPreset.EXPAND -> applyExpand(original, rules)
            // No local model to follow an arbitrary instruction — best-effort Fix & Polish,
            // with the summary note (below) making clear this preset needs an AI engine.
            TransformPreset.CUSTOM -> applyFixAndPolish(original, rules)
        }

        val diff = DiffCalculator.calculateDiff(original, transformed)
        TransformResult(
            originalText = original,
            transformedText = transformed,
            preset = preset,
            diffTokens = diff,
            summaryNote = buildSummaryNote(preset, original, transformed)
        )
    }

    override suspend fun generateAllSuggestions(payload: TextPayload): Map<TransformPreset, TransformResult> = withContext(Dispatchers.Default) {
        TransformPreset.entries.associateWith { preset ->
            transform(payload, preset)
        }
    }

    fun applyFixAndPolish(input: String, rules: LanguageRules = resolveRules(input)): String {
        // Emails, URLs, handles, decimals, and abbreviations must survive the punctuation and
        // capitalization rules below untouched — protect them first, restore at the end.
        val (protectedInput, savedSpans) = protectSpans(input, rules)
        var text = protectedInput

        text = text.replace(Regex("\\s+([,.:;?!])"), "$1")
        text = text.replace(Regex("([,.:;?!])([a-zA-Z])"), "$1 $2")
        text = text.replace(Regex("[ \\t]+"), " ")

        for ((wrong, right) in rules.typoDictionary) {
            text = replacePreservingCase(text, wrong, right)
        }

        text = text.replace(Regex("\\bi\\b"), "I")
        text = text.replace(Regex("\\bi'm\\b", RegexOption.IGNORE_CASE), "I'm")
        text = text.replace(Regex("\\bi'll\\b", RegexOption.IGNORE_CASE), "I'll")
        text = text.replace(Regex("\\bi've\\b", RegexOption.IGNORE_CASE), "I've")
        text = text.replace(Regex("\\bi'd\\b", RegexOption.IGNORE_CASE), "I'd")

        text = text.replace(Regex("\\b(\\w+)\\s+\\1\\b", RegexOption.IGNORE_CASE), "$1")
        text = capitalizeSentences(text)

        if (text.length > 3 && !text.endsWith(".") && !text.endsWith("!") && !text.endsWith("?")) {
            val words = text.trim().split(Regex("\\s+"))
            if (words.size >= 3) {
                text += "."
            }
        }

        text = restoreSpans(text, savedSpans)
        text = rules.applyLanguageSpecificFixes(text)
        return text.trim()
    }

    fun applyConcise(input: String, rules: LanguageRules = resolveRules(input)): String {
        var text = applyFixAndPolish(input, rules)

        for ((wordy, concise) in rules.wordyPhrases) {
            text = replacePreservingCase(text, wordy, concise)
        }

        val fillers = listOf("really", "basically", "literally", "actually", "honestly", "needless to say,")
        for (filler in fillers) {
            text = text.replace(Regex("\\b$filler\\s+", RegexOption.IGNORE_CASE), "")
        }

        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("\\s+([,.:;?!])"), "$1")
        text = capitalizeSentences(text)

        return text.trim()
    }

    fun applyProfessional(input: String, rules: LanguageRules = resolveRules(input)): String {
        var text = applyFixAndPolish(input, rules)

        for ((casual, formal) in rules.formalReplacements) {
            text = replaceFormalGuarded(text, casual, formal, rules)
        }

        text = text.replace(Regex("\\b(thanks a lot|thanks so much)\\b", RegexOption.IGNORE_CASE), "Thank you very much")
        text = text.replace(Regex("\\b(can you please)\\b", RegexOption.IGNORE_CASE), "Could you please")
        text = text.replace(Regex("\\b(give me a call)\\b", RegexOption.IGNORE_CASE), "reach out via phone")

        text = capitalizeSentences(text)
        return text.trim()
    }

    fun applyPunchy(input: String, rules: LanguageRules = resolveRules(input)): String {
        var text = applyFixAndPolish(input, rules)

        for ((starter, punchy) in rules.punchyStarters) {
            text = replacePreservingCase(text, starter, punchy)
        }

        for ((word, punchy) in rules.punchyWords) {
            text = replacePreservingCase(text, word, punchy)
        }

        text = text.replace(Regex("\\bi just wanted to\\b", RegexOption.IGNORE_CASE), "I wanted to")
        text = text.replace(Regex("\\btry to\\b", RegexOption.IGNORE_CASE), "")

        text = text.replace(Regex("[ \\t]+"), " ")
        text = capitalizeSentences(text)
        return text.trim()
    }

    fun applyFriendly(input: String, rules: LanguageRules = resolveRules(input)): String {
        var text = applyFixAndPolish(input, rules)

        for ((formal, casual) in rules.friendlyReplacements) {
            text = replacePreservingCase(text, formal, casual)
        }

        text = text.replace(Regex("\\b(please note that)\\b", RegexOption.IGNORE_CASE), "just so you know,")
        text = text.replace(Regex("\\b(I regret to inform you)\\b", RegexOption.IGNORE_CASE), "I'm sorry to say")
        text = text.replace(Regex("\\b(is required)\\b", RegexOption.IGNORE_CASE), "is needed")

        text = capitalizeSentences(text)
        return text.trim()
    }

    /** Approximate, offline-only summary: keeps the opening sentence plus any sentence carrying
     * a strong signal word (numbers, deadlines, action verbs). Honestly labeled as approximate
     * via [TransformPreset.isOfflineApproximate] — a real summary needs an AI engine. */
    fun applySummarize(input: String, rules: LanguageRules = resolveRules(input)): String {
        val polished = applyFixAndPolish(input, rules)
        val sentences = splitSentences(polished)
        if (sentences.size <= 2) return polished

        val signalWords = listOf("must", "important", "deadline", "urgent", "asap", "required", "critical")
        val signalRegex = Regex("\\b(${signalWords.joinToString("|")})\\b", RegexOption.IGNORE_CASE)
        val hasDigit = Regex("\\d")

        val kept = mutableListOf(sentences.first())
        for (sentence in sentences.drop(1)) {
            if (signalRegex.containsMatchIn(sentence) || hasDigit.containsMatchIn(sentence)) {
                kept.add(sentence)
            }
        }
        return kept.joinToString(" ").trim()
    }

    /** Splits terse/coordinated sentences into a bullet list — a structural rewrite, not a
     * content generator, so it stays honest at [TransformPreset.isOfflineApproximate]. */
    fun applyBulletize(input: String, rules: LanguageRules = resolveRules(input)): String {
        val polished = applyFixAndPolish(input, rules)
        val sentences = splitSentences(polished)
        if (sentences.isEmpty()) return polished

        return sentences.flatMap { sentence ->
            // Also break on coordinating "and then"/"; " clause joins so each bullet stays short.
            sentence.split(Regex("(?:,?\\s+and then\\s+|;\\s*)", RegexOption.IGNORE_CASE))
        }
            .map { it.trim().trimEnd('.', ' ') }
            .filter { it.isNotBlank() }
            .joinToString("\n") { "• $it" }
    }

    /** Offline "expand" is necessarily modest: it spells out common abbreviations/contractions
     * and adds a connective lead-in rather than inventing new content — see
     * [TransformPreset.isOfflineApproximate]. */
    fun applyExpand(input: String, rules: LanguageRules = resolveRules(input)): String {
        var text = applyFixAndPolish(input, rules)

        val expansions = mapOf(
            "asap" to "as soon as possible",
            "btw" to "by the way",
            "fyi" to "for your information",
            "thx" to "thank you",
            "wanna" to "want to",
            "gonna" to "going to",
            "gotta" to "have got to"
        )
        for ((short, long) in expansions) {
            text = replacePreservingCase(text, short, long)
        }

        val prefix = "To elaborate: "
        if (text.isNotBlank() && !text.startsWith(prefix, ignoreCase = true)) {
            text = prefix + text.replaceFirstChar { it.lowercaseChar() }
        }

        return text.trim()
    }

    private fun splitSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return Regex("(?<=[.!?])\\s+").split(text.trim())
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun capitalizeSentences(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder()
        var capitalizeNext = true

        for (i in text.indices) {
            val c = text[i]
            if (capitalizeNext && c.isLetter()) {
                sb.append(c.uppercaseChar())
                capitalizeNext = false
            } else {
                sb.append(c)
                if (c in listOf('.', '!', '?')) {
                    capitalizeNext = true
                }
            }
        }
        return sb.toString()
    }

    private fun replacePreservingCase(source: String, target: String, replacement: String): String {
        val pattern = Regex("\\b" + Regex.escape(target) + "\\b", RegexOption.IGNORE_CASE)
        return pattern.replace(source) { matchResult ->
            val match = matchResult.value
            when {
                match.all { it.isUpperCase() } && match.length > 1 -> replacement.uppercase()
                match.firstOrNull()?.isUpperCase() == true -> replacement.replaceFirstChar { it.uppercaseChar() }
                else -> replacement
            }
        }
    }

    /**
     * Like [replacePreservingCase], but skips the match when [target] is a guarded phrasal-verb
     * particle (see [LanguageRules.phrasalVerbGuards]) immediately followed by one of its
     * particles — otherwise "get up" becomes "obtain up" instead of staying a phrasal verb.
     */
    private fun replaceFormalGuarded(source: String, target: String, replacement: String, rules: LanguageRules): String {
        val particles = rules.phrasalVerbGuards[target.lowercase()]
        val pattern = if (particles != null) {
            Regex(
                "\\b" + Regex.escape(target) + "\\b(?!\\s+(?:" + particles.joinToString("|") { Regex.escape(it) } + ")\\b)",
                RegexOption.IGNORE_CASE
            )
        } else {
            Regex("\\b" + Regex.escape(target) + "\\b", RegexOption.IGNORE_CASE)
        }
        return pattern.replace(source) { matchResult ->
            val match = matchResult.value
            when {
                match.all { it.isUpperCase() } && match.length > 1 -> replacement.uppercase()
                match.firstOrNull()?.isUpperCase() == true -> replacement.replaceFirstChar { it.uppercaseChar() }
                else -> replacement
            }
        }
    }

    private fun buildSummaryNote(preset: TransformPreset, original: String, transformed: String): String {
        if (original == transformed) return "No changes required."
        val origWords = if (original.isBlank()) 0 else original.trim().split(Regex("\\s+")).size
        val transWords = if (transformed.isBlank()) 0 else transformed.trim().split(Regex("\\s+")).size
        val delta = transWords - origWords

        return when (preset) {
            TransformPreset.FIX -> "Polished grammar & spelling"
            TransformPreset.CONCISE -> if (delta < 0) "Trimmed ${-delta} word${if (-delta > 1) "s" else ""}" else "Streamlined phrasing"
            TransformPreset.PROFESSIONAL -> "Refined formal vocabulary"
            TransformPreset.PUNCHY -> "Sharpened energetic impact"
            TransformPreset.FRIENDLY -> "Warmed up the tone"
            TransformPreset.SUMMARIZE -> "Approximate offline summary"
            TransformPreset.BULLETIZE -> "Restructured into bullets"
            TransformPreset.EXPAND -> "Modest offline expansion"
            TransformPreset.CUSTOM -> "Custom instructions need an AI engine — applied Fix & Polish"
        }
    }
}
