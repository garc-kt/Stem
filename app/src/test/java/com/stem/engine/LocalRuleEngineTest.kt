package com.stem.engine

import com.stem.engine.LocalRuleEngine
import com.stem.core.models.LanguagePreference
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test



class LocalRuleEngineTest {

    @Test
    fun testFixAndPolishCorrectsCommonTypoAndPunctuation() = runBlocking {
        val payload = TextPayload("teh meeting is tommorow ,dont forget")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX, LanguagePreference.ENGLISH)

        assertTrue(result.transformedText.contains("The"))
        assertTrue(result.transformedText.contains("tomorrow,"))
        assertTrue(result.transformedText.contains("don't"))
        assertTrue(result.hasChanges)
    }

    @Test
    fun testConciseRemovesWordyPhrasesAndFillers() = runBlocking {
        val payload = TextPayload("in order to succeed we really basically must work hard")
        val result = LocalRuleEngine.transform(payload, TransformPreset.CONCISE, LanguagePreference.ENGLISH)

        assertTrue(result.transformedText.startsWith("To"))
        assertTrue(!result.transformedText.contains("in order to", ignoreCase = true))
        assertTrue(!result.transformedText.contains("basically", ignoreCase = true))
        assertTrue(result.wordsSaved > 0)
    }

    @Test
    fun testProfessionalElevatesTone() = runBlocking {
        val payload = TextPayload("hey can you give me that file asap")
        val result = LocalRuleEngine.transform(payload, TransformPreset.PROFESSIONAL, LanguagePreference.ENGLISH)

        assertTrue(result.transformedText.contains("Hello"))
        assertTrue(result.transformedText.contains("provide") || result.transformedText.contains("give"))
        assertTrue(result.transformedText.contains("as soon as possible"))
    }

    @Test
    fun testPunchyEmphasizesActiveVoice() = runBlocking {
        val payload = TextPayload("i was thinking that maybe we could make better results")
        val result = LocalRuleEngine.transform(payload, TransformPreset.PUNCHY, LanguagePreference.ENGLISH)

        assertTrue(result.transformedText.contains("Let's"))
        assertTrue(result.transformedText.contains("supercharge") || result.transformedText.contains("results"))
    }

    @Test
    fun testFixAndPolishPreservesEmailAddresses() = runBlocking {
        val payload = TextPayload("contact me at user@example.com for details")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX, LanguagePreference.ENGLISH)

        assertTrue(result.transformedText.contains("user@example.com"))
    }

    @Test
    fun testFixAndPolishPreservesUrls() = runBlocking {
        val payload = TextPayload("see https://example.com/path for more info")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX, LanguagePreference.ENGLISH)

        assertTrue(result.transformedText.contains("https://example.com/path"))
    }

    @Test
    fun testFixAndPolishPreservesAbbreviations() = runBlocking {
        val payload = TextPayload("bring snacks, e.g. chips and soda")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX, LanguagePreference.ENGLISH)

        assertTrue(result.transformedText.contains("e.g."))
    }

    @Test
    fun testFixAndPolishPreservesDecimals() = runBlocking {
        val payload = TextPayload("the total came to 3.14 dollars")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX, LanguagePreference.ENGLISH)

        assertTrue(result.transformedText.contains("3.14"))
    }

    @Test
    fun testProfessionalDoesNotBreakPhrasalVerbGetUp() = runBlocking {
        // formalReplacements maps "get" -> "obtain", but "get up"/"get together" are phrasal
        // verbs where that substitution used to produce nonsense ("obtain up").
        val payload = TextPayload("I need to get up early tomorrow")
        val result = LocalRuleEngine.transform(payload, TransformPreset.PROFESSIONAL, LanguagePreference.ENGLISH)

        assertFalse(result.transformedText.contains("obtain up", ignoreCase = true))
        assertTrue(result.transformedText.contains("get up", ignoreCase = true))
    }

    @Test
    fun testToneTransformsDoNotCorruptMidSentenceProtectedSpans() = runBlocking {
        // CONCISE/PROFESSIONAL/PUNCHY/FRIENDLY each re-capitalize sentences *after*
        // applyFixAndPolish has already restored protected spans (email/URL/etc.) to plain
        // text. A naive capitalizeSentences() call at that point treats the dot in
        // "test@example.com" as a sentence end and produces "test@example.Com" — this pins
        // that it stays intact.
        val presets = listOf(
            TransformPreset.CONCISE,
            TransformPreset.PROFESSIONAL,
            TransformPreset.PUNCHY,
            TransformPreset.FRIENDLY
        )
        for (preset in presets) {
            val payload = TextPayload("please email test@example.com today for the full details")
            val result = LocalRuleEngine.transform(payload, preset, LanguagePreference.ENGLISH)

            assertTrue(
                "preset=$preset produced: ${result.transformedText}",
                result.transformedText.contains("test@example.com")
            )
            assertFalse(
                "preset=$preset produced: ${result.transformedText}",
                result.transformedText.contains("test@example.Com")
            )
        }
    }

    // Golden-output pins for SUMMARIZE/BULLETIZE/EXPAND, captured from the pre-refactor
    // implementation. Guards the upcoming regex-hoisting change (compiling shared patterns once
    // instead of per-call) against any accidental behavior drift.

    @Test
    fun testSummarizeGoldenOutput() = runBlocking {
        val payload = TextPayload("We had a great meeting today. The deadline is March 5. It was nice weather outside. Please submit the report by then.")
        val result = LocalRuleEngine.transform(payload, TransformPreset.SUMMARIZE, LanguagePreference.ENGLISH)

        assertEquals("We had a great meeting today. The deadline is March 5.", result.transformedText)
    }

    @Test
    fun testBulletizeGoldenOutput() = runBlocking {
        val payload = TextPayload("First we need to review the draft and then send it to legal. Second, schedule a follow-up.")
        val result = LocalRuleEngine.transform(payload, TransformPreset.BULLETIZE, LanguagePreference.ENGLISH)

        assertEquals("• First we need to review the draft\n• send it to legal\n• Second, schedule a follow-up", result.transformedText)
    }

    @Test
    fun testExpandGoldenOutput() = runBlocking {
        val payload = TextPayload("btw the report is done, thx")
        val result = LocalRuleEngine.transform(payload, TransformPreset.EXPAND, LanguagePreference.ENGLISH)

        assertEquals("To elaborate: by the way the report is done, thank you.", result.transformedText)
    }
}

