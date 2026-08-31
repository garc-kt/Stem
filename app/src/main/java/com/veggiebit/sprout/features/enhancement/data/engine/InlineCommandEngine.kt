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
            val previous = UndoManager.popUndo(nodeHashCode) ?: UndoManager.popUndo()
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
            Regex("(?:\\?punchy|\\.punchy)$", RegexOption.IGNORE_CASE) to TransformPreset.PUNCHY
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
                    }
                    return CommandResult.Replaced(transformed, "Applied ${preset.title}")
                }
            }
        }

        return CommandResult.None
    }

    private fun evaluateMath(expr: String): Double? {
        return try {
            val tokens = expr.replace(" ", "")
            if (tokens.isEmpty()) return null

            val numberBuffer = StringBuilder()
            var i = 0
            val numbers = mutableListOf<Double>()
            val ops = mutableListOf<Char>()

            while (i < tokens.length) {
                val c = tokens[i]
                if (c.isDigit() || c == '.') {
                    numberBuffer.append(c)
                } else if (c in listOf('+', '-', '*', '/', '%')) {
                    if (numberBuffer.isNotEmpty()) {
                        numbers.add(numberBuffer.toString().toDouble())
                        numberBuffer.clear()
                    }
                    ops.add(c)
                }
                i++
            }
            if (numberBuffer.isNotEmpty()) {
                numbers.add(numberBuffer.toString().toDouble())
            }

            if (numbers.isEmpty()) return null
            if (numbers.size == 1 && ops.isEmpty()) return numbers[0]

            var idx = 0
            while (idx < ops.size) {
                val op = ops[idx]
                if (op == '*' || op == '/' || op == '%') {
                    val n1 = numbers[idx]
                    val n2 = numbers[idx + 1]
                    val res = when (op) {
                        '*' -> n1 * n2
                        '/' -> if (n2 != 0.0) n1 / n2 else return null
                        '%' -> n1 % n2
                        else -> n1
                    }
                    numbers[idx] = res
                    numbers.removeAt(idx + 1)
                    ops.removeAt(idx)
                } else {
                    idx++
                }
            }

            var total = numbers[0]
            for (j in ops.indices) {
                val op = ops[j]
                val next = numbers[j + 1]
                total = when (op) {
                    '+' -> total + next
                    '-' -> total - next
                    else -> total
                }
            }
            total
        } catch (_: Exception) {
            null
        }
    }
}
