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

class PortugueseRulesTest {

    @Test
    fun testDetectorRecognizesPortugueseByDistinctAccents() {
        assertEquals(DetectedLanguage.PORTUGUESE, LanguageDetector.detect("Esta é uma ação de comunicação em São Paulo."))
    }

    @Test
    fun testDetectorRecognizesPortugueseByStopwords() {
        assertEquals(DetectedLanguage.PORTUGUESE, LanguageDetector.detect("o cachorro esta na casa e a gente vai la"))
    }

    @Test
    fun testFixAndPolishRestoresAccentsAndExpandsChatShorthands() = runBlocking {
        val payload = TextPayload("vc tbm nao foi na reuniao hoje")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FIX, LanguagePreference.PORTUGUESE)

        assertTrue(result.transformedText.contains("você", ignoreCase = true))
        assertTrue(result.transformedText.contains("também", ignoreCase = true))
        assertTrue(result.transformedText.contains("não", ignoreCase = true))
        assertTrue(result.transformedText.contains("reunião", ignoreCase = true))
    }

    @Test
    fun testConciseRemovesWordyPhrases() = runBlocking {
        val payload = TextPayload("com o objetivo de melhorar o projeto no momento presente vamos agir")
        val result = LocalRuleEngine.transform(payload, TransformPreset.CONCISE, LanguagePreference.PORTUGUESE)

        assertTrue(result.transformedText.contains("para", ignoreCase = true))
        assertTrue(result.transformedText.contains("agora", ignoreCase = true))
        assertTrue(!result.transformedText.contains("com o objetivo de", ignoreCase = true))
    }

    @Test
    fun testProfessionalAppliesFormalRegister() = runBlocking {
        val payload = TextPayload("valeu cara, isso tá massa e depois me avisa")
        val result = LocalRuleEngine.transform(payload, TransformPreset.PROFESSIONAL, LanguagePreference.PORTUGUESE)

        assertTrue(result.transformedText.contains("obrigado", ignoreCase = true))
        assertTrue(result.transformedText.contains("está", ignoreCase = true))
        assertTrue(result.transformedText.contains("excelente", ignoreCase = true))
    }

    @Test
    fun testFriendlyAppliesApproachableRegister() = runBlocking {
        val payload = TextPayload("Prezado senhor, solicito o envio do arquivo. Atenciosamente")
        val result = LocalRuleEngine.transform(payload, TransformPreset.FRIENDLY, LanguagePreference.PORTUGUESE)

        assertTrue(result.transformedText.contains("olá", ignoreCase = true))
        assertTrue(result.transformedText.contains("peço", ignoreCase = true))
        assertTrue(result.transformedText.contains("abraço", ignoreCase = true))
    }
}
