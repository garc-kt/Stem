package com.stem.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.stem.app.StemApplication
import com.stem.core.models.EngineMode
import com.stem.core.models.StemUserSettings
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import com.stem.core.util.AccessibilityUtils
import com.stem.core.util.HapticHelper
import com.stem.engine.InlineCommandEngine
import com.stem.engine.LocalRuleEngine
import com.stem.engine.TextEngineProvider
import com.stem.engine.TransformCache
import com.stem.engine.TransformHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch



class StemAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var userSettings = StemUserSettings()

    private var currentActiveNode: AccessibilityNodeInfoCompat? = null

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

            if (focusedNode != null && AccessibilityUtils.isEditableNode(focusedNode)) {
                if (currentActiveNode != focusedNode) {
                    currentActiveNode?.recycle()
                }
                currentActiveNode = focusedNode

                val payload = AccessibilityUtils.extractTextPayload(focusedNode)
                if (payload != null && payload.isValid) {
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
        if (!userSettings.overlayEnabled) return
        if (payload.packageName == packageName) return

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
            is InlineCommandEngine.CommandResult.RunAIPreset -> {
                if (userSettings.engineMode == EngineMode.LOCAL_RULES) {
                    serviceScope.launch {
                        val result = LocalRuleEngine.transform(TextPayload(inlineResult.body), inlineResult.preset)
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
                } else if (userSettings.engineMode == EngineMode.CLAUDE_AI && userSettings.claudeApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your Claude API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                } else if (userSettings.engineMode == EngineMode.OPENAI_COMPATIBLE && userSettings.openaiApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your OpenAI API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                }

                if (userSettings.hapticFeedbackEnabled) {
                    HapticHelper.performClickHaptic(this)
                }

                startSkeletonThinking(
                    originalBody = inlineResult.body,
                    presetName = inlineResult.summary
                ) {
                    val engine = TextEngineProvider.getEngine(userSettings)
                    val result = engine.transform(TextPayload(inlineResult.body), inlineResult.preset)
                    result.transformedText
                }
                return
            }
            is InlineCommandEngine.CommandResult.RunAIPrompt -> {
                if (userSettings.engineMode == EngineMode.LOCAL_RULES) {
                    Toast.makeText(this, "Stem: Select Gemini, Claude, or OpenAI in Settings to use custom AI prompts", Toast.LENGTH_LONG).show()
                } else if (userSettings.engineMode == EngineMode.GEMINI_AI && userSettings.geminiApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your Gemini API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                } else if (userSettings.engineMode == EngineMode.CLAUDE_AI && userSettings.claudeApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your Claude API Key in Settings to use AI", Toast.LENGTH_LONG).show()
                } else if (userSettings.engineMode == EngineMode.OPENAI_COMPATIBLE && userSettings.openaiApiKey.isBlank()) {
                    Toast.makeText(this, "Stem: Add your OpenAI API Key in Settings to use AI", Toast.LENGTH_LONG).show()
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
                    val result = engine.transform(TextPayload(inlineResult.body), TransformPreset.CUSTOM)
                    result.transformedText
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
        onTransform: suspend () -> String
    ) {
        serviceScope.launch {
            var frameIndex = 0
            var isDone = false
            var finalResult = ""

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
                finalResult = onTransform()
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
                Toast.makeText(this@StemAccessibilityService, "Stem: Enhanced", Toast.LENGTH_SHORT).show()
            } else {
                injectReplacementText(
                    newText = originalBody,
                    recordHistory = false
                )
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
        }

        val success = AccessibilityUtils.injectText(targetNode, newText, this)
        if (!success) {
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
}
