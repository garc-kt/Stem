package com.stem.core.models

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stem.core.crypto.CryptoBox
import com.stem.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException



val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stem_settings")

data class StemUserSettings(
    val overlayEnabled: Boolean = false,
    val defaultPreset: TransformPreset = TransformPreset.FIX,
    val hapticFeedbackEnabled: Boolean = true,
    val engineMode: EngineMode = EngineMode.LOCAL_RULES,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languagePreference: LanguagePreference = LanguagePreference.AUTO,
    val temperature: Float = 0.3f,
    val customPromptInstruction: String = "",
    val hasCompletedOnboarding: Boolean = false,
    val ollamaBaseUrl: String = "http://10.0.2.2:11434",
    val ollamaModel: String = "llama3.3",
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.7-flash",
    val openaiBaseUrl: String = "https://api.openai.com/v1",
    val openaiApiKey: String = "",
    val openaiModel: String = "gpt-4o-mini",
    val claudeApiKey: String = "",
    val claudeModel: String = "claude-3-7-sonnet-latest",
    val snippets: Map<String, String> = defaultSnippets,
    val customCommands: Map<String, String> = defaultCustomCommands
) {
    companion object {
        val defaultSnippets = mapOf(
            "email" to "user@example.com",
            "shrug" to "¯\\_(ツ)_/¯",
            "lenny" to "( ͡° ͜ʖ ͡°)",
            "brb" to "Be right back!"
        )

        val defaultCustomCommands = mapOf(
            "roast" to "Rewrite this in a witty, humorous roast tone while keeping it funny.",
            "translate" to "Translate this text into fluent, natural Spanish.",
            "reply" to "Draft a polite, helpful, and articulate reply to this message.",
            "poetic" to "Rewrite this in a beautiful, poetic, and eloquent style.",
            "tldr" to "Give a single punchy TL;DR takeaway sentence for this text."
        )
    }
}

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val DEFAULT_PRESET = stringPreferencesKey("default_preset")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val ENGINE_MODE = stringPreferencesKey("engine_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_PREFERENCE = stringPreferencesKey("language_preference")
        val TEMPERATURE = floatPreferencesKey("ai_temperature")
        val CUSTOM_PROMPT_INSTRUCTION = stringPreferencesKey("custom_prompt_instruction")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val OLLAMA_BASE_URL = stringPreferencesKey("ollama_base_url")
        val OLLAMA_MODEL = stringPreferencesKey("ollama_model")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val CLAUDE_API_KEY = stringPreferencesKey("claude_api_key")
        val CLAUDE_MODEL = stringPreferencesKey("claude_model")
        val SNIPPETS_DATA = stringPreferencesKey("snippets_data")
        val CUSTOM_COMMANDS_DATA = stringPreferencesKey("custom_commands_data")
    }

    val settingsFlow: Flow<StemUserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val overlayEnabled = preferences[PreferencesKeys.OVERLAY_ENABLED] ?: false
            val defaultPresetId = preferences[PreferencesKeys.DEFAULT_PRESET] ?: TransformPreset.FIX.id
            val hapticFeedback = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true
            val engineModeId = preferences[PreferencesKeys.ENGINE_MODE] ?: EngineMode.LOCAL_RULES.id
            val themeModeId = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.id
            val languagePreferenceId = preferences[PreferencesKeys.LANGUAGE_PREFERENCE] ?: LanguagePreference.AUTO.id
            val temperature = preferences[PreferencesKeys.TEMPERATURE] ?: 0.3f
            val customPromptInstruction = preferences[PreferencesKeys.CUSTOM_PROMPT_INSTRUCTION] ?: ""
            val hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
            val ollamaBaseUrl = preferences[PreferencesKeys.OLLAMA_BASE_URL] ?: "http://10.0.2.2:11434"
            val ollamaModel = preferences[PreferencesKeys.OLLAMA_MODEL] ?: "llama3.3"
            val geminiApiKey = decryptStored(preferences[PreferencesKeys.GEMINI_API_KEY])
            val storedGeminiModel = preferences[PreferencesKeys.GEMINI_MODEL]
            val geminiModel = if (storedGeminiModel.isNullOrBlank() || storedGeminiModel.startsWith("gemini-1.") || storedGeminiModel.startsWith("gemini-1.0") || storedGeminiModel.startsWith("gemini-1.5")) {
                "gemini-3.7-flash"
            } else {
                storedGeminiModel
            }

            val openaiBaseUrl = preferences[PreferencesKeys.OPENAI_BASE_URL] ?: "https://api.openai.com/v1"
            val openaiApiKey = decryptStored(preferences[PreferencesKeys.OPENAI_API_KEY])
            val storedOpenAIModel = preferences[PreferencesKeys.OPENAI_MODEL]
            val openaiModel = if (storedOpenAIModel.isNullOrBlank() || storedOpenAIModel.startsWith("gpt-3.5") || storedOpenAIModel == "text-davinci-003" || storedOpenAIModel == "gpt-5-mini") {
                "gpt-4o-mini"
            } else {
                storedOpenAIModel
            }

            val claudeApiKey = decryptStored(preferences[PreferencesKeys.CLAUDE_API_KEY])
            val storedClaudeModel = preferences[PreferencesKeys.CLAUDE_MODEL]
            val claudeModel = if (storedClaudeModel.isNullOrBlank() || storedClaudeModel.startsWith("claude-2") || storedClaudeModel.startsWith("claude-3-opus") || storedClaudeModel == "claude-4.5-sonnet" || storedClaudeModel == "claude-3-haiku-20240307" || storedClaudeModel == "claude-3-sonnet-20240229") {
                "claude-3-7-sonnet-latest"
            } else {
                storedClaudeModel
            }

            val rawSnippets = preferences[PreferencesKeys.SNIPPETS_DATA]
            val snippets = if (rawSnippets == null) {
                StemUserSettings.defaultSnippets
            } else {
                deserializePairs(rawSnippets)
            }

            val rawCustomCommands = preferences[PreferencesKeys.CUSTOM_COMMANDS_DATA]
            val customCommands = if (rawCustomCommands == null) {
                StemUserSettings.defaultCustomCommands
            } else {
                deserializePairs(rawCustomCommands)
            }

            StemUserSettings(
                overlayEnabled = overlayEnabled,
                defaultPreset = TransformPreset.fromId(defaultPresetId),
                hapticFeedbackEnabled = hapticFeedback,
                engineMode = EngineMode.fromId(engineModeId),
                themeMode = ThemeMode.fromId(themeModeId),
                languagePreference = LanguagePreference.fromId(languagePreferenceId),
                temperature = temperature,
                customPromptInstruction = customPromptInstruction,
                hasCompletedOnboarding = hasCompletedOnboarding,
                ollamaBaseUrl = ollamaBaseUrl,
                ollamaModel = ollamaModel,
                geminiApiKey = geminiApiKey,
                geminiModel = geminiModel,
                openaiBaseUrl = openaiBaseUrl,
                openaiApiKey = openaiApiKey,
                openaiModel = openaiModel,
                claudeApiKey = claudeApiKey,
                claudeModel = claudeModel,
                snippets = snippets,
                customCommands = customCommands
            )
        }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun setEngineMode(mode: EngineMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENGINE_MODE] = mode.id
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.id
        }
    }

    suspend fun setTemperature(temperature: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEMPERATURE] = temperature.coerceIn(0.0f, 1.0f)
        }
    }

    suspend fun setCustomPromptInstruction(instruction: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_PROMPT_INSTRUCTION] = instruction.trim()
        }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    suspend fun setOllamaBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OLLAMA_BASE_URL] = url
        }
    }

    suspend fun setOllamaModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OLLAMA_MODEL] = model
        }
    }

    suspend fun setGeminiSettings(apiKey: String, model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = CryptoBox.encrypt(apiKey.trim())
            preferences[PreferencesKeys.GEMINI_MODEL] = model.trim().ifBlank { "gemini-3.7-flash" }
        }
    }

    suspend fun setOpenAISettings(baseUrl: String, apiKey: String, model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPENAI_BASE_URL] = baseUrl.trim().ifBlank { "https://api.openai.com/v1" }
            preferences[PreferencesKeys.OPENAI_API_KEY] = CryptoBox.encrypt(apiKey.trim())
            preferences[PreferencesKeys.OPENAI_MODEL] = model.trim().ifBlank { "gpt-4o-mini" }
        }
    }

    suspend fun setClaudeSettings(apiKey: String, model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLAUDE_API_KEY] = CryptoBox.encrypt(apiKey.trim())
            preferences[PreferencesKeys.CLAUDE_MODEL] = model.trim().ifBlank { "claude-3-7-sonnet-latest" }
        }
    }

    suspend fun saveSnippet(key: String, expansion: String) {
        context.dataStore.edit { preferences ->
            val rawSnippets = preferences[PreferencesKeys.SNIPPETS_DATA]
            val current = if (rawSnippets == null) {
                StemUserSettings.defaultSnippets.toMutableMap()
            } else {
                deserializePairs(rawSnippets).toMutableMap()
            }
            current[key.trim().removePrefix("..").removePrefix(".")] = expansion.trim()
            preferences[PreferencesKeys.SNIPPETS_DATA] = serializePairs(current)
        }
    }

    suspend fun deleteSnippet(key: String) {
        context.dataStore.edit { preferences ->
            val rawSnippets = preferences[PreferencesKeys.SNIPPETS_DATA]
            val current = if (rawSnippets == null) {
                StemUserSettings.defaultSnippets.toMutableMap()
            } else {
                deserializePairs(rawSnippets).toMutableMap()
            }
            current.remove(key.trim().removePrefix("..").removePrefix("."))
            preferences[PreferencesKeys.SNIPPETS_DATA] = serializePairs(current)
        }
    }

    suspend fun saveCustomCommand(trigger: String, prompt: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.CUSTOM_COMMANDS_DATA]
            val current = if (raw == null) {
                StemUserSettings.defaultCustomCommands.toMutableMap()
            } else {
                deserializePairs(raw).toMutableMap()
            }
            val cleanKey = trigger.trim().removePrefix("?").removePrefix("..").removePrefix(".")
            current[cleanKey] = prompt.trim()
            preferences[PreferencesKeys.CUSTOM_COMMANDS_DATA] = serializePairs(current)
        }
    }

    suspend fun deleteCustomCommand(trigger: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.CUSTOM_COMMANDS_DATA]
            val current = if (raw == null) {
                StemUserSettings.defaultCustomCommands.toMutableMap()
            } else {
                deserializePairs(raw).toMutableMap()
            }
            val cleanKey = trigger.trim().removePrefix("?").removePrefix("..").removePrefix(".")
            current.remove(cleanKey)
            preferences[PreferencesKeys.CUSTOM_COMMANDS_DATA] = serializePairs(current)
        }
    }

    suspend fun migrateLegacyPlaintextKeysIfNeeded() {
        context.dataStore.edit { preferences ->
            for (key in listOf(PreferencesKeys.GEMINI_API_KEY, PreferencesKeys.OPENAI_API_KEY, PreferencesKeys.CLAUDE_API_KEY)) {
                val stored = preferences[key]
                if (!stored.isNullOrBlank() && !CryptoBox.isEncrypted(stored)) {
                    preferences[key] = CryptoBox.encrypt(stored)
                }
            }
        }
    }

    private fun decryptStored(stored: String?): String {
        if (stored.isNullOrBlank()) return ""
        return CryptoBox.decrypt(stored) ?: ""
    }

    private fun serializePairs(map: Map<String, String>): String {
        return map.entries.joinToString("|||") { "${it.key}:::${it.value}" }
    }

    private fun deserializePairs(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val pairs = raw.split("|||")
        for (pair in pairs) {
            val parts = pair.split(":::")
            if (parts.size == 2 && parts[0].isNotBlank()) {
                result[parts[0]] = parts[1]
            }
        }
        return result
    }
}
