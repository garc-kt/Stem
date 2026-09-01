package com.stem.engine

import com.stem.engine.TransformCache
import com.stem.core.models.DiffToken
import com.stem.core.models.DiffType
import com.stem.core.models.TransformPreset
import com.stem.core.models.TransformResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test



class TransformCacheTest {

    @Before
    fun setUp() {
        TransformCache.clear()
    }

    @Test
    fun testCacheMissReturnsNullThenHitReturnsStoredResult() {
        val text = "hello world"
        val preset = TransformPreset.FIX
        val signature = "test-engine"

        assertNull(TransformCache.get(text, preset, signature))

        val result = TransformResult(
            originalText = text,
            transformedText = "Hello world.",
            preset = preset,
            diffTokens = listOf(DiffToken("Hello world.", DiffType.ADDED))
        )
        TransformCache.put(text, preset, signature, result)

        assertEquals(result, TransformCache.get(text, preset, signature))
    }

    @Test
    fun testDifferentEngineSignaturesAreKeptSeparate() {
        // Switching AI models/hosts must not serve a stale result cached under a different
        // engine — see TextEngineProvider.engineSignature.
        val text = "hello world"
        val preset = TransformPreset.FIX
        val resultA = TransformResult(text, "A", preset, emptyList())
        val resultB = TransformResult(text, "B", preset, emptyList())

        TransformCache.put(text, preset, "engine-a", resultA)
        TransformCache.put(text, preset, "engine-b", resultB)

        assertEquals(resultA, TransformCache.get(text, preset, "engine-a"))
        assertEquals(resultB, TransformCache.get(text, preset, "engine-b"))
    }

    @Test
    fun testDifferentPresetsAreKeptSeparate() {
        val text = "hello world"
        val signature = "engine"
        val fixResult = TransformResult(text, "Fixed", TransformPreset.FIX, emptyList())
        val punchyResult = TransformResult(text, "Punchy!", TransformPreset.PUNCHY, emptyList())

        TransformCache.put(text, TransformPreset.FIX, signature, fixResult)
        TransformCache.put(text, TransformPreset.PUNCHY, signature, punchyResult)

        assertEquals(fixResult, TransformCache.get(text, TransformPreset.FIX, signature))
        assertEquals(punchyResult, TransformCache.get(text, TransformPreset.PUNCHY, signature))
    }

    @Test
    fun testClearRemovesAllEntries() {
        val text = "hello world"
        val preset = TransformPreset.FIX
        TransformCache.put(text, preset, "engine", TransformResult(text, "Hi", preset, emptyList()))

        TransformCache.clear()

        assertNull(TransformCache.get(text, preset, "engine"))
    }
}

