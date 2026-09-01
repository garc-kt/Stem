package com.stem.engine

import com.stem.engine.LocalRuleEngine
import com.stem.core.models.LanguagePreference
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test



class LocalRuleEngineTest {

    @Before
    fun setUp() {
        // LocalRuleEngine.languagePreference is a shared singleton var (see its own doc
        // comment for why) — pin it explicitly so another test class's language setting can't
        // leak in via JVM-wide test execution order.
        LocalRuleEngine.languagePreference = LanguagePreference.ENGLISH
    }

    @After
    fun tearDown() {
        LocalRuleEngine.languagePreference = LanguagePreference.AUTO
    }

    @Test
    fun testFixAndPolishCorrectsCommonTypoAndPunctuation() = runBlocking {
        val payload = TextPayload("teh meeting is tommorow ,dont forget")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX)

        assertTrue(result.transformedText.contains("The"))
        assertTrue(result.transformedText.contains("tomorrow,"))
        assertTrue(result.transformedText.contains("don't"))
        assertTrue(result.hasChanges)
    }

    @Test
    fun testConciseRemovesWordyPhrasesAndFillers() = runBlocking {
        val payload = TextPayload("in order to succeed we really basically must work hard")
        val result = LocalRuleEngine.transform(payload, TransformPreset.CONCISE)

        assertTrue(result.transformedText.startsWith("To"))
        assertTrue(!result.transformedText.contains("in order to", ignoreCase = true))
        assertTrue(!result.transformedText.contains("basically", ignoreCase = true))
        assertTrue(result.wordsSaved > 0)
    }

    @Test
    fun testProfessionalElevatesTone() = runBlocking {
        val payload = TextPayload("hey can you give me that file asap")
        val result = LocalRuleEngine.transform(payload, TransformPreset.PROFESSIONAL)

        assertTrue(result.transformedText.contains("Hello"))
        assertTrue(result.transformedText.contains("provide") || result.transformedText.contains("give"))
        assertTrue(result.transformedText.contains("as soon as possible"))
    }

    @Test
    fun testPunchyEmphasizesActiveVoice() = runBlocking {
        val payload = TextPayload("i was thinking that maybe we could make better results")
        val result = LocalRuleEngine.transform(payload, TransformPreset.PUNCHY)

        assertTrue(result.transformedText.contains("Let's"))
        assertTrue(result.transformedText.contains("supercharge") || result.transformedText.contains("results"))
    }

    @Test
    fun testFixAndPolishPreservesEmailAddresses() = runBlocking {
        val payload = TextPayload("contact me at user@example.com for details")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX)

        assertTrue(result.transformedText.contains("user@example.com"))
    }

    @Test
    fun testFixAndPolishPreservesUrls() = runBlocking {
        val payload = TextPayload("see https://example.com/path for more info")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX)

        assertTrue(result.transformedText.contains("https://example.com/path"))
    }

    @Test
    fun testFixAndPolishPreservesAbbreviations() = runBlocking {
        val payload = TextPayload("bring snacks, e.g. chips and soda")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX)

        assertTrue(result.transformedText.contains("e.g."))
    }

    @Test
    fun testFixAndPolishPreservesDecimals() = runBlocking {
        val payload = TextPayload("the total came to 3.14 dollars")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX)

        assertTrue(result.transformedText.contains("3.14"))
    }

    @Test
    fun testProfessionalDoesNotBreakPhrasalVerbGetUp() = runBlocking {
        // formalReplacements maps "get" -> "obtain", but "get up"/"get together" are phrasal
        // verbs where that substitution used to produce nonsense ("obtain up").
        val payload = TextPayload("I need to get up early tomorrow")
        val result = LocalRuleEngine.transform(payload, TransformPreset.PROFESSIONAL)

        assertFalse(result.transformedText.contains("obtain up", ignoreCase = true))
        assertTrue(result.transformedText.contains("get up", ignoreCase = true))
    }
}

