package com.veggiebit.sprout.features.enhancement

import com.veggiebit.sprout.features.enhancement.data.engine.InlineCommandEngine
import com.veggiebit.sprout.features.enhancement.data.engine.UndoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InlineCommandEngineTest {

    @Before
    fun setUp() {
        UndoManager.clear()
    }

    @Test
    fun testFixTrigger() {
        val input = "i think teh meeting is tommorow ?fix"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.Replaced)
        val replaced = (result as InlineCommandEngine.CommandResult.Replaced).newText
        assertTrue(replaced.contains("I think the meeting is tomorrow"))
    }

    @Test
    fun testFormalTrigger() {
        val input = "hey can u send me that file ?formal"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.Replaced)
        val replaced = (result as InlineCommandEngine.CommandResult.Replaced).newText
        assertTrue(replaced.contains("Hello"))
        assertTrue(replaced.contains("provide") || replaced.contains("send"))
    }

    @Test
    fun testPunchyTrigger() {
        val input = "i was thinking that maybe we could launch ?punchy"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.Replaced)
        val replaced = (result as InlineCommandEngine.CommandResult.Replaced).newText
        assertTrue(replaced.contains("Let's"))
    }

    @Test
    fun testCalculatorTrigger() {
        val input = "Total: ?calc: 25 * 4 + 10"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.Replaced)
        val replaced = (result as InlineCommandEngine.CommandResult.Replaced).newText
        assertEquals("Total: 110", replaced)
    }

    @Test
    fun testUndoTrigger() {
        UndoManager.recordChange(123, "original text before edit", "new edited text")
        val input = "new edited text ?undo"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.Replaced)
        val replaced = (result as InlineCommandEngine.CommandResult.Replaced).newText
        assertEquals("original text before edit", replaced)
    }

    @Test
    fun testSnippetExpansion() {
        val snippets = mapOf("email" to "user@example.com", "shrug" to "¯\\_(ツ)_/¯")
        val input = "Contact me at ..email"
        val result = InlineCommandEngine.evaluate(input, 123, snippets)

        assertTrue(result is InlineCommandEngine.CommandResult.Replaced)
        val replaced = (result as InlineCommandEngine.CommandResult.Replaced).newText
        assertEquals("Contact me at user@example.com", replaced)
    }

    @Test
    fun testSaveSnippetTrigger() {
        val input = "Here is my info ..save:office:Building 4, Floor 2"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.SaveSnippet)
        val saveResult = result as InlineCommandEngine.CommandResult.SaveSnippet
        assertEquals("office", saveResult.key)
        assertEquals("Building 4, Floor 2", saveResult.expansion)
        assertEquals("Here is my info", saveResult.cleanedText)
    }
}
