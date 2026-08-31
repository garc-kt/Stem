package com.veggiebit.sprout.features.settings.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veggiebit.sprout.app.SproutApplication
import com.veggiebit.sprout.app.theme.SproutTheme
import com.veggiebit.sprout.core.utils.PermissionHelper
import com.veggiebit.sprout.features.overlay.service.SproutAccessibilityService
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasAccessibilityPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            val preferencesRepo = SproutApplication.instance.preferencesRepository
            val settings by preferencesRepo.settingsFlow.collectAsStateWithLifecycle(
                initialValue = SproutUserSettings()
            )

            SproutTheme {
                SettingsScreen(
                    userSettings = settings,
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onRequestOverlayPermission = {
                        PermissionHelper.requestOverlayPermission(this)
                    },
                    onRequestAccessibilityPermission = {
                        PermissionHelper.openAccessibilitySettings(this)
                    },
                    onToggleOverlay = { enabled ->
                        scope.launch { preferencesRepo.setOverlayEnabled(enabled) }
                    },
                    onSelectDefaultPreset = { preset ->
                        scope.launch { preferencesRepo.setDefaultPreset(preset) }
                    },
                    onToggleHaptics = { enabled ->
                        scope.launch { preferencesRepo.setHapticFeedback(enabled) }
                    },
                    onSelectEngineMode = { mode ->
                        scope.launch { preferencesRepo.setEngineMode(mode) }
                    },
                    onSaveOllamaUrl = { url ->
                        scope.launch { preferencesRepo.setOllamaBaseUrl(url) }
                    },
                    onSaveOllamaModel = { model ->
                        scope.launch { preferencesRepo.setOllamaModel(model) }
                    },
                    onSaveGeminiSettings = { apiKey, model ->
                        scope.launch { preferencesRepo.setGeminiSettings(apiKey, model) }
                    },
                    onSaveOpenAISettings = { baseUrl, apiKey, model ->
                        scope.launch { preferencesRepo.setOpenAISettings(baseUrl, apiKey, model) }
                    },
                    onSaveClaudeSettings = { apiKey, model ->
                        scope.launch { preferencesRepo.setClaudeSettings(apiKey, model) }
                    },
                    onSaveSnippet = { key, expansion ->
                        scope.launch { preferencesRepo.saveSnippet(key, expansion) }
                    },
                    onDeleteSnippet = { key ->
                        scope.launch { preferencesRepo.deleteSnippet(key) }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermission = PermissionHelper.hasOverlayPermission(this)
        hasAccessibilityPermission = PermissionHelper.isAccessibilityServiceEnabled(
            this,
            SproutAccessibilityService::class.java
        )
    }
}
