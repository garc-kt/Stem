package com.veggiebit.sprout.features.overlay.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.veggiebit.sprout.app.SproutApplication
import com.veggiebit.sprout.core.utils.AccessibilityUtils
import com.veggiebit.sprout.core.utils.HapticHelper
import com.veggiebit.sprout.core.utils.PermissionHelper
import com.veggiebit.sprout.features.enhancement.data.engine.InlineCommandEngine
import com.veggiebit.sprout.features.enhancement.data.engine.UndoManager
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Monitors IME focus, observes active text buffers, evaluates inline commands/snippets, and injects transformed text.
 */
class SproutAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayManager: SproutOverlayManager? = null
    private var currentActiveNode: AccessibilityNodeInfoCompat? = null
    private var lastObservedPayload: TextPayload? = null
    private var userSettings: SproutUserSettings = SproutUserSettings()

    override fun onServiceConnected() {
        super.onServiceConnected()

        overlayManager = SproutOverlayManager(
            context = this,
            onReplaceRequested = { newText ->
                injectReplacementText(newText)
            }
        )

        serviceScope.launch {
            SproutApplication.instance.preferencesRepository.settingsFlow.collectLatest { settings ->
                userSettings = settings
                if (!settings.overlayEnabled) {
                    overlayManager?.hide()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: ""
        if (userSettings.blacklistedPackages.contains(pkgName)) {
            overlayManager?.hide()
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleNodeInteraction(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val className = event.className?.toString() ?: ""
                if (className.contains("Launcher", ignoreCase = true) || className.contains("Recent", ignoreCase = true)) {
                    overlayManager?.hide()
                }
            }
        }
    }

    private fun handleNodeInteraction(event: AccessibilityEvent) {
        val rawNode = event.source ?: rootInActiveWindow ?: return
        val node = AccessibilityNodeInfoCompat.wrap(rawNode)

        if (!AccessibilityUtils.isEditableNode(node)) {
            val focused = AccessibilityUtils.findFocusedEditableNode(AccessibilityNodeInfoCompat.wrap(rootInActiveWindow ?: return))
            if (focused != null) {
                processEditableNode(focused, isTextChanged = event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)
            } else {
                overlayManager?.hide()
            }
            return
        }

        processEditableNode(node, isTextChanged = event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)
    }

    private fun processEditableNode(node: AccessibilityNodeInfoCompat, isTextChanged: Boolean) {
        currentActiveNode = node
        val payload = AccessibilityUtils.extractTextPayload(node)

        if (payload == null || !payload.isValid) {
            overlayManager?.hide()
            return
        }

        if (isTextChanged) {
            val inlineResult = InlineCommandEngine.evaluate(payload.text, payload.nodeHashCode, userSettings.snippets)
            when (inlineResult) {
                is InlineCommandEngine.CommandResult.SaveSnippet -> {
                    serviceScope.launch {
                        SproutApplication.instance.preferencesRepository.saveSnippet(inlineResult.key, inlineResult.expansion)
                    }
                    if (userSettings.hapticFeedbackEnabled) {
                        HapticHelper.performSuccessHaptic(this)
                    }
                    injectReplacementText(inlineResult.cleanedText)
                    Toast.makeText(this, "Sprout: Saved snippet '${inlineResult.key}'", Toast.LENGTH_SHORT).show()
                    overlayManager?.hide()
                    return
                }
                is InlineCommandEngine.CommandResult.Replaced -> {
                    UndoManager.recordChange(payload.nodeHashCode, payload.text, inlineResult.newText)
                    if (userSettings.hapticFeedbackEnabled) {
                        HapticHelper.performSuccessHaptic(this)
                    }
                    injectReplacementText(inlineResult.newText)
                    Toast.makeText(this, "Sprout: ${inlineResult.summary}", Toast.LENGTH_SHORT).show()
                    overlayManager?.hide()
                    return
                }
                is InlineCommandEngine.CommandResult.None -> {
                    // Continue normal flow
                }
            }
        }

        lastObservedPayload = payload
        // Only show floating UI over apps if explicitly enabled by user
        if (userSettings.overlayEnabled && PermissionHelper.hasOverlayPermission(this)) {
            overlayManager?.show(payload, userSettings.defaultPreset)
        } else {
            overlayManager?.hide()
        }
    }

    private fun injectReplacementText(newText: String) {
        val targetNode = currentActiveNode ?: AccessibilityUtils.findFocusedEditableNode(
            AccessibilityNodeInfoCompat.wrap(rootInActiveWindow ?: return)
        )

        if (targetNode != null) {
            val currentText = targetNode.text?.toString() ?: ""
            if (currentText.isNotEmpty() && currentText != newText) {
                UndoManager.recordChange(targetNode.hashCode(), currentText, newText)
            }
            AccessibilityUtils.injectText(targetNode, newText, this)
        }
    }

    override fun onInterrupt() {
        overlayManager?.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.destroy()
        overlayManager = null
        serviceScope.cancel()
    }
}
