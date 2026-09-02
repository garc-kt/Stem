package com.stem.engine

import com.stem.engine.DiffCalculator
import com.stem.core.models.DiffType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test



class DiffCalculatorTest {

    @Test
    fun testIdenticalStringsProduceUnmodifiedDiff() {
        val original = "Hello world"
        val transformed = "Hello world"
        val diff = DiffCalculator.calculateDiff(original, transformed)

        assertEquals(1, diff.size)
        assertEquals(DiffType.UNMODIFIED, diff[0].type)
        assertEquals("Hello world", diff[0].text)
    }

    @Test
    fun testWordReplacementDiff() {
        val original = "teh meeting is tommorow"
        val transformed = "The meeting is tomorrow"
        val diff = DiffCalculator.calculateDiff(original, transformed)

        assertTrue(diff.any { it.type == DiffType.DELETED && it.text.contains("teh") })
        assertTrue(diff.any { it.type == DiffType.ADDED && it.text.contains("The") })
        assertTrue(diff.any { it.type == DiffType.DELETED && it.text.contains("tommorow") })
        assertTrue(diff.any { it.type == DiffType.ADDED && it.text.contains("tomorrow") })
    }

    @Test
    fun testWordOmissionDiff() {
        val original = "in order to succeed"
        val transformed = "To succeed"
        val diff = DiffCalculator.calculateDiff(original, transformed)

        assertTrue(diff.any { it.type == DiffType.DELETED })
        assertTrue(diff.any { it.type == DiffType.ADDED })
    }

    @Test
    fun testInputAboveTokenCapCompletesViaParagraphFallback() {
        // Deliberately exceeds MAX_DIFF_TOKENS (1200 per side) so this exercises the
        // paragraph-level fallback path instead of the O(m*n) word-token LCS matrix, which
        // previously allocated an unbounded matrix (~36MB for a 3,000-token paste).
        val originalParagraph = (1..3000).joinToString(" ") { "original" }
        val transformedParagraph = (1..3000).joinToString(" ") { "changed" }
        val original = "$originalParagraph\nSecond paragraph is unchanged."
        val transformed = "$transformedParagraph\nSecond paragraph is unchanged."

        val diff = DiffCalculator.calculateDiff(original, transformed)

        assertTrue(diff.isNotEmpty())
        assertTrue(diff.any { it.type == DiffType.DELETED })
        assertTrue(diff.any { it.type == DiffType.ADDED })
        assertTrue(diff.any { it.type == DiffType.UNMODIFIED && it.text.contains("Second paragraph is unchanged") })
    }

    @Test
    fun testInputAboveBothCapsDegradesToWholeBlockDiffWithoutHanging() {
        // Exceeds MAX_DIFF_TOKENS on both the word axis AND the paragraph axis (many short
        // lines), so neither LCS matrix is safe to allocate. This must still return promptly
        // with a coarse two-block diff rather than attempt an unbounded matrix.
        val original = (1..2000).joinToString("\n") { "original line $it" }
        val transformed = (1..2000).joinToString("\n") { "changed line $it" }

        val diff = DiffCalculator.calculateDiff(original, transformed)

        assertEquals(2, diff.size)
        assertEquals(DiffType.DELETED, diff[0].type)
        assertEquals(original, diff[0].text)
        assertEquals(DiffType.ADDED, diff[1].type)
        assertEquals(transformed, diff[1].text)
    }
}

