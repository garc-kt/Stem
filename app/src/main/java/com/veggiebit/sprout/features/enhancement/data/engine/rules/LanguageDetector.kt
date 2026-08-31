package com.veggiebit.sprout.features.enhancement.data.engine.rules

enum class DetectedLanguage { ENGLISH, SPANISH }

/**
 * Lightweight heuristic language detector for [com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine]'s
 * AUTO mode — no ML model/dependency, just accented-character density plus a stopword-frequency
 * vote. Good enough to pick a dictionary; not a general-purpose language classifier.
 */
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

        // A single accented vowel or inverted punctuation mark is a strong, near-unambiguous
        // signal for Spanish among these two languages — check it before falling back to the
        // noisier stopword vote (which needs several words to be reliable).
        val accentDensity = text.count { it in spanishAccentedChars }.toDouble() / text.length
        if (accentDensity > 0.01) return DetectedLanguage.SPANISH

        val words = text.lowercase().split(Regex("[^\\p{L}']+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return DetectedLanguage.ENGLISH

        val spanishHits = words.count { it in spanishStopwords }
        val englishHits = words.count { it in englishStopwords }

        return if (spanishHits > englishHits) DetectedLanguage.SPANISH else DetectedLanguage.ENGLISH
    }
}
