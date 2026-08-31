package com.veggiebit.sprout.features.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veggiebit.sprout.app.SproutApplication
import com.veggiebit.sprout.app.theme.ThemeMode
import com.veggiebit.sprout.features.enhancement.data.engine.TransformHistory
import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.enhancement.data.models.LanguagePreference
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.settings.data.AppRuleMode
import com.veggiebit.sprout.features.settings.data.PreferencesRepository
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Hoists all settings state and every write path behind explicit method calls. Replaces the
 * old SettingsScreen composable's own [androidx.compose.runtime.mutableStateOf] fields wired
 * directly to onValueChange, which persisted every keystroke straight to DataStore — for the
 * API-key fields that meant one write (and, once collected back through settingsFlow, one
 * re-triggered sandbox transform) per character typed. Text fields now hold local Compose
 * state and call the save* methods here only on an explicit action (blur/button), see
 * SandboxScreen / EngineScreen.
 */
class SettingsViewModel : ViewModel() {

    private val repository: PreferencesRepository = SproutApplication.instance.preferencesRepository

    // DataStore reads are near-instant but not synchronous — without this, the nav host would
    // have to guess a start destination (Onboarding vs Home) before the real
    // hasCompletedOnboarding value has loaded, flashing onboarding for returning users.
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val settings: StateFlow<SproutUserSettings> = repository.settingsFlow
        .onEach { _isReady.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SproutUserSettings())

    val history: StateFlow<List<TransformHistory.Snapshot>> = TransformHistory.history

    fun toggleOverlay(enabled: Boolean) = viewModelScope.launch { repository.setOverlayEnabled(enabled) }

    fun selectDefaultPreset(preset: TransformPreset) = viewModelScope.launch { repository.setDefaultPreset(preset) }

    fun setFavoritePresetOrder(orderedIds: List<String>) = viewModelScope.launch { repository.setFavoritePresetOrder(orderedIds) }

    fun toggleHaptics(enabled: Boolean) = viewModelScope.launch { repository.setHapticFeedback(enabled) }

    fun selectEngineMode(mode: EngineMode) = viewModelScope.launch { repository.setEngineMode(mode) }

    fun selectThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun selectLanguagePreference(preference: LanguagePreference) = viewModelScope.launch { repository.setLanguagePreference(preference) }

    fun saveTemperature(temperature: Float) = viewModelScope.launch { repository.setTemperature(temperature) }

    fun saveCustomPromptInstruction(instruction: String) = viewModelScope.launch { repository.setCustomPromptInstruction(instruction) }

    fun completeOnboarding() = viewModelScope.launch { repository.setHasCompletedOnboarding(true) }

    fun setPillAnchor(xFraction: Float, yFraction: Float) = viewModelScope.launch { repository.setPillAnchor(xFraction, yFraction) }

    fun saveOllamaUrl(url: String) = viewModelScope.launch { repository.setOllamaBaseUrl(url) }

    fun saveOllamaModel(model: String) = viewModelScope.launch { repository.setOllamaModel(model) }

    fun saveGeminiSettings(apiKey: String, model: String) = viewModelScope.launch { repository.setGeminiSettings(apiKey, model) }

    fun saveOpenAISettings(baseUrl: String, apiKey: String, model: String) = viewModelScope.launch { repository.setOpenAISettings(baseUrl, apiKey, model) }

    fun saveClaudeSettings(apiKey: String, model: String) = viewModelScope.launch { repository.setClaudeSettings(apiKey, model) }

    fun saveSnippet(key: String, expansion: String) = viewModelScope.launch { repository.saveSnippet(key, expansion) }

    fun deleteSnippet(key: String) = viewModelScope.launch { repository.deleteSnippet(key) }

    fun saveCustomCommand(trigger: String, prompt: String) = viewModelScope.launch { repository.saveCustomCommand(trigger, prompt) }

    fun deleteCustomCommand(trigger: String) = viewModelScope.launch { repository.deleteCustomCommand(trigger) }

    fun setAppRule(packageName: String, mode: AppRuleMode) = viewModelScope.launch { repository.setAppRule(packageName, mode) }

    fun clearHistory() = TransformHistory.clear()
}
