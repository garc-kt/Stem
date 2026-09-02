package com.stem.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stem.app.StemApplication
import com.stem.ui.theme.ThemeMode
import com.stem.engine.TransformHistory
import com.stem.core.models.EngineMode
import com.stem.core.models.PreferencesRepository
import com.stem.core.models.StemUserSettings
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
 * re-triggered transform) per character typed. Text fields now hold local Compose
 * state and call the save* methods here only on an explicit action (blur/button), see
 * EngineScreen.
 */
class SettingsViewModel : ViewModel() {

    private val repository: PreferencesRepository = StemApplication.instance.preferencesRepository

    // DataStore reads are near-instant but not synchronous — without this, the nav host would
    // have to guess a start destination (Onboarding vs Home) before the real
    // hasCompletedOnboarding value has loaded, flashing onboarding for returning users.
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val settings: StateFlow<StemUserSettings> = repository.settingsFlow
        .onEach { _isReady.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StemUserSettings())

    val history: StateFlow<List<TransformHistory.Snapshot>> = TransformHistory.history

    fun toggleOverlay(enabled: Boolean) = viewModelScope.launch { repository.setOverlayEnabled(enabled) }

    fun toggleHaptics(enabled: Boolean) = viewModelScope.launch { repository.setHapticFeedback(enabled) }

    fun selectEngineMode(mode: EngineMode) = viewModelScope.launch { repository.setEngineMode(mode) }

    fun selectThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun saveTemperature(temperature: Float) = viewModelScope.launch { repository.setTemperature(temperature) }

    fun saveCustomPromptInstruction(instruction: String) = viewModelScope.launch { repository.setCustomPromptInstruction(instruction) }

    fun completeOnboarding() = viewModelScope.launch { repository.setHasCompletedOnboarding(true) }

    fun saveOllamaUrl(url: String) = viewModelScope.launch { repository.setOllamaBaseUrl(url) }

    fun saveOllamaModel(model: String) = viewModelScope.launch { repository.setOllamaModel(model) }

    fun saveGeminiSettings(apiKey: String, model: String) = viewModelScope.launch { repository.setGeminiSettings(apiKey, model) }

    fun saveOpenAISettings(baseUrl: String, apiKey: String, model: String) = viewModelScope.launch { repository.setOpenAISettings(baseUrl, apiKey, model) }

    fun saveClaudeSettings(apiKey: String, model: String) = viewModelScope.launch { repository.setClaudeSettings(apiKey, model) }

    fun clearGeminiApiKey() = viewModelScope.launch { repository.clearGeminiApiKey() }

    fun clearOpenAIApiKey() = viewModelScope.launch { repository.clearOpenAIApiKey() }

    fun clearClaudeApiKey() = viewModelScope.launch { repository.clearClaudeApiKey() }

    fun saveSnippet(key: String, expansion: String) = viewModelScope.launch { repository.saveSnippet(key, expansion) }

    fun deleteSnippet(key: String) = viewModelScope.launch { repository.deleteSnippet(key) }

    fun saveCustomCommand(trigger: String, prompt: String) = viewModelScope.launch { repository.saveCustomCommand(trigger, prompt) }

    fun deleteCustomCommand(trigger: String) = viewModelScope.launch { repository.deleteCustomCommand(trigger) }

    fun clearHistory() = TransformHistory.clear()
}

