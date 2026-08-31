package com.veggiebit.sprout.core.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload

object AccessibilityUtils {

    /**
     * Checks if a given node is an editable text field.
     */
    fun isEditableNode(node: AccessibilityNodeInfoCompat?): Boolean {
        if (node == null) return false
        if (node.isEditable) return true
        val className = node.className?.toString() ?: ""
        return className.contains("EditText", ignoreCase = true) ||
               className.contains("Input", ignoreCase = true) ||
               className.contains("TextField", ignoreCase = true)
    }

    /**
     * Traverses the accessibility node tree to find the currently focused editable node.
     */
    fun findFocusedEditableNode(root: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? {
        if (root == null) return null
        if (root.isFocused && isEditableNode(root)) {
            return root
        }

        val directFocus = root.findFocus(AccessibilityNodeInfoCompat.FOCUS_INPUT)
        if (directFocus != null && isEditableNode(directFocus)) {
            return directFocus
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findFocusedEditableNode(child)
            if (found != null) {
                return found
            }
        }
        return null
    }

    /**
     * Extracts screen bounds for positioning floating overlays.
     */
    fun getNodeBounds(node: AccessibilityNodeInfoCompat?): Rect {
        val rect = Rect()
        if (node != null) {
            node.getBoundsInScreen(rect)
        }
        return rect
    }

    /**
     * Extracts text content and metadata into a [TextPayload].
     */
    fun extractTextPayload(node: AccessibilityNodeInfoCompat?): TextPayload? {
        if (node == null) return null
        val text = node.text?.toString() ?: ""
        val bounds = getNodeBounds(node)
        val selectionStart = node.textSelectionStart
        val selectionEnd = node.textSelectionEnd
        val pkg = node.packageName?.toString()
        val cls = node.className?.toString()

        return TextPayload(
            text = text,
            selectionStart = selectionStart,
            selectionEnd = selectionEnd,
            boundsInScreen = bounds,
            packageName = pkg,
            className = cls,
            nodeHashCode = node.hashCode()
        )
    }

    /**
     * Injects replacement text into the active node via ACTION_SET_TEXT or clipboard fallback.
     */
    fun injectText(node: AccessibilityNodeInfoCompat?, newText: String, context: Context): Boolean {
        if (node == null) return false

        // 1. Direct ACTION_SET_TEXT
        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
        }
        val directSuccess = node.performAction(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, arguments)
        if (directSuccess) {
            return true
        }

        // 2. Clipboard fallback
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("Sprout Replacement", newText)
                clipboard.setPrimaryClip(clip)
                node.performAction(AccessibilityNodeInfoCompat.ACTION_SELECT)
                node.performAction(AccessibilityNodeInfoCompat.ACTION_PASTE)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
