package com.stem.core.models

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stem.core.crypto.CryptoBox
import com.stem.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.IOException



val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stem_settings")

data class StemUserSettings(
    val serviceEnabled: Boolean = false,
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
    val claudeModel: String = "claude-sonnet-5",
    val snippets: Map<String, String> = defaultSnippets,
    val customCommands: Map<String, String> = defaultCustomCommands,
    val excludedPackages: Set<String> = defaultExcludedPackages
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

        // The service reads text from every app with no built-in exclusion, so it defaults to
        // staying out of well-known password managers. Banking apps are deliberately not
        // guessed here — package names vary too much by bank and region to enumerate reliably,
        // and a wrong guess would give false reassurance. Users add any app via Settings.
        val defaultExcludedPackages = setOf(
            "com.lastpass.lpandroid",
            "com.onepassword.android",
            "com.agilebits.onepassword", // legacy 1Password package, pre-rebrand
            "com.dashlane",
            "com.x8bit.bitwarden",
            "com.nordpass.android.app.password.manager",
            "com.callpod.android_apps.keeper",
            "com.google.android.apps.authenticator2"
        )
    }
}

/**
 * A persisted, browsable transform-history entry — deliberately its own type rather than reusing
 * [com.stem.engine.TransformHistory.Snapshot], which belongs to the engine layer (this file stays
 * dependency-free of it) and carries an undo-only [nodeHashCode] that has no meaning once an
 * entry is written here. This list is purely a display log: it survives the accessibility
 * service restarting (that Snapshot list is in-memory and clears on every restart) and doesn't
 * support popping.
 */
@Serializable
data class PersistedHistoryEntry(
    val id: String,
    val originalText: String,
    val replacedText: String,
    val presetName: String,
    val timestamp: Long
)

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        // Kotlin identifier renamed from the "overlay" era; the stored key string is left
        // unchanged ("overlay_enabled") so existing installs' stored preference still loads.
        val SERVICE_ENABLED = booleanPreferencesKey("overlay_enabled")
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
        val HISTORY_DATA = stringPreferencesKey("history_data")
        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")
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
            val serviceEnabled = preferences[PreferencesKeys.SERVICE_ENABLED] ?: false
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
            // Forward-migrate any Claude 2.x/3.x id (including dated snapshots like
            // claude-3-7-sonnet-latest) to the current default. Anything from Claude 4.x
            // onward is left untouched so this never rewrites a user's already-current
            // model choice back to something older.
            val claudeModel = if (storedClaudeModel.isNullOrBlank() || storedClaudeModel.startsWith("claude-2") || storedClaudeModel.startsWith("claude-3")) {
                "claude-sonnet-5"
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

            val excludedPackages = preferences[PreferencesKeys.EXCLUDED_PACKAGES] ?: StemUserSettings.defaultExcludedPackages

            StemUserSettings(
                serviceEnabled = serviceEnabled,
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
                customCommands = customCommands,
                excludedPackages = excludedPackages
            )
        }
        // DataStore can re-emit its Preferences object on writes that don't affect any field
        // this flow maps out (or on the same value being written again); every emission here
        // triggers a decrypt of 3 API keys plus, downstream, an accessibility-service settings
        // update and (for a subscribed screen) recomposition — skip the redundant ones.
        .distinctUntilChanged()

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVICE_ENABLED] = enabled
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

    /** Writes [apiKey] to [key] unless it would silently clobber a working stored key with a
     * broken or blank one: a blank incoming value never overwrites a non-blank stored value
     * (use [clearApiKey] to clear explicitly), and a Keystore-unavailable [CryptoBox.encrypt]
     * failure (null) leaves the stored value untouched rather than persisting garbage. */
    private fun androidx.datastore.preferences.core.MutablePreferences.writeApiKeyGuarded(
        key: Preferences.Key<String>,
        apiKey: String
    ) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            if (decryptStored(this[key]).isBlank()) this[key] = ""
            return
        }
        val encrypted = CryptoBox.encrypt(trimmed) ?: return
        this[key] = encrypted
    }

    suspend fun setGeminiSettings(apiKey: String, model: String) {
        context.dataStore.edit { preferences ->
            preferences.writeApiKeyGuarded(PreferencesKeys.GEMINI_API_KEY, apiKey)
            preferences[PreferencesKeys.GEMINI_MODEL] = model.trim().ifBlank { "gemini-3.7-flash" }
        }
    }

    suspend fun setOpenAISettings(baseUrl: String, apiKey: String, model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OPENAI_BASE_URL] = baseUrl.trim().ifBlank { "https://api.openai.com/v1" }
            preferences.writeApiKeyGuarded(PreferencesKeys.OPENAI_API_KEY, apiKey)
            preferences[PreferencesKeys.OPENAI_MODEL] = model.trim().ifBlank { "gpt-4o-mini" }
        }
    }

    suspend fun setClaudeSettings(apiKey: String, model: String) {
        context.dataStore.edit { preferences ->
            preferences.writeApiKeyGuarded(PreferencesKeys.CLAUDE_API_KEY, apiKey)
            preferences[PreferencesKeys.CLAUDE_MODEL] = model.trim().ifBlank { "claude-sonnet-5" }
        }
    }

    suspend fun clearGeminiApiKey() {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.GEMINI_API_KEY] = "" }
    }

    suspend fun clearOpenAIApiKey() {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.OPENAI_API_KEY] = "" }
    }

    suspend fun clearClaudeApiKey() {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CLAUDE_API_KEY] = "" }
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

    val historyFlow: Flow<List<PersistedHistoryEntry>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> deserializeHistory(preferences[PreferencesKeys.HISTORY_DATA]) }
        .distinctUntilChanged()

    suspend fun addHistoryEntry(entry: PersistedHistoryEntry) {
        context.dataStore.edit { preferences ->
            val current = deserializeHistory(preferences[PreferencesKeys.HISTORY_DATA])
            val updated = (current + entry).takeLast(MAX_PERSISTED_HISTORY)
            preferences[PreferencesKeys.HISTORY_DATA] = historyJson.encodeToString(historyListSerializer, updated)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HISTORY_DATA] = ""
        }
    }

    suspend fun setPackageExcluded(packageName: String, excluded: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.EXCLUDED_PACKAGES] ?: StemUserSettings.defaultExcludedPackages
            preferences[PreferencesKeys.EXCLUDED_PACKAGES] = if (excluded) {
                current + packageName
            } else {
                current - packageName
            }
        }
    }

    private fun deserializeHistory(raw: String?): List<PersistedHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            historyJson.decodeFromString(historyListSerializer, raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun migrateLegacyPlaintextKeysIfNeeded() {
        context.dataStore.edit { preferences ->
            for (key in listOf(PreferencesKeys.GEMINI_API_KEY, PreferencesKeys.OPENAI_API_KEY, PreferencesKeys.CLAUDE_API_KEY)) {
                val stored = preferences[key]
                if (!stored.isNullOrBlank() && !CryptoBox.isEncrypted(stored)) {
                    CryptoBox.encrypt(stored)?.let { preferences[key] = it }
                }
            }
        }
    }

    private fun decryptStored(stored: String?): String {
        if (stored.isNullOrBlank()) return ""
        return CryptoBox.decrypt(stored) ?: ""
    }

    private val pairsMapSerializer = MapSerializer(String.serializer(), String.serializer())
    private val pairsJson = Json { ignoreUnknownKeys = true }

    private val historyListSerializer = ListSerializer(PersistedHistoryEntry.serializer())
    private val historyJson = Json { ignoreUnknownKeys = true }

    private companion object {
        const val MAX_PERSISTED_HISTORY = 50
    }

    private fun serializePairs(map: Map<String, String>): String {
        return pairsJson.encodeToString(pairsMapSerializer, map)
    }

    /** Reads JSON written by [serializePairs]. Also accepts the legacy pre-JSON
     * "key:::value|||key2:::value2" format so data saved before this migration keeps loading —
     * every write goes through [serializePairs] from here on, so a legacy entry self-heals into
     * unambiguous JSON storage the next time it's saved. */
    private fun deserializePairs(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        if (raw.trimStart().startsWith("{")) {
            return try {
                pairsJson.decodeFromString(pairsMapSerializer, raw)
            } catch (_: Exception) {
                emptyMap()
            }
        }
        val result = mutableMapOf<String, String>()
        for (pair in raw.split("|||")) {
            val parts = pair.split(":::", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) {
                result[parts[0]] = parts[1]
            }
        }
        return result
    }
}
