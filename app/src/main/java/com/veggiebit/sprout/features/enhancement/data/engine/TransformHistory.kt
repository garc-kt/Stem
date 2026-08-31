package com.veggiebit.sprout.features.enhancement.data.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory volatile history of transactional replacements — renamed from UndoManager, whose
 * 20-entry history was recorded but only ever reachable one step at a time. Now exposed as a
 * [StateFlow] so a session history sheet can list/re-apply any past entry, not just the last
 * one. `recordChange`/`popUndo`/`canUndo`/`clear` keep their original signatures so every
 * existing caller (the accessibility service, the overlay, inline `?undo`) needed only the
 * rename, not a behavior change.
 *
 * Privacy note: this is process memory only, never persisted, and is cleared in
 * [SproutAccessibilityService.onDestroy] — consistent with Sprout's zero-storage policy for
 * user-typed content (see plan.md §4.3).
 */
object TransformHistory {

    data class Snapshot(
        val nodeHashCode: Int,
        val originalText: String,
        val replacedText: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private const val MAX_HISTORY = 20

    private val _history = MutableStateFlow<List<Snapshot>>(emptyList())
    val history: StateFlow<List<Snapshot>> = _history.asStateFlow()

    @Synchronized
    fun recordChange(nodeHashCode: Int, original: String, replaced: String) {
        if (original == replaced) return
        _history.value = (_history.value + Snapshot(nodeHashCode, original, replaced)).takeLast(MAX_HISTORY)
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

    /** Re-applies a specific historical entry (from the history sheet) without removing it
     * from history — unlike [popUndo], which consumes the entry it returns. */
    fun reapply(snapshot: Snapshot): String = snapshot.replacedText

    @Synchronized
    fun clear() {
        _history.value = emptyList()
    }
}
