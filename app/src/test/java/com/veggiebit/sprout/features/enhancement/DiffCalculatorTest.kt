package com.veggiebit.sprout.features.enhancement

import com.veggiebit.sprout.features.enhancement.data.engine.DiffCalculator
import com.veggiebit.sprout.features.enhancement.data.models.DiffType
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
}
