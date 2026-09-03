package com.stem.engine

import com.stem.core.models.TransformPreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



object InlineCommandEngine {

    sealed class CommandResult {
        data class Replaced(val newText: String, val summary: String) : CommandResult()
        data class RunAIPreset(val body: String, val preset: TransformPreset, val summary: String) : CommandResult()
        data class RunAIPrompt(val body: String, val customPrompt: String, val summary: String) : CommandResult()
        data class SaveSnippet(val key: String, val expansion: String, val cleanedText: String) : CommandResult()
        data class SaveCustomCommand(val trigger: String, val prompt: String, val cleanedText: String) : CommandResult()
        /** The `?undo`/`.undo` trigger matched and history has an entry to restore. Carries no
         * popped value — evaluate() only reports availability (via the read-only
         * [TransformHistory.canUndo]) so it stays a pure function; the caller pops exactly once,
         * only when it actually commits to injecting the restored text. */
        data class Undo(val nodeHashCode: Int) : CommandResult()
        object None : CommandResult()
    }

    private val cmdRegex = Regex("(?:\\.\\.cmd:|(?<=\\s|^)\\.cmd:|\\?cmd:|\\.\\.savecmd:|;;cmd:|;;savecmd:)([a-zA-Z0-9_-]+):(.+)$", RegexOption.IGNORE_CASE)
    private val saveRegex = Regex("(?:\\.\\.save:|(?<=\\s|^)\\.save:|;;save:)([a-zA-Z0-9_-]+):(.+)$", RegexOption.IGNORE_CASE)
    private val dynamicAIRegex = Regex("(?:\\?ai:|\\?prompt:|\\?do:|(?<=\\s|^)\\.ai:|;;ai:|;;prompt:|;;do:)\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val calcRegex = Regex("(?:\\?calc:|(?<=\\s|^)\\.c:|;;calc:|;;c:)\\s*([0-9+\\-*/().^%\\s]+)$", RegexOption.IGNORE_CASE)
    private val triggerMap = listOf(
        Regex("(?:\\?fix|(?<=\\s|^)\\.fix|;;fix)$", RegexOption.IGNORE_CASE) to TransformPreset.FIX,
        Regex("(?:\\?concise|(?<=\\s|^)\\.concise|\\?shorten|(?<=\\s|^)\\.shorten|;;concise|;;shorten)$", RegexOption.IGNORE_CASE) to TransformPreset.CONCISE,
        Regex("(?:\\?formal|(?<=\\s|^)\\.formal|\\?prof|(?<=\\s|^)\\.prof|;;formal|;;prof)$", RegexOption.IGNORE_CASE) to TransformPreset.PROFESSIONAL,
        Regex("(?:\\?punchy|(?<=\\s|^)\\.punchy|;;punchy)$", RegexOption.IGNORE_CASE) to TransformPreset.PUNCHY,
        Regex("(?:\\?friendly|(?<=\\s|^)\\.friendly|;;friendly)$", RegexOption.IGNORE_CASE) to TransformPreset.FRIENDLY,
        Regex("(?:\\?summarize|(?<=\\s|^)\\.summarize|\\?summary|(?<=\\s|^)\\.summary|;;summarize|;;summary)$", RegexOption.IGNORE_CASE) to TransformPreset.SUMMARIZE,
        Regex("(?:\\?bullets|(?<=\\s|^)\\.bullets|\\?bulletize|(?<=\\s|^)\\.bulletize|;;bullets|;;bulletize)$", RegexOption.IGNORE_CASE) to TransformPreset.BULLETIZE,
        Regex("(?:\\?expand|(?<=\\s|^)\\.expand|;;expand)$", RegexOption.IGNORE_CASE) to TransformPreset.EXPAND
    )

    private fun matchesSingleDotTrigger(text: String, command: String): Boolean {
        val target = ".$command"
        if (!text.endsWith(target, ignoreCase = true)) return false
        val charBeforeDotIndex = text.length - target.length - 1
        return charBeforeDotIndex < 0 || text[charBeforeDotIndex].isWhitespace()
    }

    fun evaluate(
        text: String,
        nodeHashCode: Int,
        snippets: Map<String, String> = emptyMap(),
        customCommands: Map<String, String> = emptyMap()
    ): CommandResult {
        val trimmed = text.trimEnd()

        // Fast bail: every trigger recognized below (;;cmd:, ..key, ?fix, ;;fix, ?undo, ;;undo, ?now, ;;calc:,
        // custom commands, ...) requires a literal '.', '?', or ';' somewhere in the trimmed text.
        // This runs once per keystroke on the accessibility hot path, so skipping the entire
        // regex/suffix cascade for ordinary prose (the overwhelming majority of keystrokes)
        // is worth a single linear scan.
        if (trimmed.none { it == '.' || it == '?' || it == ';' }) {
            return CommandResult.None
        }

        val cmdMatch = cmdRegex.find(trimmed)
        if (cmdMatch != null) {
            val trigger = cmdMatch.groupValues[1]
            val prompt = cmdMatch.groupValues[2]
            val prefix = trimmed.substring(0, cmdMatch.range.first).trimEnd()
            return CommandResult.SaveCustomCommand(trigger, prompt, prefix)
        }

        val saveMatch = saveRegex.find(trimmed)
        if (saveMatch != null) {
            val key = saveMatch.groupValues[1]
            val expansion = saveMatch.groupValues[2]
            val prefix = trimmed.substring(0, saveMatch.range.first).trimEnd()
            return CommandResult.SaveSnippet(key, expansion, prefix)
        }

        val dynamicMatch = dynamicAIRegex.find(trimmed)
        if (dynamicMatch != null) {
            val prompt = dynamicMatch.groupValues[1].trim()
            val body = trimmed.substring(0, dynamicMatch.range.first).trimEnd()
            if (prompt.isNotBlank() && body.isNotBlank()) {
                return CommandResult.RunAIPrompt(body, prompt, "?ai")
            }
        }

        for ((trigger, prompt) in customCommands) {
            val semiTrigger = ";;$trigger"
            val qTrigger = "?$trigger"
            val dotDotTrigger = "..$trigger"
            val matchedTrigger = when {
                trimmed.endsWith(semiTrigger, ignoreCase = true) -> semiTrigger
                trimmed.endsWith(qTrigger, ignoreCase = true) -> qTrigger
                trimmed.endsWith(dotDotTrigger, ignoreCase = true) -> dotDotTrigger
                matchesSingleDotTrigger(trimmed, trigger) -> ".$trigger"
                else -> null
            }
            if (matchedTrigger != null) {
                val body = trimmed.substring(0, trimmed.length - matchedTrigger.length).trimEnd()
                if (body.isNotBlank()) {
                    return CommandResult.RunAIPrompt(body, prompt, matchedTrigger)
                }
            }
        }

        for ((key, expansion) in snippets) {
            val semiTrigger = ";;$key"
            val dotDotTrigger = "..$key"
            val matchedTrigger = when {
                trimmed.endsWith(semiTrigger, ignoreCase = true) -> semiTrigger
                trimmed.endsWith(dotDotTrigger, ignoreCase = true) -> dotDotTrigger
                matchesSingleDotTrigger(trimmed, key) -> ".$key"
                else -> null
            }
            if (matchedTrigger != null) {
                val prefix = trimmed.substring(0, trimmed.length - matchedTrigger.length).trimEnd()
                val newText = if (prefix.isEmpty()) expansion else "$prefix $expansion"
                return CommandResult.Replaced(newText, matchedTrigger)
            }
        }

        val hasUndoTrigger = trimmed.endsWith("?undo", ignoreCase = true) ||
            trimmed.endsWith(";;undo", ignoreCase = true) ||
            matchesSingleDotTrigger(trimmed, "undo")
        if (hasUndoTrigger) {
            if (TransformHistory.canUndo(nodeHashCode) || TransformHistory.canUndo()) {
                return CommandResult.Undo(nodeHashCode)
            }
        }

        val nowMatch = listOf(";;now", "?now").firstOrNull { trimmed.endsWith(it, ignoreCase = true) }
            ?: if (matchesSingleDotTrigger(trimmed, "now")) ".now" else null
        if (nowMatch != null) {
            val formatted = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
            val prefix = trimmed.substring(0, trimmed.length - nowMatch.length).trimEnd()
            val newText = if (prefix.isEmpty()) formatted else "$prefix $formatted"
            return CommandResult.Replaced(newText, nowMatch)
        }

        val dateMatch = listOf(";;date", "?date").firstOrNull { trimmed.endsWith(it, ignoreCase = true) }
            ?: if (matchesSingleDotTrigger(trimmed, "date")) ".date" else null
        if (dateMatch != null) {
            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val prefix = trimmed.substring(0, trimmed.length - dateMatch.length).trimEnd()
            val newText = if (prefix.isEmpty()) formatted else "$prefix $formatted"
            return CommandResult.Replaced(newText, dateMatch)
        }

        val calcMatch = calcRegex.find(trimmed)
        if (calcMatch != null) {
            val expr = calcMatch.groupValues[1].trim()
            val resultVal = evaluateMath(expr)
            if (resultVal != null) {
                val prefix = trimmed.substring(0, calcMatch.range.first).trimEnd()
                val formattedVal = if (resultVal == resultVal.toLong().toDouble()) resultVal.toLong().toString() else resultVal.toString()
                val newText = if (prefix.isEmpty()) formattedVal else "$prefix $formattedVal"
                return CommandResult.Replaced(newText, "?calc")
            }
        }

        for ((regex, preset) in triggerMap) {
            val match = regex.find(trimmed)
            if (match != null) {
                val body = trimmed.substring(0, match.range.first).trimEnd()
                if (body.isNotBlank()) {
                    return CommandResult.RunAIPreset(body, preset, "?${preset.id}")
                }
            }
        }

        return CommandResult.None
    }

    private sealed class MathToken {
        data class Num(val value: Double) : MathToken()
        data class Op(val symbol: Char) : MathToken()
    }

    private fun tokenizeMath(expr: String): List<MathToken>? {
        val s = expr.replace(" ", "")
        if (s.isEmpty()) return null
        val tokens = mutableListOf<MathToken>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    val value = s.substring(start, i).toDoubleOrNull() ?: return null
                    tokens.add(MathToken.Num(value))
                }
                c in "+-*/%^()" -> {
                    tokens.add(MathToken.Op(c))
                    i++
                }
                else -> return null
            }
        }
        return tokens
    }

    private class MathParser(private val tokens: List<MathToken>) {
        private var pos = 0
        private fun peek(): MathToken? = tokens.getOrNull(pos)

        fun parse(): Double? {
            val result = parseExpr() ?: return null
            return if (pos == tokens.size) result else null
        }

        private fun parseExpr(): Double? {
            var left = parseTerm() ?: return null
            while (true) {
                val tok = peek()
                if (tok is MathToken.Op && (tok.symbol == '+' || tok.symbol == '-')) {
                    pos++
                    val right = parseTerm() ?: return null
                    left = if (tok.symbol == '+') left + right else left - right
                } else break
            }
            return left
        }

        private fun parseTerm(): Double? {
            var left = parsePower() ?: return null
            while (true) {
                val tok = peek()
                if (tok is MathToken.Op && (tok.symbol == '*' || tok.symbol == '/' || tok.symbol == '%')) {
                    pos++
                    val right = parsePower() ?: return null
                    left = when (tok.symbol) {
                        '*' -> left * right
                        '/' -> if (right != 0.0) left / right else return null
                        '%' -> if (right != 0.0) left % right else return null
                        else -> return null
                    }
                } else break
            }
            return left
        }

        private fun parsePower(): Double? {
            val base = parseUnary() ?: return null
            val tok = peek()
            return if (tok is MathToken.Op && tok.symbol == '^') {
                pos++
                val exponent = parsePower() ?: return null
                Math.pow(base, exponent)
            } else {
                base
            }
        }

        private fun parseUnary(): Double? {
            val tok = peek()
            if (tok is MathToken.Op && tok.symbol == '-') {
                pos++
                return parseUnary()?.let { -it }
            }
            if (tok is MathToken.Op && tok.symbol == '+') {
                pos++
                return parseUnary()
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Double? {
            val tok = peek() ?: return null
            return when {
                tok is MathToken.Num -> {
                    pos++
                    tok.value
                }
                tok is MathToken.Op && tok.symbol == '(' -> {
                    pos++
                    val value = parseExpr() ?: return null
                    val closing = peek()
                    if (closing is MathToken.Op && closing.symbol == ')') {
                        pos++
                        value
                    } else {
                        null
                    }
                }
                else -> null
            }
        }
    }

    private fun evaluateMath(expr: String): Double? {
        return try {
            val tokens = tokenizeMath(expr) ?: return null
            if (tokens.isEmpty()) return null
            MathParser(tokens).parse()
        } catch (_: Exception) {
            null
        }
    }
}
