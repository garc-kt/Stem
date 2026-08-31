package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InlineCommandEngine {

    sealed class CommandResult {
        data class Replaced(val newText: String, val summary: String) : CommandResult()
        data class SaveSnippet(val key: String, val expansion: String, val cleanedText: String) : CommandResult()
        object None : CommandResult()
    }

    fun evaluate(
        text: String,
        nodeHashCode: Int,
        snippets: Map<String, String> = emptyMap()
    ): CommandResult {
        val trimmed = text.trimEnd()

        // 1. Quick Save Snippet: ..save:key:expansion or .save:key:expansion
        val saveRegex = Regex("(?:\\.\\.save:|\\.save:)([a-zA-Z0-9_-]+):(.+)$", RegexOption.IGNORE_CASE)
        val saveMatch = saveRegex.find(trimmed)
        if (saveMatch != null) {
            val key = saveMatch.groupValues[1]
            val expansion = saveMatch.groupValues[2]
            val prefix = trimmed.substring(0, saveMatch.range.first).trimEnd()
            return CommandResult.SaveSnippet(key, expansion, prefix)
        }

        // 2. Snippet Expansions: ..key or .key
        for ((key, expansion) in snippets) {
            val dotDotTrigger = "..$key"
            val dotTrigger = ".$key"
            if (trimmed.endsWith(dotDotTrigger, ignoreCase = true)) {
                val prefix = trimmed.substring(0, trimmed.length - dotDotTrigger.length).trimEnd()
                val newText = if (prefix.isEmpty()) expansion else "$prefix $expansion"
                return CommandResult.Replaced(newText, "Expanded snippet '$key'")
            } else if (trimmed.endsWith(dotTrigger, ignoreCase = true)) {
                val prefix = trimmed.substring(0, trimmed.length - dotTrigger.length).trimEnd()
                val newText = if (prefix.isEmpty()) expansion else "$prefix $expansion"
                return CommandResult.Replaced(newText, "Expanded snippet '$key'")
            }
        }

        // 3. Undo trigger: ?undo or .undo
        if (trimmed.endsWith("?undo", ignoreCase = true) || trimmed.endsWith(".undo", ignoreCase = true)) {
            val previous = TransformHistory.popUndo(nodeHashCode) ?: TransformHistory.popUndo()
            if (previous != null) {
                return CommandResult.Replaced(previous, "Undid last change")
            }
        }

        // 4. Date/Time triggers: ?now, ?date, .now, .date
        if (trimmed.endsWith("?now", ignoreCase = true) || trimmed.endsWith(".now", ignoreCase = true)) {
            val formatted = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
            val prefix = trimmed.removeSuffix("?now").removeSuffix("?Now").removeSuffix(".now").removeSuffix(".Now").trimEnd()
            val newText = if (prefix.isEmpty()) formatted else "$prefix $formatted"
            return CommandResult.Replaced(newText, "Inserted timestamp")
        }

        if (trimmed.endsWith("?date", ignoreCase = true) || trimmed.endsWith(".date", ignoreCase = true)) {
            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val prefix = trimmed.removeSuffix("?date").removeSuffix("?Date").removeSuffix(".date").removeSuffix(".Date").trimEnd()
            val newText = if (prefix.isEmpty()) formatted else "$prefix $formatted"
            return CommandResult.Replaced(newText, "Inserted date")
        }

        // 5. Calculator triggers: ?calc: 25 * 4 + 10 or .c: 25 * 4 + 10
        val calcRegex = Regex("(?:\\?calc:|\\.c:)\\s*([0-9+\\-*/().^%\\s]+)$", RegexOption.IGNORE_CASE)
        val calcMatch = calcRegex.find(trimmed)
        if (calcMatch != null) {
            val expr = calcMatch.groupValues[1].trim()
            val resultVal = evaluateMath(expr)
            if (resultVal != null) {
                val prefix = trimmed.substring(0, calcMatch.range.first).trimEnd()
                val formattedVal = if (resultVal == resultVal.toLong().toDouble()) resultVal.toLong().toString() else resultVal.toString()
                val newText = if (prefix.isEmpty()) formattedVal else "$prefix $formattedVal"
                return CommandResult.Replaced(newText, "Calculated $expr = $formattedVal")
            }
        }

        // 6. Preset triggers: ?fix, ?concise, ?shorten, ?formal, ?punchy
        val triggerMap = listOf(
            Regex("(?:\\?fix|\\.fix)$", RegexOption.IGNORE_CASE) to TransformPreset.FIX,
            Regex("(?:\\?concise|\\.concise|\\?shorten|\\.shorten)$", RegexOption.IGNORE_CASE) to TransformPreset.CONCISE,
            Regex("(?:\\?formal|\\.formal|\\?prof|\\.prof)$", RegexOption.IGNORE_CASE) to TransformPreset.PROFESSIONAL,
            Regex("(?:\\?punchy|\\.punchy)$", RegexOption.IGNORE_CASE) to TransformPreset.PUNCHY,
            Regex("(?:\\?friendly|\\.friendly)$", RegexOption.IGNORE_CASE) to TransformPreset.FRIENDLY,
            Regex("(?:\\?summarize|\\.summarize|\\?summary|\\.summary)$", RegexOption.IGNORE_CASE) to TransformPreset.SUMMARIZE,
            Regex("(?:\\?bullets|\\.bullets|\\?bulletize|\\.bulletize)$", RegexOption.IGNORE_CASE) to TransformPreset.BULLETIZE,
            Regex("(?:\\?expand|\\.expand)$", RegexOption.IGNORE_CASE) to TransformPreset.EXPAND
        )

        for ((regex, preset) in triggerMap) {
            val match = regex.find(trimmed)
            if (match != null) {
                val body = trimmed.substring(0, match.range.first).trimEnd()
                if (body.isNotBlank()) {
                    val transformed = when (preset) {
                        TransformPreset.FIX -> LocalRuleEngine.applyFixAndPolish(body)
                        TransformPreset.CONCISE -> LocalRuleEngine.applyConcise(body)
                        TransformPreset.PROFESSIONAL -> LocalRuleEngine.applyProfessional(body)
                        TransformPreset.PUNCHY -> LocalRuleEngine.applyPunchy(body)
                        TransformPreset.FRIENDLY -> LocalRuleEngine.applyFriendly(body)
                        TransformPreset.SUMMARIZE -> LocalRuleEngine.applySummarize(body)
                        TransformPreset.BULLETIZE -> LocalRuleEngine.applyBulletize(body)
                        TransformPreset.EXPAND -> LocalRuleEngine.applyExpand(body)
                        TransformPreset.CUSTOM -> LocalRuleEngine.applyFixAndPolish(body)
                    }
                    return CommandResult.Replaced(transformed, "Applied ${preset.title}")
                }
            }
        }

        return CommandResult.None
    }

    // --- Recursive-descent arithmetic parser -------------------------------------------
    // Replaces a previous flat two-pass scanner that silently dropped parentheses and '^',
    // producing wrong answers (e.g. "(2+3)*4" evaluated as "14" instead of 20). Grammar:
    //   expr   := term (('+' | '-') term)*
    //   term   := power (('*' | '/' | '%') power)*
    //   power  := unary ('^' power)?      -- right-associative
    //   unary  := ('-' | '+') unary | primary
    //   primary:= NUMBER | '(' expr ')'
    // Any malformed input (leftover tokens, unmatched parens, divide/mod by zero) yields null
    // so the inline trigger no-ops instead of injecting a wrong result.

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
