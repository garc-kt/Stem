package com.veggiebit.sprout.features.overlay.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.veggiebit.sprout.app.SproutApplication
import com.veggiebit.sprout.app.theme.SproutTheme
import com.veggiebit.sprout.core.utils.HapticHelper
import com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider
import com.veggiebit.sprout.features.enhancement.data.engine.UndoManager
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.overlay.ui.SproutFloatingOverlay
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages the floating WindowManager overlay and coordinates Jetpack Compose UI state.
 */
class SproutOverlayManager(
    private val context: Context,
    private val onReplaceRequested: (String) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var composeView: ComposeLifecycleServiceView? = null
    private var isViewAttached = false

    private var currentPayload by mutableStateOf<TextPayload?>(null)
    private var currentPreset by mutableStateOf(TransformPreset.FIX)
    private var currentTransformResult by mutableStateOf<TransformResult?>(null)
    private var isExpanded by mutableStateOf(false)
    private var canUndo by mutableStateOf(false)
    private var userSettings: SproutUserSettings = SproutUserSettings()

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

    fun show(payload: TextPayload, defaultPreset: TransformPreset = TransformPreset.FIX) {
        currentPayload = payload
        currentPreset = defaultPreset
        canUndo = UndoManager.canUndo(payload.nodeHashCode)
        computeTransformation(payload, defaultPreset)
        calculateInitialPosition(payload.boundsInScreen)

        if (!isViewAttached) {
            attachView()
        } else {
            updateViewLayout()
        }
    }

    fun updateSettings(settings: SproutUserSettings) {
        userSettings = settings
    }

    fun updatePayload(payload: TextPayload) {
        currentPayload = payload
        canUndo = UndoManager.canUndo(payload.nodeHashCode)
        computeTransformation(payload, currentPreset)
        if (isViewAttached) {
            calculateInitialPosition(payload.boundsInScreen)
            updateViewLayout()
        }
    }

    fun hide() {
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
        hide()
        composeView?.onDestroy()
        composeView = null
        serviceScope.cancel()
    }

    private fun attachView() {
        val view = ComposeLifecycleServiceView(context).apply {
            setContent {
                SproutTheme {
                    SproutFloatingOverlay(
                        payload = currentPayload,
                        transformResult = currentTransformResult,
                        selectedPreset = currentPreset,
                        isExpanded = isExpanded,
                        canUndo = canUndo,
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
                            currentPayload?.let { computeTransformation(it, preset) }
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
                            val previous = UndoManager.popUndo(currentPayload?.nodeHashCode) ?: UndoManager.popUndo()
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

    private fun computeTransformation(payload: TextPayload, preset: TransformPreset) {
        serviceScope.launch {
            val settings = SproutApplication.instance.preferencesRepository.settingsFlow.first()
            userSettings = settings
            val engine = TextEngineProvider.getEngine(settings)
            val result = engine.transform(payload, preset)
            currentTransformResult = result
        }
    }

    private fun calculateInitialPosition(nodeBounds: Rect?) {
        if (nodeBounds == null || nodeBounds.isEmpty) return
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

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
