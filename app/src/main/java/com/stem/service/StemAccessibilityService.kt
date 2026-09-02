package com.stem.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.stem.app.StemApplication
import com.stem.core.models.EngineMode
import com.stem.core.models.PersistedHistoryEntry
import com.stem.core.models.StemUserSettings
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import com.stem.core.models.TransformResult
import com.stem.core.util.AccessibilityUtils
import com.stem.core.util.HapticHelper
import com.stem.engine.InlineCommandEngine
import com.stem.engine.LocalRuleEngine
import com.stem.engine.TextEngineProvider
import com.stem.engine.TransformCache
import com.stem.engine.TransformHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout



class StemAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var userSettings = StemUserSettings()

    private var currentActiveNode: AccessibilityNodeInfoCompat? = null

    // A second trigger firing while an AI transform is still animating/in flight (a different
    // field, or the same one again) previously launched a second concurrent coroutine, so two
    // animation loops fought over injectReplacementText. Only one may run at a time; a new
    // trigger cancels whatever is still running.
    private var activeThinkingJob: Job? = null

    // Suppresses reprocessing of the accessibility event Stem's own text injection generates.
    // A boolean flag cleared synchronously right after injectText() returns doesn't work here:
    // the OS delivers the resulting AccessibilityEvent asynchronously, arriving after the flag
    // is already false, so the service would reprocess its own output as if the user had typed
    // it — replaying whatever inline command just triggered. A short time window keyed to the
    // specific node closes that race without blocking real input from a different field.
    @Volatile
    private var suppressedNodeHash: Int = 0
    @Volatile
    private var suppressUntilMs: Long = 0L

    private fun isSuppressed(nodeHashCode: Int): Boolean {
        return nodeHashCode == suppressedNodeHash && System.currentTimeMillis() < suppressUntilMs
    }

    private fun markSuppressed(nodeHashCode: Int) {
        suppressedNodeHash = nodeHashCode
        suppressUntilMs = System.currentTimeMillis() + SUPPRESSION_WINDOW_MS
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            StemApplication.instance.preferencesRepository.settingsFlow.collectLatest { settings ->
                userSettings = settings
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            val rootNode = rootInActiveWindow ?: return
            val rootCompat = AccessibilityNodeInfoCompat.wrap(rootNode)
            val focusedNode = AccessibilityUtils.findFocusedEditableNode(rootCompat)

            // findFocusedEditableNode returns the root instance itself when the root is the
            // focused editable node; only recycle the root wrapper when it's a distinct object
            // we're done with, not the node we're about to keep.
            if (focusedNode !== rootCompat) {
                rootCompat.recycle()
            }

            if (focusedNode != null && AccessibilityUtils.isEditableNode(focusedNode)) {
                // Reference equality, not AccessibilityNodeInfoCompat's structural equals():
                // each traversal call obtains a distinct native node object even when it
                // represents the same on-screen field, so a structurally-equal-but-different
                // instance must still be recycled here or its resource leaks.
                if (currentActiveNode !== focusedNode) {
                    currentActiveNode?.recycle()
                }
                currentActiveNode = focusedNode

                val payload = AccessibilityUtils.extractTextPayload(focusedNode)
                if (payload != null && payload.isValid && !isSuppressed(payload.nodeHashCode)) {
                    handleTextPayload(payload)
                }
            } else {
                focusedNode?.recycle()
                currentActiveNode?.recycle()
                currentActiveNode = null
            }
        }
    }

    override fun onInterrupt() {
    }

    private fun handleTextPayload(payload: TextPayload) {
        if (!userSettings.serviceEnabled) return
        if (payload.packageName == packageName) return
        if (payload.packageName in userSettings.excludedPackages) return

        val currentText = payload.text.trimEnd()
        if (currentText.length < 2) return

        val inlineResult = InlineCommandEngine.evaluate(
            text = currentText,
            nodeHashCode = payload.nodeHashCode,
            snippets = userSettings.snippets,
            customCommands = userSettings.customCommands
        )
        when (inlineResult) {
            is InlineCommandEngine.CommandResult.SaveCustomCommand -> {
                serviceScope.launch {
                    StemApplication.instance.preferencesRepository.saveCustomCommand(inlineResult.trigger, inlineResult.prompt)
                }
                if (userSettings.hapticFeedbackEnabled) {
                    HapticHelper.performSuccessHaptic(this)
                }
                injectReplacementText(inlineResult.cleanedText, recordHistory = false)
                Toast.makeText(this, "Stem: Saved command '?${inlineResult.trigger}'", Toast.LENGTH_SHORT).show()
                return
            }
            is InlineCommandEngine.CommandResult.SaveSnippet -> {
                serviceScope.launch {
                    StemApplication.instance.preferencesRepository.saveSnippet(inlineResult.key, inlineResult.expansion)
                }
                if (userSettings.hapticFeedbackEnabled) {
                    HapticHelper.performSuccessHaptic(this)
                }
                injectReplacementText(inlineResult.cleanedText, recordHistory = false)
                Toast.makeText(this, "Stem: Saved snippet '..${inlineResult.key}'", Toast.LENGTH_SHORT).show()
                return
            }
            is InlineCommandEngine.CommandResult.Replaced -> {
                if (userSettings.hapticFeedbackEnabled) {
                    HapticHelper.performSuccessHaptic(this)
                }
                injectReplacementText(
                    newText = inlineResult.newText,
                    recordHistory = true,
                    explicitOriginal = currentText,
                    presetName = inlineResult.summary
                )
                Toast.makeText(this, "Stem: ${inlineResult.summary}", Toast.LENGTH_SHORT).show()
                return
            }
            is InlineCommandEngine.CommandResult.Undo -> {
                val previous = TransformHistory.popUndo(inlineResult.nodeHashCode) ?: TransformHistory.popUndo()
                if (previous != null) {
                    if (userSettings.hapticFeedbackEnabled) {
                        HapticHelper.performSuccessHaptic(this)
                    }
                    injectReplacementText(newText = previous, recordHistory = false)
                    Toast.makeText(this, "Stem: ?undo", Toast.LENGTH_SHORT).show()
                }
                return
            }
            is InlineCommandEngine.CommandResult.RunAIPreset -> {
                if (userSettings.engineMode == EngineMode.LOCAL_RULES) {
                    serviceScope.launch {
                        val result = LocalRuleEngine.transform(TextPayload(inlineResult.body), inlineResult.preset, userSettings.languagePreference)
                        if (userSettings.hapticFeedbackEnabled) {
                            HapticHelper.performSuccessHaptic(this@StemAccessibilityService)
                        }
                        injectReplacementText(
                            newText = result.transformedText,
                            recordHistory = true,
                            explicitOriginal = inlineResult.body,
                            presetName = inlineResult.summary
                        )
                        Toast.makeText(this@StemAccessibilityService, "Stem: Applied ${inlineResult.summary}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                if (userSettings.engineMode == EngineMode.GEMINI_AI && userSettings.geminiApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your Gemini API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    return
                } else if (userSettings.engineMode == EngineMode.CLAUDE_AI && userSettings.claudeApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your Claude API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    return
                } else if (userSettings.engineMode == EngineMode.OPENAI_COMPATIBLE && userSettings.openaiApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your OpenAI API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    return
                }

                if (userSettings.hapticFeedbackEnabled) {
                    HapticHelper.performClickHaptic(this)
                }

                startSkeletonThinking(
                    originalBody = inlineResult.body,
                    presetName = inlineResult.summary
                ) {
                    val engine = TextEngineProvider.getEngine(userSettings)
                    engine.transform(TextPayload(inlineResult.body), inlineResult.preset, userSettings.languagePreference)
                }
                return
            }
            is InlineCommandEngine.CommandResult.RunAIPrompt -> {
                if (userSettings.engineMode == EngineMode.LOCAL_RULES) {
                    Toast.makeText(this, "Stem: Select Gemini, Claude, or OpenAI in Settings to use custom AI prompts", Toast.LENGTH_LONG).show()
                    return
                } else if (userSettings.engineMode == EngineMode.GEMINI_AI && userSettings.geminiApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your Gemini API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    return
                } else if (userSettings.engineMode == EngineMode.CLAUDE_AI && userSettings.claudeApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your Claude API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    return
                } else if (userSettings.engineMode == EngineMode.OPENAI_COMPATIBLE && userSettings.openaiApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your OpenAI API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                    return
                }

                if (userSettings.hapticFeedbackEnabled) {
                    HapticHelper.performClickHaptic(this)
                }

                startSkeletonThinking(
                    originalBody = inlineResult.body,
                    presetName = inlineResult.summary
                ) {
                    val customSettings = userSettings.copy(customPromptInstruction = inlineResult.customPrompt)
                    val engine = TextEngineProvider.getEngine(customSettings)
                    engine.transform(TextPayload(inlineResult.body), TransformPreset.CUSTOM, customSettings.languagePreference)
                }
                return
            }
            is InlineCommandEngine.CommandResult.None -> {
            }
        }
    }

    private fun startSkeletonThinking(
        originalBody: String,
        presetName: String = "Enhance",
        onTransform: suspend () -> TransformResult
    ) {
        activeThinkingJob?.cancel()
        activeThinkingJob = serviceScope.launch {
            var frameIndex = 0
            var isDone = false
            var finalResult = ""
            var errorMessage: String? = null

            val tokens = originalBody.split(Regex("(?<=\\s)|(?=\\s)|(?<=[^\\w\\s])|(?=[^\\w\\s])"))
            val wordIndices = tokens.mapIndexedNotNull { index, token ->
                if (token.isNotBlank() && token.any { it.isLetterOrDigit() }) index else null
            }

            val animationJob = launch {
                while (!isDone) {
                    val frameText = if (wordIndices.isEmpty()) {
                        val dotFrames = listOf("● · ·", "· ● ·", "· · ●", "· ● ·")
                        dotFrames[frameIndex % dotFrames.size]
                    } else {
                        val activeWordPos = (frameIndex % wordIndices.size)
                        val sb = StringBuilder()
                        var wordCount = 0
                        for (token in tokens) {
                            if (token.isBlank() || !token.any { it.isLetterOrDigit() }) {
                                sb.append(token)
                            } else {
                                val dist = kotlin.math.abs(wordCount - activeWordPos)
                                val (activeChar, inactiveChar) = when (dist) {
                                    0 -> '●' to '•'
                                    1 -> '•' to '·'
                                    else -> '·' to '·'
                                }
                                for (i in token.indices) {
                                    sb.append(if (dist == 0) activeChar else inactiveChar)
                                }
                                wordCount++
                            }
                        }
                        sb.toString()
                    }

                    injectReplacementText(
                        newText = frameText,
                        recordHistory = false
                    )
                    frameIndex++
                    delay(110)
                }
            }

            try {
                val result = withTimeout(TRANSFORM_TIMEOUT_MS) { onTransform() }
                finalResult = result.transformedText
                errorMessage = result.errorMessage
            } catch (_: TimeoutCancellationException) {
                errorMessage = "Request timed out"
            } finally {
                isDone = true
                animationJob.cancelAndJoin()
            }

            if (finalResult.isNotBlank()) {
                if (userSettings.hapticFeedbackEnabled) {
                    HapticHelper.performSuccessHaptic(this@StemAccessibilityService)
                }
                injectReplacementText(
                    newText = finalResult,
                    recordHistory = true,
                    explicitOriginal = originalBody,
                    presetName = presetName
                )
                val toastText = if (errorMessage != null) {
                    "Stem: engine unavailable ($errorMessage) — used local rules"
                } else {
                    "Stem: Enhanced"
                }
                Toast.makeText(this@StemAccessibilityService, toastText, Toast.LENGTH_LONG).show()
            } else {
                injectReplacementText(
                    newText = originalBody,
                    recordHistory = false
                )
                Toast.makeText(this@StemAccessibilityService, "Stem: Could not enhance text - restored original", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun injectReplacementText(
        newText: String,
        recordHistory: Boolean = true,
        explicitOriginal: String? = null,
        presetName: String = "Enhance"
    ) {
        var targetNode = currentActiveNode
        val isStillValid = targetNode?.refresh() == true
        if (!isStillValid) {
            val staleNode = targetNode
            val freshlyFound = rootInActiveWindow?.let {
                AccessibilityUtils.findFocusedEditableNode(AccessibilityNodeInfoCompat.wrap(it))
            }
            staleNode?.recycle()
            if (freshlyFound == null) {
                currentActiveNode = null
                return
            }
            currentActiveNode = freshlyFound
            targetNode = freshlyFound
        }

        val originalToRecord = explicitOriginal ?: (targetNode.text?.toString() ?: "")

        if (recordHistory && originalToRecord.isNotBlank() && originalToRecord != newText) {
            TransformHistory.recordChange(
                nodeHashCode = targetNode.hashCode(),
                original = originalToRecord,
                replaced = newText,
                presetName = presetName
            )
            // TransformHistory above is the in-memory undo stack, cleared on every service
            // restart. This is the separate, persisted browsing log the History tab reads.
            serviceScope.launch {
                StemApplication.instance.preferencesRepository.addHistoryEntry(
                    PersistedHistoryEntry(
                        id = java.util.UUID.randomUUID().toString(),
                        originalText = originalToRecord,
                        replacedText = newText,
                        presetName = presetName,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        val success = AccessibilityUtils.injectText(targetNode, newText, this)
        if (success) {
            markSuppressed(targetNode.hashCode())
        } else {
            targetNode.recycle()
            currentActiveNode = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentActiveNode?.recycle()
        currentActiveNode = null
        TransformHistory.clear()
        TransformCache.clear()
        serviceScope.cancel()
    }

    companion object {
        // Must comfortably exceed the OS's async AccessibilityEvent delivery latency (normally
        // well under 100ms) so the reflected event from our own injection never slips through.
        private const val SUPPRESSION_WINDOW_MS = 400L

        // Below HttpClientFactory's 30s OkHttp read timeout so the thinking animation gives up
        // and restores the original text instead of animating indefinitely on a stalled request.
        private const val TRANSFORM_TIMEOUT_MS = 20_000L
    }
}
