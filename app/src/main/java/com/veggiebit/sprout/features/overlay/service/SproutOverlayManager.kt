package com.veggiebit.sprout.features.overlay.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.veggiebit.sprout.app.SproutApplication
import com.veggiebit.sprout.app.theme.SproutTheme
import com.veggiebit.sprout.core.utils.HapticHelper
import com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider
import com.veggiebit.sprout.features.enhancement.data.engine.TransformCache
import com.veggiebit.sprout.features.enhancement.data.engine.TransformHistory
import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.overlay.ui.SproutFloatingOverlay
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages the floating WindowManager overlay and coordinates Jetpack Compose UI state.
 *
 * The compose surface is created once and reused across hide()/show() cycles rather than
 * being rebuilt every time — rebuilding leaked a ComposeView + LifecycleRegistry +
 * ViewModelStore on every focus change.
 */
class SproutOverlayManager(
    private val context: Context,
    private val onReplaceRequested: (String) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var composeView: ComposeLifecycleServiceView? = null
    private var isViewAttached = false
    private var isDestroyed = false

    private var currentPayload by mutableStateOf<TextPayload?>(null)
    private var currentPreset by mutableStateOf(TransformPreset.FIX)
    private var currentTransformResult by mutableStateOf<TransformResult?>(null)
    private var isExpanded by mutableStateOf(false)
    private var isTransforming by mutableStateOf(false)
    private var canUndo by mutableStateOf(false)

    // Compose-observable (not a plain var) so the composed overlay reacts live to settings
    // changes — theme mode in particular, which otherwise would only apply at the moment the
    // compose surface was first attached.
    private var userSettings: SproutUserSettings by mutableStateOf(SproutUserSettings())
    private var lastShownNodeHashCode: Int? = null
    private var transformJob: Job? = null

    private val layoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        gravity = Gravity.TOP or Gravity.START
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        x = 50
        y = 300
    }

    /**
     * Called on every observed text-field event. Only resets the selected preset and jumps
     * the panel position when the focused node actually changed — otherwise it would clobber
     * the preset the user just picked and yank the panel mid-interaction on every keystroke.
     */
    fun show(payload: TextPayload, defaultPreset: TransformPreset = TransformPreset.FIX) {
        val isNewNode = payload.nodeHashCode != lastShownNodeHashCode
        lastShownNodeHashCode = payload.nodeHashCode
        currentPayload = payload

        if (isNewNode) {
            currentPreset = defaultPreset
            canUndo = TransformHistory.canUndo(payload.nodeHashCode)
            if (!isExpanded) {
                calculateInitialPosition(payload.boundsInScreen)
            }
        }

        scheduleTransformation(payload, currentPreset, isNewNode)

        if (!isViewAttached) {
            attachView()
        } else if (isNewNode && !isExpanded) {
            updateViewLayout()
        }
    }

    fun updateSettings(settings: SproutUserSettings) {
        userSettings = settings
    }

    fun hide() {
        transformJob?.cancel()
        lastShownNodeHashCode = null
        if (isViewAttached && composeView != null) {
            try {
                composeView?.onPause()
                windowManager.removeView(composeView)
            } catch (_: Exception) {}
            isViewAttached = false
            isExpanded = false
        }
    }

    fun destroy() {
        if (isDestroyed) return
        isDestroyed = true
        hide()
        composeView?.onDestroy()
        composeView = null
        serviceScope.cancel()
    }

    private fun scheduleTransformation(payload: TextPayload, preset: TransformPreset, isNewNode: Boolean) {
        transformJob?.cancel()

        val engineSignature = TextEngineProvider.engineSignature(userSettings)
        val cached = TransformCache.get(payload.text, preset, engineSignature)
        if (cached != null) {
            currentTransformResult = cached
            isTransforming = false
            return
        }

        // Reset preset display state when moving to a fresh node so stale suggestions never
        // flash for the wrong field; keep the last result visible while re-debouncing on edits
        // to the same node so the panel doesn't blank out on every keystroke.
        if (isNewNode) {
            currentTransformResult = null
        }

        val debounceMillis = if (userSettings.engineMode.isCloud || userSettings.engineMode == EngineMode.OLLAMA_AI) 1200L else 400L

        transformJob = serviceScope.launch {
            delay(debounceMillis)
            isTransforming = true
            val engine = TextEngineProvider.getEngine(userSettings)
            val result = engine.transform(payload, preset)
            TransformCache.put(payload.text, preset, engineSignature, result)
            currentTransformResult = result
            isTransforming = false
        }
    }

    private fun attachView() {
        val view = composeView ?: ComposeLifecycleServiceView(context).apply {
            setContent {
                SproutTheme(themeMode = userSettings.themeMode) {
                    val historySnapshot by TransformHistory.history.collectAsState()
                    val nodeHistory = remember(historySnapshot, currentPayload?.nodeHashCode) {
                        val nodeHash = currentPayload?.nodeHashCode
                        if (nodeHash == null) emptyList() else historySnapshot.filter { it.nodeHashCode == nodeHash }
                    }

                    SproutFloatingOverlay(
                        payload = currentPayload,
                        transformResult = currentTransformResult,
                        isTransforming = isTransforming,
                        selectedPreset = currentPreset,
                        presets = userSettings.orderedPresets,
                        isExpanded = isExpanded,
                        canUndo = canUndo,
                        historyEntries = nodeHistory,
                        onExpandClick = {
                            isExpanded = true
                            if (userSettings.hapticFeedbackEnabled) {
                                HapticHelper.performClickHaptic(context)
                            }
                            updateExpandedLayoutParams()
                        },
                        onCollapseClick = {
                            isExpanded = false
                            updateCollapsedLayoutParams()
                        },
                        onPresetSelected = { preset ->
                            currentPreset = preset
                            if (userSettings.hapticFeedbackEnabled) {
                                HapticHelper.performClickHaptic(context)
                            }
                            currentPayload?.let { scheduleTransformation(it, preset, isNewNode = false) }
                        },
                        onReplaceInline = { result ->
                            if (userSettings.hapticFeedbackEnabled) {
                                HapticHelper.performSuccessHaptic(context)
                            }
                            onReplaceRequested(result.transformedText)
                            Toast.makeText(context, "Sprout: Injected ${result.preset.shortName} text", Toast.LENGTH_SHORT).show()
                            isExpanded = false
                            updateCollapsedLayoutParams()
                        },
                        onCopyText = { text ->
                            copyToClipboard(text)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onUndoClick = {
                            val previous = TransformHistory.popUndo(currentPayload?.nodeHashCode) ?: TransformHistory.popUndo()
                            if (previous != null) {
                                if (userSettings.hapticFeedbackEnabled) {
                                    HapticHelper.performClickHaptic(context)
                                }
                                onReplaceRequested(previous)
                                Toast.makeText(context, "Sprout: Undid last change", Toast.LENGTH_SHORT).show()
                                isExpanded = false
                                updateCollapsedLayoutParams()
                            }
                        },
                        onHistoryEntrySelected = { entry ->
                            if (userSettings.hapticFeedbackEnabled) {
                                HapticHelper.performClickHaptic(context)
                            }
                            onReplaceRequested(entry.replacedText)
                            Toast.makeText(context, "Sprout: Restored a previous replacement", Toast.LENGTH_SHORT).show()
                        },
                        onPillDrag = ::dragPillBy,
                        onPillDragEnd = {
                            snapPillToEdgeAndPersist()
                        },
                        onDismiss = {
                            hide()
                        }
                    )
                }
            }
        }

        composeView = view
        try {
            windowManager.addView(view, layoutParams)
            view.onResume()
            isViewAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Applies a live drag delta to the pill's WindowManager position. Kept as a plain member
     * function (like [calculateInitialPosition]/[snapPillToEdgeAndPersist]) rather than an
     * inline lambda inside the deeply-nested `setContent { SproutTheme { SproutFloatingOverlay(...) } }`
     * call — accessing `layoutParams.x`/`.y` directly from inside that nested composable lambda
     * failed to resolve on this toolchain even for a plain (non-lambda) local variable copy,
     * while every access from an ordinary member function works fine. */
    private fun dragPillBy(dx: Float, dy: Float) {
        layoutParams.x = layoutParams.x + dx.toInt()
        layoutParams.y = layoutParams.y + dy.toInt()
        updateViewLayout()
    }

    private fun calculateInitialPosition(nodeBounds: Rect?) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Once the user has dragged the pill at least once, respect where they put it instead
        // of re-deriving a cursor-relative position on every new field — that's the whole
        // point of letting them move it. -1f is the "never dragged" sentinel.
        if (userSettings.pillAnchorXFraction >= 0f && userSettings.pillAnchorYFraction >= 0f) {
            layoutParams.x = (userSettings.pillAnchorXFraction * screenWidth).toInt()
            layoutParams.y = (userSettings.pillAnchorYFraction * screenHeight).toInt()
            return
        }

        if (nodeBounds == null || nodeBounds.isEmpty) return

        var targetX = nodeBounds.right - 140
        var targetY = nodeBounds.top - 120

        if (targetX < 20) targetX = 20
        if (targetX > screenWidth - 180) targetX = screenWidth - 180

        if (targetY < 100) {
            targetY = nodeBounds.bottom + 20
        }
        if (targetY > screenHeight - 200) {
            targetY = screenHeight - 240
        }

        layoutParams.x = targetX
        layoutParams.y = targetY
    }

    /** Snaps the pill to the nearer horizontal edge after a drag, then persists the anchor as
     * a fraction of screen size (so it survives rotation and different devices). Uses a manual
     * delay-stepped ease rather than Compose's [Animatable] — this runs from a plain
     * service-level [CoroutineScope], not a Composition, so the `MonotonicFrameClock`
     * `Animatable.animateTo` needs isn't available here and would throw. */
    private fun snapPillToEdgeAndPersist() {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val clampedY = layoutParams.y.coerceIn(0, maxOf(0, screenHeight - 80))
        val targetX = if (layoutParams.x + 60 < screenWidth / 2) 20 else screenWidth - 180
        val startX = layoutParams.x

        serviceScope.launch {
            val steps = 12
            repeat(steps) { step ->
                val fraction = (step + 1) / steps.toFloat()
                val eased = 1f - (1f - fraction) * (1f - fraction) * (1f - fraction) // ease-out cubic
                layoutParams.x = (startX + (targetX - startX) * eased).toInt()
                layoutParams.y = clampedY
                updateViewLayout()
                delay(16)
            }
            layoutParams.x = targetX
            layoutParams.y = clampedY
            updateViewLayout()

            SproutApplication.instance.preferencesRepository.setPillAnchor(
                xFraction = targetX.toFloat() / screenWidth,
                yFraction = clampedY.toFloat() / screenHeight
            )
        }
    }

    private fun updateExpandedLayoutParams() {
        val displayMetrics = context.resources.displayMetrics
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.x = 0
        layoutParams.y = maxOf(60, (displayMetrics.heightPixels * 0.35).toInt())
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        updateViewLayout()
    }

    private fun updateCollapsedLayoutParams() {
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        currentPayload?.boundsInScreen?.let { calculateInitialPosition(it) }
        updateViewLayout()
    }

    private fun updateViewLayout() {
        if (isViewAttached && composeView != null) {
            try {
                windowManager.updateViewLayout(composeView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Sprout Text", text)
        clipboard.setPrimaryClip(clip)
    }
}
