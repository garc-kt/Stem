package com.stem.engine

import com.stem.core.models.DiffToken
import com.stem.core.models.DiffType



/**
 * Computes word-level diffs between original and transformed text using Longest Common Subsequence (LCS).
 */
object DiffCalculator {

    private const val MAX_DIFF_TOKENS = 1200
    private val TOKEN_REGEX = Regex("(\\w+|\\s+|[^\\w\\s]+)")
    private val PARAGRAPH_REGEX = Regex("([^\\n]+\\n?|\\n)")

    private fun tokenize(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val tokens = mutableListOf<String>()
        for (match in TOKEN_REGEX.findAll(text)) {
            tokens.add(match.value)
        }
        return tokens
    }

    private fun tokenizeParagraphs(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val tokens = mutableListOf<String>()
        for (match in PARAGRAPH_REGEX.findAll(text)) {
            tokens.add(match.value)
        }
        return tokens
    }

    fun calculateDiff(original: String, transformed: String): List<DiffToken> {
        if (original == transformed) {
            return if (original.isEmpty()) emptyList() else listOf(DiffToken(original, DiffType.UNMODIFIED))
        }
        if (original.isEmpty()) {
            return listOf(DiffToken(transformed, DiffType.ADDED))
        }
        if (transformed.isEmpty()) {
            return listOf(DiffToken(original, DiffType.DELETED))
        }

        val origTokens = tokenize(original)
        val transTokens = tokenize(transformed)

        return if (origTokens.size > MAX_DIFF_TOKENS || transTokens.size > MAX_DIFF_TOKENS) {
            lcsDiff(tokenizeParagraphs(original), tokenizeParagraphs(transformed))
        } else {
            lcsDiff(origTokens, transTokens)
        }
    }

    private fun lcsDiff(origTokens: List<String>, transTokens: List<String>): List<DiffToken> {
        val m = origTokens.size
        val n = transTokens.size

        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 1..m) {
            for (j in 1..n) {
                if (origTokens[i - 1] == transTokens[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        val rawDiff = mutableListOf<DiffToken>()
        var i = m
        var j = n

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && origTokens[i - 1] == transTokens[j - 1]) {
                rawDiff.add(DiffToken(origTokens[i - 1], DiffType.UNMODIFIED))
                i--
                j--
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                rawDiff.add(DiffToken(transTokens[j - 1], DiffType.ADDED))
                j--
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                rawDiff.add(DiffToken(origTokens[i - 1], DiffType.DELETED))
                i--
            }
        }

        rawDiff.reverse()

        val consolidated = mutableListOf<DiffToken>()
        for (token in rawDiff) {
            if (consolidated.isNotEmpty() && consolidated.last().type == token.type) {
                val last = consolidated.removeAt(consolidated.size - 1)
                consolidated.add(DiffToken(last.text + token.text, last.type))
            } else {
                consolidated.add(token)
            }
        }

        return consolidated
    }
}
