package com.veggiebit.sprout.features.enhancement.data.engine

/**
 * In-memory volatile history manager for single-tap and trigger undo operations.
 */
object UndoManager {

    private data class UndoSnapshot(
        val nodeHashCode: Int,
        val originalText: String,
        val replacedText: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val history = mutableListOf<UndoSnapshot>()
    private const val MAX_HISTORY = 20

    @Synchronized
    fun recordChange(nodeHashCode: Int, original: String, replaced: String) {
        if (original == replaced) return
        history.add(UndoSnapshot(nodeHashCode, original, replaced))
        if (history.size > MAX_HISTORY) {
            history.removeAt(0)
        }
    }

    @Synchronized
    fun popUndo(nodeHashCode: Int? = null): String? {
        if (history.isEmpty()) return null
        val index = if (nodeHashCode != null) {
            history.indexOfLast { it.nodeHashCode == nodeHashCode }
        } else {
            history.lastIndex
        }

        if (index >= 0) {
            val snapshot = history.removeAt(index)
            return snapshot.originalText
        }
        return null
    }

    @Synchronized
    fun canUndo(nodeHashCode: Int? = null): Boolean {
        return if (nodeHashCode != null) {
            history.any { it.nodeHashCode == nodeHashCode }
        } else {
            history.isNotEmpty()
        }
    }

    @Synchronized
    fun clear() {
        history.clear()
    }
}
