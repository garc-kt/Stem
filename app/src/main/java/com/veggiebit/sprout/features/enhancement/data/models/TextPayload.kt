package com.veggiebit.sprout.features.enhancement.data.models

import android.graphics.Rect

/**
 * Encapsulates the text buffer and context captured from an active AccessibilityNode.
 */
data class TextPayload(
    val text: String,
    val selectionStart: Int = -1,
    val selectionEnd: Int = -1,
    val boundsInScreen: Rect? = null,
    val packageName: String? = null,
    val className: String? = null,
    val nodeHashCode: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isValid: Boolean get() = text.isNotBlank() && text.length >= 2
    val wordCount: Int get() = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    val charCount: Int get() = text.length
}
