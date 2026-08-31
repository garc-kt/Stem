package com.veggiebit.sprout.features.enhancement

import com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRuleEngineTest {

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
}
