package com.stem.engine

import com.stem.engine.InlineCommandEngine
import com.stem.engine.TransformHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test



class InlineCommandEngineTest {

    @Before
    fun setUp() {
        TransformHistory.clear()
    }

    @Test
    fun testFixTrigger() {
        val input = "i think teh meeting is tommorow ?fix"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.RunAIPreset)
        val aiResult = result as InlineCommandEngine.CommandResult.RunAIPreset
        assertEquals("i think teh meeting is tommorow", aiResult.body)
        assertEquals(com.stem.core.models.TransformPreset.FIX, aiResult.preset)
    }

    @Test
    fun testFormalTrigger() {
        val input = "hey can u send me that file ?formal"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.RunAIPreset)
        val aiResult = result as InlineCommandEngine.CommandResult.RunAIPreset
        assertEquals("hey can u send me that file", aiResult.body)
        assertEquals(com.stem.core.models.TransformPreset.PROFESSIONAL, aiResult.preset)
    }

    @Test
    fun testCustomCommandTrigger() {
        val customCommands = mapOf("roast" to "Rewrite this in a witty roast tone")
        val input = "That meeting was boring ?roast"
        val result = InlineCommandEngine.evaluate(
            text = input,
            nodeHashCode = 123,
            customCommands = customCommands
        )

        assertTrue(result is InlineCommandEngine.CommandResult.RunAIPrompt)
        val promptResult = result as InlineCommandEngine.CommandResult.RunAIPrompt
        assertEquals("That meeting was boring", promptResult.body)
        assertEquals("Rewrite this in a witty roast tone", promptResult.customPrompt)
    }

    @Test
    fun testDynamicAIPromptTrigger() {
        val input = "We launch next Tuesday ?ai: make this sound like an exciting movie trailer"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.RunAIPrompt)
        val promptResult = result as InlineCommandEngine.CommandResult.RunAIPrompt
        assertEquals("We launch next Tuesday", promptResult.body)
        assertEquals("make this sound like an exciting movie trailer", promptResult.customPrompt)
    }

    @Test
    fun testSaveCustomCommandTrigger() {
        val input = "Notes ..cmd:pirate:Rewrite this in full pirate talk with ahoy"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.SaveCustomCommand)
        val saveResult = result as InlineCommandEngine.CommandResult.SaveCustomCommand
        assertEquals("pirate", saveResult.trigger)
        assertEquals("Rewrite this in full pirate talk with ahoy", saveResult.prompt)
        assertEquals("Notes", saveResult.cleanedText)
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
    fun testCalculatorTriggerRespectsParentheses() {
        // Previously the two-pass scanner silently dropped parentheses, evaluating this as
        // 2+3*4=14 instead of the correct (2+3)*4=20.
        val input = "Total: ?calc: (2+3)*4"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.Replaced)
        assertEquals("Total: 20", (result as InlineCommandEngine.CommandResult.Replaced).newText)
    }

    @Test
    fun testCalculatorTriggerSupportsExponent() {
        // Previously '^' was accepted into the expression charset but silently dropped by the
        // evaluator, evaluating "2^8" as 2+8=10... (actually as digits concatenated) rather
        // than the correct 2^8=256.
        val input = "Value: ?calc: 2^8"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.Replaced)
        assertEquals("Value: 256", (result as InlineCommandEngine.CommandResult.Replaced).newText)
    }

    @Test
    fun testCalculatorTriggerSupportsUnaryMinus() {
        val input = "Value: ?calc: -5+10"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.Replaced)
        assertEquals("Value: 5", (result as InlineCommandEngine.CommandResult.Replaced).newText)
    }

    @Test
    fun testCalculatorTriggerWithMalformedExpressionDoesNothing() {
        // Malformed input (a stray operator) should no-op rather than inject a wrong result.
        val input = "Value: ?calc: 5*/2"
        val result = InlineCommandEngine.evaluate(input, 123)

        assertTrue(result is InlineCommandEngine.CommandResult.None)
    }

    @Test
    fun testUndoTrigger() {
        TransformHistory.recordChange(123, "original text before edit", "new edited text")
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

        // Also test single-dot prefix .email
        val singleDotResult = InlineCommandEngine.evaluate("Contact me at .email", 123, snippets)
        assertTrue(singleDotResult is InlineCommandEngine.CommandResult.Replaced)
        assertEquals("Contact me at user@example.com", (singleDotResult as InlineCommandEngine.CommandResult.Replaced).newText)
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

    @Test
    fun testAllPresetTriggers() {
        val tests = listOf(
            "Hello ?concise" to com.stem.core.models.TransformPreset.CONCISE,
            "Hello ?punchy" to com.stem.core.models.TransformPreset.PUNCHY,
            "Hello ?friendly" to com.stem.core.models.TransformPreset.FRIENDLY,
            "Hello ?bullets" to com.stem.core.models.TransformPreset.BULLETIZE,
            "Hello ?summarize" to com.stem.core.models.TransformPreset.SUMMARIZE,
            "Hello ?expand" to com.stem.core.models.TransformPreset.EXPAND
        )

        for ((input, expectedPreset) in tests) {
            val result = InlineCommandEngine.evaluate(input, 123)
            assertTrue("Expected $expectedPreset for '$input'", result is InlineCommandEngine.CommandResult.RunAIPreset)
            assertEquals(expectedPreset, (result as InlineCommandEngine.CommandResult.RunAIPreset).preset)
            assertEquals("Hello", result.body)
        }
    }

    @Test
    fun testDateAndTimestampTriggers() {
        val dateResult = InlineCommandEngine.evaluate("Meeting on ?date", 123)
        assertTrue(dateResult is InlineCommandEngine.CommandResult.Replaced)
        assertTrue((dateResult as InlineCommandEngine.CommandResult.Replaced).newText.startsWith("Meeting on "))

        val nowResult = InlineCommandEngine.evaluate("Signed at ?now", 123)
        assertTrue(nowResult is InlineCommandEngine.CommandResult.Replaced)
        assertTrue((nowResult as InlineCommandEngine.CommandResult.Replaced).newText.startsWith("Signed at "))
    }
}

