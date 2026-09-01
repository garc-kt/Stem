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
        // Nothing else in this class holds onto currentActiveNode beyond a field read at the
        // point of use (injectReplacementText always reads the current value, never a stale
        // captured copy), so the previous node is safe to recycle here on every reassignment.
        val previousNode = currentActiveNode
        if (previousNode != null && previousNode !== node) {
            previousNode.recycle()
        }
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
                        // Resolved explicitly from the user's current language setting rather
                        // than going through LocalRuleEngine's transform()/languagePreference
                        // var — this path calls the apply* helpers directly, and those require
                        // an explicit LanguageRules so this can't silently pick up a stale value
                        // set by some other concurrent caller (overlay panel, sandbox, etc.).
                        val rules = com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.resolveRules(
                            inlineResult.body, userSettings.languagePreference
                        )
                        val transformed = when (inlineResult.preset) {
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.FIX -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyFixAndPolish(inlineResult.body, rules)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.CONCISE -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyConcise(inlineResult.body, rules)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.PROFESSIONAL -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyProfessional(inlineResult.body, rules)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.PUNCHY -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyPunchy(inlineResult.body, rules)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.FRIENDLY -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyFriendly(inlineResult.body, rules)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.SUMMARIZE -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applySummarize(inlineResult.body, rules)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.BULLETIZE -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyBulletize(inlineResult.body, rules)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.EXPAND -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyExpand(inlineResult.body, rules)
                            com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.CUSTOM -> com.veggiebit.sprout.features.enhancement.data.engine.LocalRuleEngine.applyFixAndPolish(inlineResult.body, rules)
                        }
                        if (userSettings.hapticFeedbackEnabled) {
                            HapticHelper.performSuccessHaptic(this)
                        }
                        injectReplacementText(transformed)
                        Toast.makeText(this, "Sprout: ${inlineResult.summary}", Toast.LENGTH_SHORT).show()
                        overlayManager?.hide()
                        return
                    }

                    // Check missing API credentials
                    if (userSettings.engineMode == com.veggiebit.sprout.features.enhancement.data.models.EngineMode.GEMINI_AI && userSettings.geminiApiKey.isBlank()) {
                        Toast.makeText(this, "Sprout: Add your Gemini API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    } else if (userSettings.engineMode == com.veggiebit.sprout.features.enhancement.data.models.EngineMode.CLAUDE_AI && userSettings.claudeApiKey.isBlank()) {
                        Toast.makeText(this, "Sprout: Add your Claude API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    } else if (userSettings.engineMode == com.veggiebit.sprout.features.enhancement.data.models.EngineMode.OPENAI_COMPATIBLE && userSettings.openaiApiKey.isBlank()) {
                        Toast.makeText(this, "Sprout: Add your OpenAI API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    }
                    if (userSettings.hapticFeedbackEnabled) {
                        HapticHelper.performClickHaptic(this)
                    }

                    // Direct In-Field Thinking right on the active text field of the keyboard
                    injectReplacementText("${inlineResult.body} [Thinking...]")

                    serviceScope.launch {
                        val engine = com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider.getEngine(userSettings)
                        val result = engine.transform(com.veggiebit.sprout.features.enhancement.data.models.TextPayload(inlineResult.body), inlineResult.preset)
                        if (result.transformedText.isNotBlank()) {
                            if (userSettings.hapticFeedbackEnabled) {
                                HapticHelper.performSuccessHaptic(this@SproutAccessibilityService)
                            }
                            injectReplacementText(result.transformedText)
                            Toast.makeText(this@SproutAccessibilityService, "Stem: ${result.summaryNote ?: inlineResult.summary}", Toast.LENGTH_SHORT).show()
                        } else {
                            injectReplacementText(inlineResult.body)
                        }
                    }
                    return
                }
                is InlineCommandEngine.CommandResult.RunAIPrompt -> {
                    if (userSettings.engineMode == com.veggiebit.sprout.features.enhancement.data.models.EngineMode.LOCAL_RULES) {
                        Toast.makeText(this, "Stem: Select Gemini, Claude, or OpenAI in Settings to use custom AI prompts", Toast.LENGTH_LONG).show()
                    } else if (userSettings.engineMode == com.veggiebit.sprout.features.enhancement.data.models.EngineMode.GEMINI_AI && userSettings.geminiApiKey.isBlank()) {
                        Toast.makeText(this, "Stem: Add your Gemini API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    } else if (userSettings.engineMode == com.veggiebit.sprout.features.enhancement.data.models.EngineMode.CLAUDE_AI && userSettings.claudeApiKey.isBlank()) {
                        Toast.makeText(this, "Stem: Add your Claude API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    } else if (userSettings.engineMode == com.veggiebit.sprout.features.enhancement.data.models.EngineMode.OPENAI_COMPATIBLE && userSettings.openaiApiKey.isBlank()) {
                        Toast.makeText(this, "Stem: Add your OpenAI API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    }

                    if (userSettings.hapticFeedbackEnabled) {
                        HapticHelper.performClickHaptic(this)
                    }

                    // Direct In-Field Thinking right on the active text field of the keyboard
                    injectReplacementText("${inlineResult.body} [Thinking...]")

                    serviceScope.launch {
                        val customSettings = userSettings.copy(customPromptInstruction = inlineResult.customPrompt)
                        val engine = com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider.getEngine(customSettings)
                        val result = engine.transform(com.veggiebit.sprout.features.enhancement.data.models.TextPayload(inlineResult.body), com.veggiebit.sprout.features.enhancement.data.models.TransformPreset.CUSTOM)
                        if (result.transformedText.isNotBlank()) {
                            if (userSettings.hapticFeedbackEnabled) {
                                HapticHelper.performSuccessHaptic(this@SproutAccessibilityService)
                            }
                            injectReplacementText(result.transformedText)
                            Toast.makeText(this@SproutAccessibilityService, "Stem: Enhanced", Toast.LENGTH_SHORT).show()
                        } else {
                            injectReplacementText(inlineResult.body)
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
        overlayManager?.hide()
    }

    private fun injectReplacementText(newText: String) {
        // The retained node reference can go stale between accessibility events (the source
        // view may have been recycled/rebound). Refresh it before acting on it, and fall back
        // to a fresh focus lookup rather than silently injecting into a dead node.
        var targetNode = currentActiveNode
        val isStillValid = targetNode?.refresh() == true
        if (!isStillValid) {
            val staleNode = targetNode
            val root = rootInActiveWindow
            if (root == null) {
                staleNode?.recycle()
                currentActiveNode = null
                return
            }
            targetNode = AccessibilityUtils.findFocusedEditableNode(AccessibilityNodeInfoCompat.wrap(root))
            staleNode?.recycle()
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
        currentActiveNode?.recycle()
        currentActiveNode = null
        serviceScope.cancel()
        // Zero-persistence policy (plan.md §4.3): history lives in process memory only.
        TransformHistory.clear()
    }
}
