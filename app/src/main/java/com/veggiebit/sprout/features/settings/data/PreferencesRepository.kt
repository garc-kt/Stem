package com.veggiebit.sprout.features.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veggiebit.sprout.app.theme.ThemeMode
import com.veggiebit.sprout.core.crypto.CryptoBox
import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.enhancement.data.models.LanguagePreference
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sprout_settings")

data class SproutUserSettings(
    val overlayEnabled: Boolean = false, // Unobtrusive by default like SwiftSlate
    val defaultPreset: TransformPreset = TransformPreset.FIX,
    val favoritePresetIds: List<String> = TransformPreset.defaultOrder.map { it.id },
    val hapticFeedbackEnabled: Boolean = true,
    val engineMode: EngineMode = EngineMode.LOCAL_RULES,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languagePreference: LanguagePreference = LanguagePreference.AUTO,
    val temperature: Float = 0.3f, // 0.0 (Precise) to 1.0 (Creative)
    val customPromptInstruction: String = "",
    val hasCompletedOnboarding: Boolean = false,
    // -1f means "not yet dragged" — the overlay falls back to cursor-anchored placement.
    val pillAnchorXFraction: Float = -1f,
    val pillAnchorYFraction: Float = -1f,
    // Ollama LAN
    val ollamaBaseUrl: String = "http://10.0.2.2:11434",
    val ollamaModel: String = "llama3.3",
    // Gemini
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.0-flash",
    // OpenAI / Compatible
    val openaiBaseUrl: String = "https://api.openai.com/v1",
    val openaiApiKey: String = "",
    val openaiModel: String = "gpt-4o-mini",
    // Claude
    val claudeApiKey: String = "",
    val claudeModel: String = "claude-3-7-sonnet-20250219",
    val appRules: Map<String, AppRuleMode> = emptyMap(),
    val snippets: Map<String, String> = defaultSnippets
) {
    /** Derived from [appRules] so the accessibility service's existing per-package hide check
     * keeps working unmodified — packages set to NEVER behave exactly like the old blacklist. */
    val blacklistedPackages: Set<String>
        get() = appRules.filterValues { it == AppRuleMode.NEVER }.keys

    val orderedPresets: List<TransformPreset>
        get() {
            val known = favoritePresetIds.mapNotNull { id -> TransformPreset.entries.firstOrNull { it.id == id } }
            val missing = TransformPreset.entries.filter { it !in known }
            return known + missing
        }

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
        val FAVORITE_PRESETS = stringPreferencesKey("favorite_presets")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val ENGINE_MODE = stringPreferencesKey("engine_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_PREFERENCE = stringPreferencesKey("language_preference")
        val TEMPERATURE = floatPreferencesKey("ai_temperature")
        val CUSTOM_PROMPT_INSTRUCTION = stringPreferencesKey("custom_prompt_instruction")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val PILL_ANCHOR_X = floatPreferencesKey("pill_anchor_x")
        val PILL_ANCHOR_Y = floatPreferencesKey("pill_anchor_y")
        val OLLAMA_BASE_URL = stringPreferencesKey("ollama_base_url")
        val OLLAMA_MODEL = stringPreferencesKey("ollama_model")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val CLAUDE_API_KEY = stringPreferencesKey("claude_api_key")
        val CLAUDE_MODEL = stringPreferencesKey("claude_model")
        val BLACKLISTED_PACKAGES = stringPreferencesKey("blacklisted_packages") // legacy, migrated
        val APP_RULES_DATA = stringPreferencesKey("app_rules_data")
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
            val themeModeId = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.id
            val languagePreferenceId = preferences[PreferencesKeys.LANGUAGE_PREFERENCE] ?: LanguagePreference.AUTO.id
            val temperature = preferences[PreferencesKeys.TEMPERATURE] ?: 0.3f
            val customPromptInstruction = preferences[PreferencesKeys.CUSTOM_PROMPT_INSTRUCTION] ?: ""
            val hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
            val pillAnchorX = preferences[PreferencesKeys.PILL_ANCHOR_X] ?: -1f
            val pillAnchorY = preferences[PreferencesKeys.PILL_ANCHOR_Y] ?: -1f
            val ollamaBaseUrl = preferences[PreferencesKeys.OLLAMA_BASE_URL] ?: "http://10.0.2.2:11434"
            val ollamaModel = preferences[PreferencesKeys.OLLAMA_MODEL] ?: "llama3.3"
            val geminiApiKey = decryptStored(preferences[PreferencesKeys.GEMINI_API_KEY])
            val geminiModel = preferences[PreferencesKeys.GEMINI_MODEL] ?: "gemini-2.0-flash"
            val openaiBaseUrl = preferences[PreferencesKeys.OPENAI_BASE_URL] ?: "https://api.openai.com/v1"
            val openaiApiKey = decryptStored(preferences[PreferencesKeys.OPENAI_API_KEY])
            val openaiModel = preferences[PreferencesKeys.OPENAI_MODEL] ?: "gpt-4o-mini"
            val claudeApiKey = decryptStored(preferences[PreferencesKeys.CLAUDE_API_KEY])

            // One-time migration: any retired or invalid placeholder is mapped forward to the current default
            val storedClaudeModel = preferences[PreferencesKeys.CLAUDE_MODEL]
            val claudeModel = if (storedClaudeModel.isNullOrBlank() || storedClaudeModel == "claude-haiku-4-5" || storedClaudeModel.startsWith("claude-2")) {
                "claude-3-7-sonnet-20250219"
            } else {
                storedClaudeModel
            }

            val favoritePresetIds = preferences[PreferencesKeys.FAVORITE_PRESETS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?: TransformPreset.defaultOrder.map { it.id }

            val appRules = decodeAppRules(preferences[PreferencesKeys.APP_RULES_DATA])
                .ifEmpty {
                    // Legacy migration: fold the old blacklist set into the new per-app rules
                    // map as NEVER entries. Only applies once — after the first write to
                    // APP_RULES_DATA, that key takes over and this branch is never hit again.
                    preferences[PreferencesKeys.BLACKLISTED_PACKAGES]
                        ?.split(",")
                        ?.filter { it.isNotBlank() }
                        ?.associateWith { AppRuleMode.NEVER }
                        ?: emptyMap()
                }

            val rawSnippets = preferences[PreferencesKeys.SNIPPETS_DATA]
            val snippets = if (rawSnippets.isNullOrBlank()) {
                SproutUserSettings.defaultSnippets
            } else {
                deserializePairs(rawSnippets)
            }

            SproutUserSettings(
                overlayEnabled = overlayEnabled,
                defaultPreset = TransformPreset.fromId(defaultPresetId),
                favoritePresetIds = favoritePresetIds,
                hapticFeedbackEnabled = hapticFeedback,
                engineMode = EngineMode.fromId(engineModeId),
                themeMode = ThemeMode.fromId(themeModeId),
                languagePreference = LanguagePreference.fromId(languagePreferenceId),
                temperature = temperature,
                customPromptInstruction = customPromptInstruction,
                hasCompletedOnboarding = hasCompletedOnboarding,
                pillAnchorXFraction = pillAnchorX,
                pillAnchorYFraction = pillAnchorY,
                ollamaBaseUrl = ollamaBaseUrl,
                ollamaModel = ollamaModel,
                geminiApiKey = geminiApiKey,
                geminiModel = geminiModel,
                openaiBaseUrl = openaiBaseUrl,
                openaiApiKey = openaiApiKey,
                openaiModel = openaiModel,
                claudeApiKey = claudeApiKey,
                claudeModel = claudeModel,
                appRules = appRules,
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

    suspend fun setFavoritePresetOrder(orderedIds: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITE_PRESETS] = orderedIds.joinToString(",")
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

    suspend fun setLanguagePreference(preference: LanguagePreference) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE_PREFERENCE] = preference.id
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

    suspend fun setPillAnchor(xFraction: Float, yFraction: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PILL_ANCHOR_X] = xFraction
            preferences[PreferencesKeys.PILL_ANCHOR_Y] = yFraction
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
            preferences[PreferencesKeys.GEMINI_MODEL] = model.trim().ifBlank { "gemini-2.0-flash" }
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
            preferences[PreferencesKeys.CLAUDE_MODEL] = model.trim().ifBlank { "claude-3-7-sonnet-20250219" }
        }
    }

    suspend fun setAppRule(packageName: String, mode: AppRuleMode) {
        context.dataStore.edit { preferences ->
            val current = decodeAppRules(preferences[PreferencesKeys.APP_RULES_DATA]).toMutableMap()
            if (mode == AppRuleMode.AUTO) {
                current.remove(packageName)
            } else {
                current[packageName] = mode
            }
            preferences[PreferencesKeys.APP_RULES_DATA] = encodeAppRules(current)
        }
    }

    suspend fun setBlacklistedPackages(packages: Set<String>) {
        // Retained for callers still working with the legacy flat blacklist shape; folds
        // straight into the new per-app rules map as NEVER entries.
        context.dataStore.edit { preferences ->
            val current = decodeAppRules(preferences[PreferencesKeys.APP_RULES_DATA])
                .filterValues { it != AppRuleMode.NEVER }
                .toMutableMap()
            packages.forEach { current[it] = AppRuleMode.NEVER }
            preferences[PreferencesKeys.APP_RULES_DATA] = encodeAppRules(current)
        }
    }

    suspend fun saveSnippet(key: String, expansion: String) {
        context.dataStore.edit { preferences ->
            val rawSnippets = preferences[PreferencesKeys.SNIPPETS_DATA]
            val current = if (rawSnippets.isNullOrBlank()) {
                SproutUserSettings.defaultSnippets.toMutableMap()
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
            val current = if (rawSnippets.isNullOrBlank()) {
                SproutUserSettings.defaultSnippets.toMutableMap()
            } else {
                deserializePairs(rawSnippets).toMutableMap()
            }
            current.remove(key.trim().removePrefix("..").removePrefix("."))
            preferences[PreferencesKeys.SNIPPETS_DATA] = serializePairs(current)
        }
    }

    /**
     * One-shot migration for API keys stored in plaintext by versions prior to Keystore
     * encryption. Safe to call on every app start — each key is only rewritten if it isn't
     * already in the `ENC1:` format, so this becomes a no-op after the first run.
     */
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

    private fun encodeAppRules(map: Map<String, AppRuleMode>): String {
        return map.entries.joinToString("|||") { "${it.key}:::${it.value.id}" }
    }

    private fun decodeAppRules(raw: String?): Map<String, AppRuleMode> {
        if (raw.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<String, AppRuleMode>()
        for (pair in raw.split("|||")) {
            val parts = pair.split(":::")
            if (parts.size == 2) {
                result[parts[0]] = AppRuleMode.fromId(parts[1])
            }
        }
        return result
    }

    private fun serializePairs(map: Map<String, String>): String {
        return map.entries.joinToString("|||") { "${it.key}:::${it.value}" }
    }

    private fun deserializePairs(raw: String): Map<String, String> {
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
