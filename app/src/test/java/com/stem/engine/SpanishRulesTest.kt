package com.stem.engine

import com.stem.engine.LocalRuleEngine
import com.stem.engine.DetectedLanguage
import com.stem.engine.LanguageDetector
import com.stem.core.models.LanguagePreference
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test



/**
 * ARCHITECTURE_AND_PROCESS.md §2 Phase 2 documented Spanish spelling/tone support that never
 * actually existed in code — these cover the [LanguageDetector] heuristic and the new
 * [com.stem.engine.SpanishRules] dictionaries.
 */
class SpanishRulesTest {

    @Test
    fun testDetectorRecognizesSpanishByAccentedCharacters() {
        assertEquals(DetectedLanguage.SPANISH, LanguageDetector.detect("¿Cómo estás? Muy bien, gracias."))
    }

    @Test
    fun testDetectorRecognizesSpanishByStopwordFrequency() {
        assertEquals(DetectedLanguage.SPANISH, LanguageDetector.detect("el perro esta en la casa y la gente lo ve"))
    }

    @Test
    fun testDetectorRecognizesEnglishByDefault() {
        assertEquals(DetectedLanguage.ENGLISH, LanguageDetector.detect("the quick brown fox jumps over the lazy dog"))
    }

    @Test
    fun testFixAndPolishRestoresAccentsOnCommonWords() = runBlocking {
        val payload = TextPayload("aqui tambien esta el codigo")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX, LanguagePreference.SPANISH)

        assertTrue(result.transformedText.contains("aquí", ignoreCase = true))
        assertTrue(result.transformedText.contains("también", ignoreCase = true))
        assertTrue(result.transformedText.contains("código", ignoreCase = true))
    }

    @Test
    fun testFixAndPolishAddsMissingInvertedQuestionMark() = runBlocking {
        val payload = TextPayload("Como estas hoy?")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX, LanguagePreference.SPANISH)

        assertTrue(result.transformedText.contains("¿"))
    }

    @Test
    fun testProfessionalAppliesFormalRegister() = runBlocking {
        val payload = TextPayload("q tal, porfa avisame")
        val result = LocalRuleEngine.transform(payload, TransformPreset.PROFESSIONAL, LanguagePreference.SPANISH)

        assertTrue(result.transformedText.contains("por favor", ignoreCase = true))
    }
}

