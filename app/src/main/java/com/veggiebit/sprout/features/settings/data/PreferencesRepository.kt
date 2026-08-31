package com.veggiebit.sprout.features.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sprout_settings")

data class SproutUserSettings(
    val overlayEnabled: Boolean = false, // Unobtrusive by default like SwiftSlate
    val defaultPreset: TransformPreset = TransformPreset.FIX,
    val hapticFeedbackEnabled: Boolean = true,
    val engineMode: EngineMode = EngineMode.LOCAL_RULES,
    // Ollama LAN
    val ollamaBaseUrl: String = "http://10.0.2.2:11434",
    val ollamaModel: String = "llama3.2",
    // Gemini
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-1.5-flash",
    // OpenAI / Compatible
    val openaiBaseUrl: String = "https://api.openai.com/v1",
    val openaiApiKey: String = "",
    val openaiModel: String = "gpt-4o-mini",
    // Claude
    val claudeApiKey: String = "",
    val claudeModel: String = "claude-3-5-haiku-20241022",
    val blacklistedPackages: Set<String> = emptySet(),
    val snippets: Map<String, String> = defaultSnippets
) {
    companion object {
        val defaultSnippets = mapOf(
            "email" to "user@example.com",
            "shrug" to "¯\\_(ツ)_/¯",
            "lenny" to "( ͡° ͜ʖ ͡°)",
            "brb" to "Be right back!"
        )
    }
}

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val DEFAULT_PRESET = stringPreferencesKey("default_preset")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val ENGINE_MODE = stringPreferencesKey("engine_mode")
        val OLLAMA_BASE_URL = stringPreferencesKey("ollama_base_url")
        val OLLAMA_MODEL = stringPreferencesKey("ollama_model")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val CLAUDE_API_KEY = stringPreferencesKey("claude_api_key")
        val CLAUDE_MODEL = stringPreferencesKey("claude_model")
        val BLACKLISTED_PACKAGES = stringPreferencesKey("blacklisted_packages")
        val SNIPPETS_DATA = stringPreferencesKey("snippets_data")
    }

    val settingsFlow: Flow<SproutUserSettings> = context.dataStore.data
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
            val ollamaBaseUrl = preferences[PreferencesKeys.OLLAMA_BASE_URL] ?: "http://10.0.2.2:11434"
            val ollamaModel = preferences[PreferencesKeys.OLLAMA_MODEL] ?: "llama3.2"
            val geminiApiKey = preferences[PreferencesKeys.GEMINI_API_KEY] ?: ""
            val geminiModel = preferences[PreferencesKeys.GEMINI_MODEL] ?: "gemini-1.5-flash"
            val openaiBaseUrl = preferences[PreferencesKeys.OPENAI_BASE_URL] ?: "https://api.openai.com/v1"
            val openaiApiKey = preferences[PreferencesKeys.OPENAI_API_KEY] ?: ""
            val openaiModel = preferences[PreferencesKeys.OPENAI_MODEL] ?: "gpt-4o-mini"
            val claudeApiKey = preferences[PreferencesKeys.CLAUDE_API_KEY] ?: ""
            val claudeModel = preferences[PreferencesKeys.CLAUDE_MODEL] ?: "claude-3-5-haiku-20241022"

            val blacklisted = preferences[PreferencesKeys.BLACKLISTED_PACKAGES]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet() ?: emptySet()

            val rawSnippets = preferences[PreferencesKeys.SNIPPETS_DATA]
            val snippets = if (rawSnippets.isNullOrBlank()) {
                SproutUserSettings.defaultSnippets
            } else {
                deserializeSnippets(rawSnippets)
            }

            SproutUserSettings(
                overlayEnabled = overlayEnabled,
                defaultPreset = TransformPreset.fromId(defaultPresetId),
                hapticFeedbackEnabled = hapticFeedback,
                engineMode = EngineMode.fromId(engineModeId),
                ollamaBaseUrl = ollamaBaseUrl,
                ollamaModel = ollamaModel,
                geminiApiKey = geminiApiKey,
                geminiModel = geminiModel,
                openaiBaseUrl = openaiBaseUrl,
                openaiApiKey = openaiApiKey,
                openaiModel = openaiModel,
                claudeApiKey = claudeApiKey,
                claudeModel = claudeModel,
                blacklistedPackages = blacklisted,
                snippets = snippets
            )
        }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setDefaultPreset(preset: TransformPreset) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PRESET] = preset.id
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
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey.trim()
            preferences[PreferencesKeys.GEMINI_MODEL] = model.trim().ifBlank { "gemini-1.5-flash" }
        }
    }

    suspend fun setOpenAISettings(baseUrl: String, apiKey: String, model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPENAI_BASE_URL] = baseUrl.trim().ifBlank { "https://api.openai.com/v1" }
            preferences[PreferencesKeys.OPENAI_API_KEY] = apiKey.trim()
            preferences[PreferencesKeys.OPENAI_MODEL] = model.trim().ifBlank { "gpt-4o-mini" }
        }
    }

    suspend fun setClaudeSettings(apiKey: String, model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLAUDE_API_KEY] = apiKey.trim()
            preferences[PreferencesKeys.CLAUDE_MODEL] = model.trim().ifBlank { "claude-3-5-haiku-20241022" }
        }
    }

    suspend fun setBlacklistedPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLACKLISTED_PACKAGES] = packages.joinToString(",")
        }
    }

    suspend fun saveSnippet(key: String, expansion: String) {
        context.dataStore.edit { preferences ->
            val rawSnippets = preferences[PreferencesKeys.SNIPPETS_DATA]
            val current = if (rawSnippets.isNullOrBlank()) {
                SproutUserSettings.defaultSnippets.toMutableMap()
            } else {
                deserializeSnippets(rawSnippets).toMutableMap()
            }
            current[key.trim().removePrefix("..").removePrefix(".")] = expansion.trim()
            preferences[PreferencesKeys.SNIPPETS_DATA] = serializeSnippets(current)
        }
    }

    suspend fun deleteSnippet(key: String) {
        context.dataStore.edit { preferences ->
            val rawSnippets = preferences[PreferencesKeys.SNIPPETS_DATA]
            val current = if (rawSnippets.isNullOrBlank()) {
                SproutUserSettings.defaultSnippets.toMutableMap()
            } else {
                deserializeSnippets(rawSnippets).toMutableMap()
            }
            current.remove(key.trim().removePrefix("..").removePrefix("."))
            preferences[PreferencesKeys.SNIPPETS_DATA] = serializeSnippets(current)
        }
    }

    private fun serializeSnippets(map: Map<String, String>): String {
        return map.entries.joinToString("|||") { "${it.key}:::${it.value}" }
    }

    private fun deserializeSnippets(raw: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pairs = raw.split("|||")
        for (pair in pairs) {
            val parts = pair.split(":::")
            if (parts.size == 2) {
                result[parts[0]] = parts[1]
            }
        }
        return result
    }
}
