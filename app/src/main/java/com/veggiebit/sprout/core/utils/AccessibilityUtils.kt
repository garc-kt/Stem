package com.veggiebit.sprout.core.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import java.util.ArrayDeque

object AccessibilityUtils {

    /** Depth/size caps for the fallback tree walk — a Chrome/WebView tree can be thousands of
     * nodes deep; without a bound this recursion runs unbounded binder calls on the main thread. */
    private const val MAX_SEARCH_DEPTH = 12
    private const val MAX_VISITED_NODES = 400

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
     * Locates the currently focused editable node. Tries the cheap direct focus lookup first;
     * only falls back to a bounded iterative breadth-first walk (depth- and node-count-capped)
     * if that fails, to avoid unbounded main-thread binder traversal on deep view hierarchies.
     */
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
            // Obtained via findFocus() but not what we need — must recycle before falling
            // through, or it leaks (see findEditableNodeBounded's doc for why this matters).
            directFocus.recycle()
        }

        return findEditableNodeBounded(root)
    }

    /**
     * Every [AccessibilityNodeInfoCompat] obtained via [AccessibilityNodeInfoCompat.getChild]
     * must be recycled by whoever obtained it once it's no longer needed — on API < 33 these
     * come from a small, finite per-process object pool (recycle() is a documented no-op from
     * 33 onward, but minSdk here is 26). This walk fires on every keystroke via
     * [AccessibilityUtils.findFocusedEditableNode], so an unbounded leak here can exhaust that
     * pool mid-session on older API levels. [root] is caller-owned and is never recycled here.
     */
    private fun findEditableNodeBounded(root: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
        val queue = ArrayDeque<Pair<AccessibilityNodeInfoCompat, Int>>()
        // Keyed by identity so a node reachable via more than one path is recycled exactly
        // once, and recycling only happens after the walk fully completes — never while a
        // node might still be sitting unprocessed in the queue.
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

        // 2. Clipboard fallback — select the full existing buffer first so paste *replaces* it
        // instead of inserting at the caret, then restore whatever clip the user had before.
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

            val clip = ClipData.newPlainText("Sprout Replacement", newText).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    description.extras = PersistableBundle().apply {
                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                }
            }
            clipboard.setPrimaryClip(clip)

            val pasted = node.performAction(AccessibilityNodeInfoCompat.ACTION_PASTE)

            // Restore whatever was on the clipboard before Sprout touched it — the transformed
            // text should not linger there once injection completes.
            if (previousClip != null) {
                clipboard.setPrimaryClip(previousClip)
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }

            pasted
        } catch (_: Exception) {
            false
        }
    }
}
