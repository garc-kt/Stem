package com.stem.engine

import com.stem.core.models.TextPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test



class TextPayloadTest {

    @Test
    fun testPayloadMetrics() {
        val payload = TextPayload("Hello beautiful world")
        assertEquals(3, payload.wordCount)
        assertEquals(21, payload.charCount)
        assertTrue(payload.isValid)
    }

    @Test
    fun testEmptyPayloadIsInvalid() {
        val payload = TextPayload(" ")
        assertFalse(payload.isValid)
    }
}

