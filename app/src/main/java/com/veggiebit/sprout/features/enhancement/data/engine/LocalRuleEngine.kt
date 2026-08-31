package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance, 100% on-device rule and dictionary-based transformation engine.
 */
object LocalRuleEngine : TextEngine {

    private val typoDictionary = mapOf(
        "teh" to "the",
        "recieved" to "received",
        "seperate" to "separate",
        "definately" to "definitely",
        "untill" to "until",
        "truely" to "truly",
        "accomodate" to "accommodate",
        "occured" to "occurred",
        "tommorow" to "tomorrow",
        "alot" to "a lot",
        "beleive" to "believe",
        "goverment" to "government",
        "calender" to "calendar",
        "thier" to "their",
        "wierd" to "weird",
        "writting" to "writing",
        "embarass" to "embarrass",
        "adress" to "address",
        "recommand" to "recommend",
        "neccessary" to "necessary",
        "succesful" to "successful",
        "availible" to "available",
        "completly" to "completely",
        "peice" to "piece",
        "noone" to "no one",
        "alread" to "already",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "isnt" to "isn't",
        "didnt" to "didn't",
        "couldnt" to "couldn't",
        "shouldnt" to "shouldn't",
        "wouldnt" to "wouldn't",
        "thats" to "that's",
        "whats" to "what's",
        "theres" to "there's",
        "lets" to "let's",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "arent" to "aren't",
        "werent" to "weren't"
    )

    private val wordyPhrases = mapOf(
        "in order to" to "to",
        "due to the fact that" to "because",
        "at this point in time" to "now",
        "at the present time" to "now",
        "for the purpose of" to "to",
        "with regard to" to "regarding",
        "in the event that" to "if",
        "has the ability to" to "can",
        "is able to" to "can",
        "in spite of the fact that" to "although",
        "take into consideration" to "consider",
        "make a decision" to "decide",
        "give consideration to" to "consider",
        "a large number of" to "many",
        "a majority of" to "most",
        "at all times" to "always",
        "in close proximity to" to "near",
        "prior to" to "before",
        "subsequent to" to "after",
        "by means of" to "by",
        "in terms of" to "regarding",
        "as a matter of fact" to "in fact",
        "it is important to note that" to "note that",
        "each and every" to "every",
        "first and foremost" to "first",
        "basic fundamentals" to "fundamentals",
        "future plans" to "plans",
        "completely eliminate" to "eliminate",
        "absolutely essential" to "essential",
        "very unique" to "unique",
        "as per your request" to "as requested",
        "reach a consensus" to "agree"
    )

    private val formalReplacements = mapOf(
        "wanna" to "would like to",
        "gonna" to "will",
        "gotta" to "need to",
        "kinda" to "somewhat",
        "sorta" to "somewhat",
        "dunno" to "do not know",
        "btw" to "by the way",
        "asap" to "as soon as possible",
        "fyi" to "for your information",
        "thx" to "thank you",
        "thanks" to "thank you",
        "hey" to "hello",
        "yeah" to "yes",
        "yep" to "yes",
        "nope" to "no",
        "talk about" to "discuss",
        "give" to "provide",
        "fix" to "resolve",
        "help" to "assist",
        "ask" to "inquire",
        "buy" to "purchase",
        "get" to "obtain",
        "show" to "demonstrate",
        "tell" to "inform",
        "start" to "commence",
        "end" to "conclude",
        "make sure" to "ensure",
        "look into" to "investigate",
        "set up" to "configure",
        "find out" to "determine",
        "let me know" to "please advise",
        "sorry for" to "I apologize for",
        "can't" to "cannot",
        "won't" to "will not",
        "don't" to "do not",
        "didn't" to "did not",
        "couldn't" to "could not",
        "shouldn't" to "should not",
        "wouldn't" to "would not",
        "it's" to "it is",
        "that's" to "that is",
        "there's" to "there is",
        "what's" to "what is",
        "we're" to "we are",
        "they're" to "they are",
        "I'm" to "I am"
    )

    private val punchyStarters = mapOf(
        "i was thinking that maybe we could" to "Let's",
        "it would be great if we could" to "Let's",
        "we might want to consider" to "Let's",
        "there are many reasons why" to "Key reasons:",
        "i just wanted to check if" to "Checking in:",
        "i am writing this to let you know" to "Update:",
        "in my personal opinion" to "Honestly,",
        "feel free to" to "Please"
    )

    private val punchyWords = mapOf(
        "good" to "great",
        "bad" to "critical",
        "fast" to "rapid",
        "big" to "massive",
        "nice" to "superb",
        "important" to "vital",
        "hard" to "challenging",
        "very good" to "stellar",
        "really nice" to "exceptional",
        "make better" to "supercharge"
    )

    override suspend fun transform(payload: TextPayload, preset: TransformPreset): TransformResult = withContext(Dispatchers.Default) {
        val original = payload.text
        if (original.isBlank()) {
            return@withContext TransformResult(original, original, preset, emptyList())
        }

        val transformed = when (preset) {
            TransformPreset.FIX -> applyFixAndPolish(original)
            TransformPreset.CONCISE -> applyConcise(original)
            TransformPreset.PROFESSIONAL -> applyProfessional(original)
            TransformPreset.PUNCHY -> applyPunchy(original)
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

    fun applyFixAndPolish(input: String): String {
        var text = input

        text = text.replace(Regex("\\s+([,.:;?!])"), "$1")
        text = text.replace(Regex("([,.:;?!])([a-zA-Z])"), "$1 $2")
        text = text.replace(Regex("[ \\t]+"), " ")

        for ((wrong, right) in typoDictionary) {
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

        return text.trim()
    }

    fun applyConcise(input: String): String {
        var text = applyFixAndPolish(input)

        for ((wordy, concise) in wordyPhrases) {
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

    fun applyProfessional(input: String): String {
        var text = applyFixAndPolish(input)

        for ((casual, formal) in formalReplacements) {
            text = replacePreservingCase(text, casual, formal)
        }

        text = text.replace(Regex("\\b(thanks a lot|thanks so much)\\b", RegexOption.IGNORE_CASE), "Thank you very much")
        text = text.replace(Regex("\\b(can you please)\\b", RegexOption.IGNORE_CASE), "Could you please")
        text = text.replace(Regex("\\b(give me a call)\\b", RegexOption.IGNORE_CASE), "reach out via phone")

        text = capitalizeSentences(text)
        return text.trim()
    }

    fun applyPunchy(input: String): String {
        var text = applyFixAndPolish(input)

        for ((starter, punchy) in punchyStarters) {
            text = replacePreservingCase(text, starter, punchy)
        }

        for ((word, punchy) in punchyWords) {
            text = replacePreservingCase(text, word, punchy)
        }

        text = text.replace(Regex("\\bi just wanted to\\b", RegexOption.IGNORE_CASE), "I wanted to")
        text = text.replace(Regex("\\btry to\\b", RegexOption.IGNORE_CASE), "")

        text = text.replace(Regex("[ \\t]+"), " ")
        text = capitalizeSentences(text)
        return text.trim()
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
        }
    }
}
