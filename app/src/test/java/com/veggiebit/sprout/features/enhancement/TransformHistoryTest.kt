package com.veggiebit.sprout.features.enhancement

import com.veggiebit.sprout.features.enhancement.data.engine.TransformHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransformHistoryTest {

    @Before
    fun setUp() {
        TransformHistory.clear()
    }

    @Test
    fun testSnapshotsHaveUniqueIds() {
        TransformHistory.recordChange(1, "First Original", "First Replaced")
        TransformHistory.recordChange(2, "Second Original", "Second Replaced")

        val list = TransformHistory.history.value
        assertEquals(2, list.size)
        assertNotEquals(list[0].id, list[1].id)
        assertTrue(list[0].id.isNotBlank())
        assertTrue(list[1].id.isNotBlank())
    }

    @Test
    fun testDeduplicateConsecutiveIdenticalChanges() {
        TransformHistory.recordChange(100, "Same text", "Polished text")
        // Attempt immediate duplicate recording
        TransformHistory.recordChange(100, "Same text", "Polished text")

        val list = TransformHistory.history.value
        assertEquals(1, list.size)
    }

    @Test
    fun testIgnoreUnchangedText() {
        TransformHistory.recordChange(100, "Hello", "Hello")
        assertTrue(TransformHistory.history.value.isEmpty())
    }

    @Test
    fun testUndoPopsLastChange() {
        TransformHistory.recordChange(10, "A", "B")
        TransformHistory.recordChange(10, "B", "C")

        assertEquals("B", TransformHistory.popUndo(10))
        assertEquals("A", TransformHistory.popUndo(10))
        assertEquals(null, TransformHistory.popUndo(10))
    }
}
