package com.stem.engine

import com.stem.engine.InlineCommandEngine
import com.stem.engine.TransformHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        assertTrue(result is InlineCommandEngine.CommandResult.Undo)
        assertEquals(123, (result as InlineCommandEngine.CommandResult.Undo).nodeHashCode)
        // evaluate() is pure: it must not have popped the history entry itself.
        assertTrue(TransformHistory.canUndo(123))
    }

    @Test
    fun testUndoTriggerDoesNotDoublePopOnRepeatedEvaluation() {
        TransformHistory.recordChange(456, "before", "after")

        InlineCommandEngine.evaluate("after ?undo", 456)
        InlineCommandEngine.evaluate("after ?undo", 456)

        // Calling evaluate() twice must not consume the history entry twice — it never pops.
        assertTrue(TransformHistory.canUndo(456))
        assertEquals("before", TransformHistory.popUndo(456))
        assertFalse(TransformHistory.canUndo(456))
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

    @Test
    fun testPlainProseWithoutDotOrQuestionMarkBailsToNone() {
        // Every recognized trigger contains a literal '.' or '?' — evaluate() short-circuits
        // before the regex/suffix cascade when neither is present in the trimmed text.
        val result = InlineCommandEngine.evaluate("just an ordinary sentence with no triggers yet", 123)
        assertTrue(result is InlineCommandEngine.CommandResult.None)
    }

    @Test
    fun testPlainProseContainingPeriodWithoutMatchingTriggerStillBailsToNone() {
        val result = InlineCommandEngine.evaluate("This is a normal sentence with a period.", 123)
        assertTrue(result is InlineCommandEngine.CommandResult.None)
    }

    @Test
    fun testSemicolonPrefixPresets() {
        val tests = listOf(
            "Draft ;;fix" to com.stem.core.models.TransformPreset.FIX,
            "Draft ;;concise" to com.stem.core.models.TransformPreset.CONCISE,
            "Draft ;;formal" to com.stem.core.models.TransformPreset.PROFESSIONAL,
            "Draft ;;punchy" to com.stem.core.models.TransformPreset.PUNCHY,
            "Draft ;;friendly" to com.stem.core.models.TransformPreset.FRIENDLY,
            "Draft ;;bullets" to com.stem.core.models.TransformPreset.BULLETIZE,
            "Draft ;;summarize" to com.stem.core.models.TransformPreset.SUMMARIZE,
            "Draft ;;expand" to com.stem.core.models.TransformPreset.EXPAND
        )

        for ((input, expectedPreset) in tests) {
            val result = InlineCommandEngine.evaluate(input, 123)
            assertTrue("Expected $expectedPreset for '$input'", result is InlineCommandEngine.CommandResult.RunAIPreset)
            assertEquals(expectedPreset, (result as InlineCommandEngine.CommandResult.RunAIPreset).preset)
            assertEquals("Draft", result.body)
        }
    }

    @Test
    fun testSemicolonPrefixCalcAndSnippetsAndCustomCommands() {
        // Calc with ;;calc:
        val calcResult = InlineCommandEngine.evaluate("Total: ;;calc: 50 * 2 + 5", 123)
        assertTrue(calcResult is InlineCommandEngine.CommandResult.Replaced)
        assertEquals("Total: 105", (calcResult as InlineCommandEngine.CommandResult.Replaced).newText)

        // Snippet with ;;email
        val snippets = mapOf("email" to "contact@stem.ai")
        val snipResult = InlineCommandEngine.evaluate("Reach me at ;;email", 123, snippets = snippets)
        assertTrue(snipResult is InlineCommandEngine.CommandResult.Replaced)
        assertEquals("Reach me at contact@stem.ai", (snipResult as InlineCommandEngine.CommandResult.Replaced).newText)

        // Custom command with ;;roast
        val custom = mapOf("roast" to "Roast this aggressively")
        val customResult = InlineCommandEngine.evaluate("Check my outfit ;;roast", 123, customCommands = custom)
        assertTrue(customResult is InlineCommandEngine.CommandResult.RunAIPrompt)
        assertEquals("Check my outfit", (customResult as InlineCommandEngine.CommandResult.RunAIPrompt).body)
        assertEquals("Roast this aggressively", customResult.customPrompt)

        // Undo with ;;undo
        TransformHistory.recordChange(999, "old text", "new text")
        val undoResult = InlineCommandEngine.evaluate("new text ;;undo", 999)
        assertTrue(undoResult is InlineCommandEngine.CommandResult.Undo)

        // Now & date with ;;now and ;;date
        val nowRes = InlineCommandEngine.evaluate("Done at ;;now", 123)
        assertTrue(nowRes is InlineCommandEngine.CommandResult.Replaced)
        assertTrue((nowRes as InlineCommandEngine.CommandResult.Replaced).newText.startsWith("Done at "))

        val dateRes = InlineCommandEngine.evaluate("Deadline: ;;date", 123)
        assertTrue(dateRes is InlineCommandEngine.CommandResult.Replaced)
        assertTrue((dateRes as InlineCommandEngine.CommandResult.Replaced).newText.startsWith("Deadline: "))
    }

    @Test
    fun testSingleDotDoesNotFalseTriggerOnDomainsOrFilenamesOrWords() {
        val snippets = mapOf("com" to "commercial", "sh" to "shell script", "py" to "python")
        val customCommands = mapOf("roast" to "Roast prompt")

        // URL / domain
        val urlResult = InlineCommandEngine.evaluate("Visit https://google.com", 123, snippets = snippets)
        assertTrue("Expected None for google.com, got $urlResult", urlResult is InlineCommandEngine.CommandResult.None)

        // Filename
        val fileResult = InlineCommandEngine.evaluate("Execute script.sh", 123, snippets = snippets)
        assertTrue("Expected None for script.sh, got $fileResult", fileResult is InlineCommandEngine.CommandResult.None)

        // Preset command attached to word
        val fixResult = InlineCommandEngine.evaluate("We deployed a hot.fix", 123)
        assertTrue("Expected None for hot.fix, got $fixResult", fixResult is InlineCommandEngine.CommandResult.None)

        // Built-in .now attached to word
        val nowResult = InlineCommandEngine.evaluate("Need it right.now", 123)
        assertTrue("Expected None for right.now, got $nowResult", nowResult is InlineCommandEngine.CommandResult.None)

        // Built-in .date attached to word
        val dateResult = InlineCommandEngine.evaluate("Check the due.date", 123)
        assertTrue("Expected None for due.date, got $dateResult", dateResult is InlineCommandEngine.CommandResult.None)

        // Custom command attached to word
        val customResult = InlineCommandEngine.evaluate("Review app.roast", 123, customCommands = customCommands)
        assertTrue("Expected None for app.roast, got $customResult", customResult is InlineCommandEngine.CommandResult.None)
    }

    @Test
    fun testSingleDotTriggersCorrectlyWhenPrecededByWhitespaceOrStart() {
        val snippets = mapOf("email" to "hello@stem.ai")
        val customCommands = mapOf("roast" to "Roast prompt")

        // Preceded by space: Snippet
        val snipRes = InlineCommandEngine.evaluate("Reach out at .email", 123, snippets = snippets)
        assertTrue(snipRes is InlineCommandEngine.CommandResult.Replaced)
        assertEquals("Reach out at hello@stem.ai", (snipRes as InlineCommandEngine.CommandResult.Replaced).newText)

        // At start of string: Snippet
        val startSnip = InlineCommandEngine.evaluate(".email", 123, snippets = snippets)
        assertTrue(startSnip is InlineCommandEngine.CommandResult.Replaced)
        assertEquals("hello@stem.ai", (startSnip as InlineCommandEngine.CommandResult.Replaced).newText)

        // Preceded by space: Preset
        val presetRes = InlineCommandEngine.evaluate("Please polish this sentence .fix", 123)
        assertTrue(presetRes is InlineCommandEngine.CommandResult.RunAIPreset)
        assertEquals("Please polish this sentence", (presetRes as InlineCommandEngine.CommandResult.RunAIPreset).body)

        // Preceded by space: Custom Command
        val customRes = InlineCommandEngine.evaluate("Review my resume .roast", 123, customCommands = customCommands)
        assertTrue(customRes is InlineCommandEngine.CommandResult.RunAIPrompt)
        assertEquals("Review my resume", (customRes as InlineCommandEngine.CommandResult.RunAIPrompt).body)
    }

    @Test
    fun testFormatHistoryCommand() {
        assertEquals(";;fix", com.stem.ui.screens.formatHistoryCommand(";;fix"))
        assertEquals("..expand", com.stem.ui.screens.formatHistoryCommand("..expand"))
        assertEquals(".fix", com.stem.ui.screens.formatHistoryCommand(".fix"))
        assertEquals("?formal", com.stem.ui.screens.formatHistoryCommand("?formal"))
        assertEquals("?custom", com.stem.ui.screens.formatHistoryCommand("custom"))
        assertEquals("?enhance", com.stem.ui.screens.formatHistoryCommand(""))
    }
}

