package com.veggiebit.sprout.features.enhancement.data.engine

import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult

/**
 * Small in-memory (never persisted) cache preventing redundant engine calls for text/preset
 * combinations already computed in this process lifetime. Bounded LRU — zero disk footprint,
 * consistent with Sprout's zero-telemetry policy.
 */
object TransformCache {

    private const val MAX_ENTRIES = 32

    private data class CacheKey(val text: String, val presetId: String, val engineSignature: String)

    private val cache = object : LinkedHashMap<CacheKey, TransformResult>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, TransformResult>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    @Synchronized
    fun get(text: String, preset: TransformPreset, engineSignature: String): TransformResult? {
        return cache[CacheKey(text, preset.id, engineSignature)]
    }

    @Synchronized
    fun put(text: String, preset: TransformPreset, engineSignature: String, result: TransformResult) {
        cache[CacheKey(text, preset.id, engineSignature)] = result
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}
