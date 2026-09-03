package com.stem.core.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.stem.core.models.TextPayload
import java.util.ArrayDeque



object AccessibilityUtils {

    private const val MAX_SEARCH_DEPTH = 12
    private const val MAX_VISITED_NODES = 400

    fun isEditableNode(node: AccessibilityNodeInfoCompat?): Boolean {
        if (node == null) return false
        if (node.isEditable) return true
        val className = node.className?.toString() ?: ""
        return className.contains("EditText", ignoreCase = true) ||
               className.contains("Input", ignoreCase = true) ||
               className.contains("TextField", ignoreCase = true)
    }

    fun findFocusedEditableNode(root: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? {
        if (root == null) return null
        if (root.isFocused && isEditableNode(root)) {
            return root
        }

        val directFocus = root.findFocus(AccessibilityNodeInfoCompat.FOCUS_INPUT)
        if (directFocus != null) {
            if (isEditableNode(directFocus)) {
                return directFocus
            }
            directFocus.recycle()
        }

        return findEditableNodeBounded(root)
    }

    private fun findEditableNodeBounded(root: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
        val queue = ArrayDeque<Pair<AccessibilityNodeInfoCompat, Int>>()
        val obtainedChildren = LinkedHashMap<Int, AccessibilityNodeInfoCompat>()
        val visitedIdentities = HashSet<Int>()
        queue.add(root to 0)
        var visitedCount = 0
        var result: AccessibilityNodeInfoCompat? = null

        while (queue.isNotEmpty() && visitedCount < MAX_VISITED_NODES) {
            val (node, depth) = queue.removeFirst()

            val identity = System.identityHashCode(node)
            if (!visitedIdentities.add(identity)) continue
            visitedCount++

            if (node.isFocused && isEditableNode(node)) {
                result = node
                break
            }

            if (depth < MAX_SEARCH_DEPTH) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    queue.add(child to depth + 1)
                    if (child !== root) {
                        obtainedChildren[System.identityHashCode(child)] = child
                    }
                }
            }
        }

        val resultIdentity = result?.let { System.identityHashCode(it) }
        for ((identity, node) in obtainedChildren) {
            if (identity != resultIdentity) node.recycle()
        }
        return result
    }

    fun getNodeBounds(node: AccessibilityNodeInfoCompat?): Rect {
        val rect = Rect()
        if (node != null) {
            node.getBoundsInScreen(rect)
        }
        return rect
    }

    /** [fallbackPackageName] covers nodes whose own packageName reports null — a real,
     * documented Android behavior for some descendant/virtual nodes (e.g. WebView-hosted text
     * inputs) even though the hosting app's package is well defined. Callers should pass the
     * originating AccessibilityEvent's packageName, which the OS populates independently of node
     * traversal. Without this, package-based checks (the self-package skip, per-app exclusions)
     * silently fail open for exactly those nodes: `null in excludedPackages` is always false. */
    fun extractTextPayload(node: AccessibilityNodeInfoCompat?, fallbackPackageName: String? = null): TextPayload? {
        if (node == null) return null
        val text = node.text?.toString() ?: ""
        val bounds = getNodeBounds(node)
        val selectionStart = node.textSelectionStart
        val selectionEnd = node.textSelectionEnd
        val pkg = node.packageName?.toString() ?: fallbackPackageName
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

    fun injectText(node: AccessibilityNodeInfoCompat?, newText: String, context: Context): Boolean {
        if (node == null) return false

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

        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false

            val previousClip = clipboard.primaryClip

            val textLength = node.text?.length ?: 0
            val selectionArgs = Bundle().apply {
                putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT, textLength)
            }
            node.performAction(AccessibilityNodeInfoCompat.ACTION_SET_SELECTION, selectionArgs)

            val clip = ClipData.newPlainText("Stem Replacement", newText).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    description.extras = PersistableBundle().apply {
                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                }
            }

            // Restore the clipboard even if setPrimaryClip or the paste action throws —
            // otherwise a mid-operation failure leaves the transformed text sitting in the
            // user's clipboard indefinitely.
            try {
                clipboard.setPrimaryClip(clip)
                node.performAction(AccessibilityNodeInfoCompat.ACTION_PASTE)
            } finally {
                if (previousClip != null) {
                    clipboard.setPrimaryClip(previousClip)
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}
