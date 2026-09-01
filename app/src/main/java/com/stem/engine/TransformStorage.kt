package com.stem.engine

import com.stem.core.models.TransformPreset
import com.stem.core.models.TransformResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow



/**
 * In-memory volatile history of transactional replacements.
 */
object TransformHistory {

    data class Snapshot(
        val id: String = java.util.UUID.randomUUID().toString(),
        val nodeHashCode: Int,
        val originalText: String,
        val replacedText: String,
        val presetName: String = "Enhance",
        val timestamp: Long = System.currentTimeMillis()
    )

    private const val MAX_HISTORY = 20

    private val _history = MutableStateFlow<List<Snapshot>>(emptyList())
    val history: StateFlow<List<Snapshot>> = _history.asStateFlow()

    @Synchronized
    fun recordChange(nodeHashCode: Int, original: String, replaced: String, presetName: String = "Enhance") {
        if (original == replaced || original.isBlank() || replaced.isBlank()) return
        val current = _history.value
        val last = current.lastOrNull()
        if (last != null && last.originalText == original && last.replacedText == replaced && (System.currentTimeMillis() - last.timestamp < 2000)) {
            return
        }
        _history.value = (current + Snapshot(nodeHashCode = nodeHashCode, originalText = original, replacedText = replaced, presetName = presetName)).takeLast(MAX_HISTORY)
    }

    @Synchronized
    fun popUndo(nodeHashCode: Int? = null): String? {
        val current = _history.value
        if (current.isEmpty()) return null
        val index = if (nodeHashCode != null) {
            current.indexOfLast { it.nodeHashCode == nodeHashCode }
        } else {
            current.lastIndex
        }

        if (index >= 0) {
            val snapshot = current[index]
            _history.value = current.toMutableList().apply { removeAt(index) }
            return snapshot.originalText
        }
        return null
    }

    @Synchronized
    fun canUndo(nodeHashCode: Int? = null): Boolean {
        return if (nodeHashCode != null) {
            _history.value.any { it.nodeHashCode == nodeHashCode }
        } else {
            _history.value.isNotEmpty()
        }
    }

    @Synchronized
    fun clear() {
        _history.value = emptyList()
    }
}

/**
 * Small in-memory cache preventing redundant engine calls.
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
        if (result.transformedText.isNotBlank() && result.transformedText != result.originalText) {
            cache[CacheKey(text, preset.id, engineSignature)] = result
        }
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}
