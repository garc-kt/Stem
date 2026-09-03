package com.stem.engine

import com.stem.core.models.LanguagePreference
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import com.stem.core.models.TransformResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap



enum class DetectedLanguage { ENGLISH, SPANISH, PORTUGUESE }

object LanguageDetector {
    private val portugueseDistinctAccents = "ãõçêôàÃÕÇÊÔÀ"
    private val spanishDistinctAccents = "ñÑ¿¡"
    private val sharedLatinAccents = "áéíóúÁÉÍÓÚ"

    private val spanishStopwords = setOf(
        "el", "la", "los", "las", "de", "que", "y", "en", "un", "una", "es", "por", "con",
        "para", "no", "se", "su", "al", "lo", "como", "mas", "más", "pero", "sus", "le", "ya",
        "o", "este", "si", "sí", "porque", "esta", "entre", "cuando", "muy", "sin", "sobre",
        "tambien", "también", "me", "hasta", "hay", "donde", "quien", "desde", "todo", "nos",
        "durante", "uno", "les", "ni", "contra", "otros", "ese", "eso", "ante", "ellos", "e",
        "esto", "mi", "mí", "antes", "algunos", "unos", "yo", "otro", "otras", "otra",
        "tanto", "esa", "estos", "mucho", "quienes", "nada", "muchos", "cual", "poco",
        "ella", "estar", "estas", "algunas", "algo", "nosotros"
    )

    private val portugueseStopwords = setOf(
        "o", "a", "os", "as", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
        "em", "no", "na", "nos", "nas", "por", "pelo", "pela", "pelos", "pelas", "pra", "pro",
        "para", "com", "sem", "que", "e", "ou", "mas", "se", "como", "quando", "muito",
        "mais", "já", "também", "tambem", "você", "voce", "vc", "ele", "ela", "eles", "elas",
        "nós", "eu", "isso", "esse", "essa", "este", "esta", "foi", "são", "ser", "ter",
        "está", "estou", "não", "nao", "porque", "pq", "tbm", "agora", "agr", "beleza", "obrigado"
    )

    private val englishStopwords = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not",
        "on", "with", "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they",
        "we", "say", "her", "she", "or", "an", "will", "my", "one", "all", "would", "there",
        "their", "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me"
    )

    private val wordSplitRegex = Regex("[^\\p{L}']+")

    fun detect(text: String): DetectedLanguage {
        if (text.isBlank()) return DetectedLanguage.ENGLISH

        val length = text.length.toDouble()
        val ptDistinctDensity = text.count { it in portugueseDistinctAccents } / length
        if (ptDistinctDensity > 0.005) return DetectedLanguage.PORTUGUESE

        val esDistinctDensity = text.count { it in spanishDistinctAccents } / length
        if (esDistinctDensity > 0.005) return DetectedLanguage.SPANISH

        val words = text.lowercase().split(wordSplitRegex).filter { it.isNotBlank() }
        if (words.isEmpty()) return DetectedLanguage.ENGLISH

        val portugueseHits = words.count { it in portugueseStopwords }
        val spanishHits = words.count { it in spanishStopwords }
        val englishHits = words.count { it in englishStopwords }

        if (portugueseHits > englishHits && portugueseHits >= spanishHits) return DetectedLanguage.PORTUGUESE
        if (spanishHits > englishHits && spanishHits > portugueseHits) return DetectedLanguage.SPANISH

        val accentDensity = text.count { it in sharedLatinAccents } / length
        if (accentDensity > 0.01) {
            return if (portugueseHits >= spanishHits) DetectedLanguage.PORTUGUESE else DetectedLanguage.SPANISH
        }

        return DetectedLanguage.ENGLISH
    }
}

object LocalRuleEngine : TextEngine {

    fun resolveRules(text: String, preference: LanguagePreference): LanguageRules {
        val effective = when (preference) {
            LanguagePreference.AUTO -> LanguageDetector.detect(text)
            LanguagePreference.ENGLISH -> DetectedLanguage.ENGLISH
            LanguagePreference.SPANISH -> DetectedLanguage.SPANISH
            LanguagePreference.PORTUGUESE -> DetectedLanguage.PORTUGUESE
        }
        return when (effective) {
            DetectedLanguage.SPANISH -> SpanishRules
            DetectedLanguage.PORTUGUESE -> PortugueseRules
            DetectedLanguage.ENGLISH -> EnglishRules
        }
    }

    private const val PROTECT_OPEN = '\uE000'
    private const val PROTECT_CLOSE = '\uE001'
    private val restoreSpansRegex = Regex("$PROTECT_OPEN(\\d+)$PROTECT_CLOSE")

    private val sharedProtectedPatterns = listOf(
        Regex("[\\w.+-]+@[\\w-]+\\.[\\w.-]+"),
        Regex("https?://\\S+", RegexOption.IGNORE_CASE),
        Regex("\\bwww\\.[\\w-]+(?:\\.[\\w-]+)+\\S*", RegexOption.IGNORE_CASE),
        Regex("[@#][A-Za-z0-9_]+"),
        Regex("\\d+\\.\\d+")
    )

    // --- Regexes hoisted to compile once at class-init instead of once per transform() call.
    // LocalRuleEngine runs on the accessibility hot path (every matching keystroke), so a fresh
    // Regex() per call across every rule below was real, avoidable per-call cost.
    private val whitespaceCollapseRegex = Regex("[ \\t]+")
    private val whitespaceSplitRegex = Regex("\\s+")
    private val spaceBeforePunctuationRegex = Regex("\\s+([,.:;?!])")
    private val punctuationSpaceAfterRegex = Regex("([,.:;?!])([a-zA-Z])")
    private val bareIRegex = Regex("\\bi\\b")
    private val imRegex = Regex("\\bi'm\\b", RegexOption.IGNORE_CASE)
    private val illRegex = Regex("\\bi'll\\b", RegexOption.IGNORE_CASE)
    private val iveRegex = Regex("\\bi've\\b", RegexOption.IGNORE_CASE)
    private val idRegex = Regex("\\bi'd\\b", RegexOption.IGNORE_CASE)
    private val duplicateWordRegex = Regex("\\b(\\w+)\\s+\\1\\b", RegexOption.IGNORE_CASE)

    private val fillers = listOf("really", "basically", "literally", "actually", "honestly", "needless to say,")
    private val fillerRegexes = fillers.map { Regex("\\b$it\\s+", RegexOption.IGNORE_CASE) }

    private val thanksALotRegex = Regex("\\b(thanks a lot|thanks so much)\\b", RegexOption.IGNORE_CASE)
    private val canYouPleaseRegex = Regex("\\b(can you please)\\b", RegexOption.IGNORE_CASE)
    private val giveMeACallRegex = Regex("\\b(give me a call)\\b", RegexOption.IGNORE_CASE)

    private val iJustWantedToRegex = Regex("\\bi just wanted to\\b", RegexOption.IGNORE_CASE)
    private val tryToRegex = Regex("\\btry to\\b", RegexOption.IGNORE_CASE)

    private val pleaseNoteThatRegex = Regex("\\b(please note that)\\b", RegexOption.IGNORE_CASE)
    private val iRegretToInformYouRegex = Regex("\\b(I regret to inform you)\\b", RegexOption.IGNORE_CASE)
    private val isRequiredRegex = Regex("\\b(is required)\\b", RegexOption.IGNORE_CASE)

    private val signalWords = listOf("must", "important", "deadline", "urgent", "asap", "required", "critical")
    private val signalRegex = Regex("\\b(${signalWords.joinToString("|")})\\b", RegexOption.IGNORE_CASE)
    private val hasDigitRegex = Regex("\\d")

    private val bulletizeSplitRegex = Regex("(?:,?\\s+and then\\s+|;\\s*)", RegexOption.IGNORE_CASE)
    private val sentenceSplitRegex = Regex("(?<=[.!?])\\s+")

    private val expansions = mapOf(
        "asap" to "as soon as possible",
        "btw" to "by the way",
        "fyi" to "for your information",
        "thx" to "thank you",
        "wanna" to "want to",
        "gonna" to "going to",
        "gotta" to "have got to"
    )

    // Compiled-pattern caches for replacePreservingCase/replaceFormalGuarded, whose regex
    // depends on the dictionary entry's `target` string rather than being a fixed literal —
    // these can't be hoisted to a single val above, but the pattern for a given target never
    // changes, so compiling it once (on first use) and reusing it thereafter still eliminates
    // the per-call recompilation. Plain-match patterns don't depend on which LanguageRules is
    // active, so a flat cache keyed by target is safe; guarded patterns additionally depend on
    // rules.phrasalVerbGuards, so that cache is keyed by (rules, target).
    // LocalRuleEngine is a singleton object whose transform() runs on Dispatchers.Default (a
    // real multi-threaded pool), reachable concurrently from the accessibility service and
    // ProcessTextActivity — a plain HashMap mutated via getOrPut is not safe under concurrent
    // structural modification (the earlier per-call `Regex(...)` construction held no shared
    // state, so it never needed this).
    private val plainPatternCache = ConcurrentHashMap<String, Regex>()
    private val guardedPatternCache = ConcurrentHashMap<Pair<LanguageRules, String>, Regex>()

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
        return restoreSpansRegex.replace(input) { match ->
            match.groupValues[1].toIntOrNull()?.let { idx -> saved.getOrNull(idx) } ?: match.value
        }
    }

    override suspend fun transform(
        payload: TextPayload,
        preset: TransformPreset,
        languagePreference: LanguagePreference
    ): TransformResult {
        val preference = languagePreference
        return withContext(Dispatchers.Default) {
            val original = payload.text
            if (original.isBlank()) {
                return@withContext TransformResult(original, original, preset, emptyList())
            }

            val rules = resolveRules(original, preference)
            val transformed = when (preset) {
                TransformPreset.FIX -> applyFixAndPolish(original, rules)
                TransformPreset.CONCISE -> applyConcise(original, rules)
                TransformPreset.PROFESSIONAL -> applyProfessional(original, rules)
                TransformPreset.PUNCHY -> applyPunchy(original, rules)
                TransformPreset.FRIENDLY -> applyFriendly(original, rules)
                TransformPreset.SUMMARIZE -> applySummarize(original, rules)
                TransformPreset.BULLETIZE -> applyBulletize(original, rules)
                TransformPreset.EXPAND -> applyExpand(original, rules)
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
    }

    fun applyFixAndPolish(input: String, rules: LanguageRules): String {
        val (protectedInput, savedSpans) = protectSpans(input, rules)
        var text = protectedInput

        text = text.replace(spaceBeforePunctuationRegex, "$1")
        text = text.replace(punctuationSpaceAfterRegex, "$1 $2")
        text = text.replace(whitespaceCollapseRegex, " ")

        for ((wrong, right) in rules.typoDictionary) {
            text = replacePreservingCase(text, wrong, right)
        }

        text = text.replace(bareIRegex, "I")
        text = text.replace(imRegex, "I'm")
        text = text.replace(illRegex, "I'll")
        text = text.replace(iveRegex, "I've")
        text = text.replace(idRegex, "I'd")

        text = text.replace(duplicateWordRegex, "$1")
        text = capitalizeSentences(text)

        if (text.length > 3 && !text.endsWith(".") && !text.endsWith("!") && !text.endsWith("?")) {
            val words = text.trim().split(whitespaceSplitRegex)
            if (words.size >= 3) {
                text += "."
            }
        }

        text = restoreSpans(text, savedSpans)
        text = rules.applyLanguageSpecificFixes(text)
        return text.trim()
    }

    fun applyConcise(input: String, rules: LanguageRules): String {
        var text = applyFixAndPolish(input, rules)

        for ((wordy, concise) in rules.wordyPhrases) {
            text = replacePreservingCase(text, wordy, concise)
        }

        for (fillerRegex in fillerRegexes) {
            text = text.replace(fillerRegex, "")
        }

        text = text.replace(whitespaceCollapseRegex, " ")
        text = text.replace(spaceBeforePunctuationRegex, "$1")
        text = capitalizeSentencesGuarded(text, rules)

        return text.trim()
    }

    fun applyProfessional(input: String, rules: LanguageRules): String {
        var text = applyFixAndPolish(input, rules)

        for ((casual, formal) in rules.formalReplacements) {
            text = replaceFormalGuarded(text, casual, formal, rules)
        }

        text = text.replace(thanksALotRegex, "Thank you very much")
        text = text.replace(canYouPleaseRegex, "Could you please")
        text = text.replace(giveMeACallRegex, "reach out via phone")

        text = capitalizeSentencesGuarded(text, rules)
        return text.trim()
    }

    fun applyPunchy(input: String, rules: LanguageRules): String {
        var text = applyFixAndPolish(input, rules)

        for ((starter, punchy) in rules.punchyStarters) {
            text = replacePreservingCase(text, starter, punchy)
        }

        for ((word, punchy) in rules.punchyWords) {
            text = replacePreservingCase(text, word, punchy)
        }

        text = text.replace(iJustWantedToRegex, "I wanted to")
        text = text.replace(tryToRegex, "")

        text = text.replace(whitespaceCollapseRegex, " ")
        text = capitalizeSentencesGuarded(text, rules)
        return text.trim()
    }

    fun applyFriendly(input: String, rules: LanguageRules): String {
        var text = applyFixAndPolish(input, rules)

        for ((formal, casual) in rules.friendlyReplacements) {
            text = replacePreservingCase(text, formal, casual)
        }

        text = text.replace(pleaseNoteThatRegex, "just so you know,")
        text = text.replace(iRegretToInformYouRegex, "I'm sorry to say")
        text = text.replace(isRequiredRegex, "is needed")

        text = capitalizeSentencesGuarded(text, rules)
        return text.trim()
    }

    fun applySummarize(input: String, rules: LanguageRules): String {
        val polished = applyFixAndPolish(input, rules)
        val sentences = splitSentences(polished)
        if (sentences.size <= 2) return polished

        val kept = mutableListOf(sentences.first())
        for (sentence in sentences.drop(1)) {
            if (signalRegex.containsMatchIn(sentence) || hasDigitRegex.containsMatchIn(sentence)) {
                kept.add(sentence)
            }
        }
        return kept.joinToString(" ").trim()
    }

    fun applyBulletize(input: String, rules: LanguageRules): String {
        val polished = applyFixAndPolish(input, rules)
        val sentences = splitSentences(polished)
        if (sentences.isEmpty()) return polished

        return sentences.flatMap { sentence ->
            sentence.split(bulletizeSplitRegex)
        }
            .map { it.trim().trimEnd('.', ' ') }
            .filter { it.isNotBlank() }
            .joinToString("\n") { "• $it" }
    }

    fun applyExpand(input: String, rules: LanguageRules): String {
        var text = applyFixAndPolish(input, rules)

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
        return sentenceSplitRegex.split(text.trim())
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /** Capitalizes sentence starts without corrupting protected spans (URLs, emails, handles,
     * decimals, abbreviations). Unlike [applyFixAndPolish], which capitalizes while its own
     * protected placeholders are still in place, CONCISE/PROFESSIONAL/PUNCHY/FRIENDLY need a
     * second capitalization pass *after* wordy-phrase/formality rewrites — by then
     * applyFixAndPolish has already restored spans to plain text, so capitalizing directly would
     * treat "example.com"'s dot as a sentence end and produce "example.Com". */
    private fun capitalizeSentencesGuarded(text: String, rules: LanguageRules): String {
        val (protectedText, saved) = protectSpans(text, rules)
        return restoreSpans(capitalizeSentences(protectedText), saved)
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
        // computeIfAbsent, not getOrPut: getOrPut is get-then-put, not atomic even on a
        // ConcurrentHashMap, so two racing threads could both compile the same pattern (harmless
        // here since Regex compilation is pure, but computeIfAbsent is genuinely atomic and just
        // as simple).
        val pattern = plainPatternCache.computeIfAbsent(target) {
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

    private fun replaceFormalGuarded(source: String, target: String, replacement: String, rules: LanguageRules): String {
        val pattern = guardedPatternCache.computeIfAbsent(rules to target) {
            val particles = rules.phrasalVerbGuards[target.lowercase()]
            if (particles != null) {
                Regex(
                    "\\b" + Regex.escape(target) + "\\b(?!\\s+(?:" + particles.joinToString("|") { Regex.escape(it) } + ")\\b)",
                    RegexOption.IGNORE_CASE
                )
            } else {
                Regex("\\b" + Regex.escape(target) + "\\b", RegexOption.IGNORE_CASE)
            }
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
        val origWords = if (original.isBlank()) 0 else original.trim().split(whitespaceSplitRegex).size
        val transWords = if (transformed.isBlank()) 0 else transformed.trim().split(whitespaceSplitRegex).size
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
