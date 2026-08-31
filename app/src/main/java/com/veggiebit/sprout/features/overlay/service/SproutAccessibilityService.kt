package com.veggiebit.sprout.features.overlay.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.veggiebit.sprout.app.SproutApplication
import com.veggiebit.sprout.core.utils.AccessibilityUtils
import com.veggiebit.sprout.core.utils.HapticHelper
import com.veggiebit.sprout.core.utils.PermissionHelper
import com.veggiebit.sprout.features.enhancement.data.engine.InlineCommandEngine
import com.veggiebit.sprout.features.enhancement.data.engine.TransformHistory
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
                overlayManager?.updateSettings(settings)
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
            val inlineResult = InlineCommandEngine.evaluate(
                text = payload.text,
                nodeHashCode = payload.nodeHashCode,
                snippets = userSettings.snippets,
                customCommands = userSettings.customCommands
            )
            when (inlineResult) {
                is InlineCommandEngine.CommandResult.SaveCustomCommand -> {
                    serviceScope.launch {
                        SproutApplication.instance.preferencesRepository.saveCustomCommand(inlineResult.trigger, inlineResult.prompt)
                    }
                    if (userSettings.hapticFeedbackEnabled) {
                        HapticHelper.performSuccessHaptic(this)
                    }
                    injectReplacementText(inlineResult.cleanedText)
                    Toast.makeText(this, "Sprout: Saved command '?${inlineResult.trigger}'", Toast.LENGTH_SHORT).show()
                    overlayManager?.hide()
                    return
                }
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
                    if (userSettings.hapticFeedbackEnabled) {
                        HapticHelper.performSuccessHaptic(this)
                    }
                    injectReplacementText(inlineResult.newText)
                    Toast.makeText(this, "Sprout: ${inlineResult.summary}", Toast.LENGTH_SHORT).show()
                    overlayManager?.hide()
                    return
                }
                is InlineCommandEngine.CommandResult.RunAIPreset -> {
                    if (userSettings.engineMode == com.veggiebit.sprout.features.enhancement.data.models.EngineMode.LOCAL_RULES) {
                        val transformed = when (inlineResult.preset) {
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.FIX -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyFixAndPolish(inlineResult.body)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.CONCISE -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyConcise(inlineResult.body)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.PROFESSIONAL -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyProfessional(inlineResult.body)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.PUNCHY -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyPunchy(inlineResult.body)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.FRIENDLY -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyFriendly(inlineResult.body)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.SUMMARIZE -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applySummarize(inlineResult.body)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.BULLETIZE -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyBulletize(inlineResult.body)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.EXPAND -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyExpand(inlineResult.body)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.CUSTOM -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyFixAndPolish(inlineResult.body)
                        }
                        if (userSettings.hapticFeedbackEnabled) {
                            HapticHelper.performSuccessHaptic(this)
                        }
                        injectReplacementText(transformed)
                        Toast.makeText(this, "Sprout: ${inlineResult.summary}", Toast.LENGTH_SHORT).show()
                        overlayManager?.hide()
                        return
                    }

                    if (userSettings.hapticFeedbackEnabled) {
                        HapticHelper.performClickHaptic(this)
                    }
                    Toast.makeText(this, "Sprout: Thinking (${userSettings.engineMode.title})...", Toast.LENGTH_SHORT).show()
                    overlayManager?.hide()

                    serviceScope.launch {
                        val engine = com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider.getEngine(userSettings)
                        val result = engine.transform(com.veggiebit.sprout.features.enhancement.data.models.TextPayload(inlineResult.body), inlineResult.preset)
                        if (result.transformedText.isNotBlank() && result.transformedText != inlineResult.body) {
                            if (userSettings.hapticFeedbackEnabled) {
                                HapticHelper.performSuccessHaptic(this@SproutAccessibilityService)
                            }
                            injectReplacementText(result.transformedText)
                            Toast.makeText(this@SproutAccessibilityService, "Sprout: ${result.summaryNote ?: inlineResult.summary}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return
                }
                is InlineCommandEngine.CommandResult.RunAIPrompt -> {
                    if (userSettings.hapticFeedbackEnabled) {
                        HapticHelper.performClickHaptic(this)
                    }
                    Toast.makeText(this, "Sprout: Thinking (${userSettings.engineMode.title})...", Toast.LENGTH_SHORT).show()
                    overlayManager?.hide()

                    serviceScope.launch {
                        val customSettings = userSettings.copy(customPromptInstruction = inlineResult.customPrompt)
                        val engine = com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider.getEngine(customSettings)
                        val result = engine.transform(com.veggiebit.sprout.features.enhancement.data.models.TextPayload(inlineResult.body), com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.CUSTOM)
                        if (result.transformedText.isNotBlank() && result.transformedText != inlineResult.body) {
                            if (userSettings.hapticFeedbackEnabled) {
                                HapticHelper.performSuccessHaptic(this@SproutAccessibilityService)
                            }
                            injectReplacementText(result.transformedText)
                            Toast.makeText(this@SproutAccessibilityService, "Sprout: ${result.summaryNote ?: inlineResult.summary}", Toast.LENGTH_SHORT).show()
                        }
                    }
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
        // The retained node reference can go stale between accessibility events (the source
        // view may have been recycled/rebound). Refresh it before acting on it, and fall back
        // to a fresh focus lookup rather than silently injecting into a dead node.
        var targetNode = currentActiveNode
        val isStillValid = targetNode?.refresh() == true
        if (!isStillValid) {
            targetNode = AccessibilityUtils.findFocusedEditableNode(
                AccessibilityNodeInfoCompat.wrap(rootInActiveWindow ?: return)
            )
            currentActiveNode = targetNode
        }

        if (targetNode != null) {
            val currentText = targetNode.text?.toString() ?: ""
            if (currentText.isNotEmpty() && currentText != newText) {
                TransformHistory.recordChange(targetNode.hashCode(), currentText, newText)
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
        // Zero-persistence policy (plan.md §4.3): history lives in process memory only.
        TransformHistory.clear()
    }
}
