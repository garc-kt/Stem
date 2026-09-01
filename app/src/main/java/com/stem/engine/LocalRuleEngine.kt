package com.stem.engine

import com.stem.core.models.LanguagePreference
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import com.stem.core.models.TransformResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



enum class DetectedLanguage { ENGLISH, SPANISH }

object LanguageDetector {
    private val spanishAccentedChars = "áéíóúñÁÉÍÓÚÑ¿¡"

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

    private val englishStopwords = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not",
        "on", "with", "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they",
        "we", "say", "her", "she", "or", "an", "will", "my", "one", "all", "would", "there",
        "their", "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me"
    )

    fun detect(text: String): DetectedLanguage {
        if (text.isBlank()) return DetectedLanguage.ENGLISH

        val accentDensity = text.count { it in spanishAccentedChars }.toDouble() / text.length
        if (accentDensity > 0.01) return DetectedLanguage.SPANISH

        val words = text.lowercase().split(Regex("[^\\p{L}']+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return DetectedLanguage.ENGLISH

        val spanishHits = words.count { it in spanishStopwords }
        val englishHits = words.count { it in englishStopwords }

        return if (spanishHits > englishHits) DetectedLanguage.SPANISH else DetectedLanguage.ENGLISH
    }
}

object LocalRuleEngine : TextEngine {

    var languagePreference: LanguagePreference = LanguagePreference.AUTO

    fun resolveRules(text: String, preference: LanguagePreference): LanguageRules {
        val effective = when (preference) {
            LanguagePreference.AUTO -> LanguageDetector.detect(text)
            LanguagePreference.ENGLISH -> DetectedLanguage.ENGLISH
            LanguagePreference.SPANISH -> DetectedLanguage.SPANISH
        }
        return if (effective == DetectedLanguage.SPANISH) SpanishRules else EnglishRules
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

    override suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult {
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

    fun applyConcise(input: String, rules: LanguageRules): String {
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

    fun applyProfessional(input: String, rules: LanguageRules): String {
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

    fun applyPunchy(input: String, rules: LanguageRules): String {
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

    fun applyFriendly(input: String, rules: LanguageRules): String {
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

    fun applySummarize(input: String, rules: LanguageRules): String {
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

    fun applyBulletize(input: String, rules: LanguageRules): String {
        val polished = applyFixAndPolish(input, rules)
        val sentences = splitSentences(polished)
        if (sentences.isEmpty()) return polished

        return sentences.flatMap { sentence ->
            sentence.split(Regex("(?:,?\\s+and then\\s+|;\\s*)", RegexOption.IGNORE_CASE))
        }
            .map { it.trim().trimEnd('.', ' ') }
            .filter { it.isNotBlank() }
            .joinToString("\n") { "• $it" }
    }

    fun applyExpand(input: String, rules: LanguageRules): String {
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
