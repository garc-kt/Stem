package com.stem.ui.screens

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stem.R
import com.stem.core.models.EngineMode
import com.stem.core.models.LanguagePreference
import com.stem.core.models.StemUserSettings
import com.stem.core.models.TransformPreset
import com.stem.core.util.AppVersion
import com.stem.core.util.InstalledAppsHelper
import com.stem.engine.ClaudeClient
import com.stem.engine.GeminiClient
import com.stem.engine.OllamaClient
import com.stem.engine.OpenAIClient
import com.stem.ui.components.PresetChipsRow
import com.stem.ui.components.StemButton
import com.stem.ui.components.StemCard
import com.stem.ui.components.StemSectionHeader
import com.stem.ui.components.StemSegmentedGroup
import com.stem.ui.theme.GitHubSponsorHeartIcon
import com.stem.ui.theme.KoFiIcon
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemCardShape
import com.stem.ui.theme.StemLogoMark
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemPillShape
import com.stem.ui.theme.StemSharpShape
import com.stem.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/**
 * Focus-loss saver helper.
 */
@Composable
private fun rememberFocusLossSaver(onSave: () -> Unit): (FocusState) -> Unit {
    var wasFocused by remember { mutableStateOf(false) }
    return { focusState ->
        if (wasFocused && !focusState.isFocused) {
            onSave()
        }
        wasFocused = focusState.isFocused
    }
}

/**
 * Minimalist password-style API key field.
 */
@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.engine_api_key_label), style = MaterialTheme.typography.bodySmall) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave(); focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged(rememberFocusLossSaver(onSave)),
            singleLine = true,
            shape = StemSharpShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = stemTheme.bg,
                unfocusedContainerColor = stemTheme.bg,
                focusedBorderColor = stemTheme.ink,
                unfocusedBorderColor = stemTheme.borderSubtle,
                cursorColor = stemTheme.ink
            )
        )
        if (value.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.engine_clear_key_button),
                style = StemMonoBadge,
                color = stemTheme.remove,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onClear)
                    .padding(vertical = 2.dp)
            )
        }
    }
}

/**
 * Stem Settings / Engine Screen:
 * Clean, minimalist controls for:
 * - AI Provider selector (horizontal chip row) & Provider configurations
 * - Generation Tuning (Temperature & Master Directive)
 * - Language, Default Preset, Theme, & Haptic feedback
 * - Excluded Apps
 * - About & Community / Zero-Telemetry Guarantee
 */
@Composable
fun EngineScreen(
    userSettings: StemUserSettings,
    onSelectEngineMode: (EngineMode) -> Unit,
    modifier: Modifier = Modifier,
    onSelectLanguagePreference: (LanguagePreference) -> Unit = {},
    onSelectDefaultPreset: (TransformPreset) -> Unit = {},
    onSaveTemperature: (Float) -> Unit = {},
    onSaveOllamaUrl: (String) -> Unit = {},
    onSaveOllamaModel: (String) -> Unit = {},
    onSaveGeminiSettings: (String, String) -> Unit = { _, _ -> },
    onSaveOpenAISettings: (String, String, String) -> Unit = { _, _, _ -> },
    onSaveClaudeSettings: (String, String) -> Unit = { _, _ -> },
    onClearGeminiApiKey: () -> Unit = {},
    onClearOpenAIApiKey: () -> Unit = {},
    onClearClaudeApiKey: () -> Unit = {},
    onSetPackageExcluded: (String, Boolean) -> Unit = { _, _ -> },
    onSaveCustomPromptInstruction: (String) -> Unit = {},
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onToggleHaptics: (Boolean) -> Unit = {}
) {
    val stemTheme = LocalStemColors.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var ollamaUrlInput by remember(userSettings.ollamaBaseUrl) { mutableStateOf(userSettings.ollamaBaseUrl) }
    var ollamaModelInput by remember(userSettings.ollamaModel) { mutableStateOf(userSettings.ollamaModel) }
    var geminiKeyInput by remember(userSettings.geminiApiKey) { mutableStateOf(userSettings.geminiApiKey) }
    var geminiModelInput by remember(userSettings.geminiModel) { mutableStateOf(userSettings.geminiModel) }
    var openAiUrlInput by remember(userSettings.openaiBaseUrl) { mutableStateOf(userSettings.openaiBaseUrl) }
    var openAiKeyInput by remember(userSettings.openaiApiKey) { mutableStateOf(userSettings.openaiApiKey) }
    var openAiModelInput by remember(userSettings.openaiModel) { mutableStateOf(userSettings.openaiModel) }
    var claudeKeyInput by remember(userSettings.claudeApiKey) { mutableStateOf(userSettings.claudeApiKey) }
    var claudeModelInput by remember(userSettings.claudeModel) { mutableStateOf(userSettings.claudeModel) }
    var customPromptInput by remember(userSettings.customPromptInstruction) { mutableStateOf(userSettings.customPromptInstruction) }

    val connectedMessage = stringResource(R.string.engine_test_connection_connected)
    val ollamaModelsFoundOne = stringResource(R.string.engine_ollama_models_found_one)
    val ollamaModelsFoundOther = stringResource(R.string.engine_ollama_models_found_other)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = stemTheme.bg,
        unfocusedContainerColor = stemTheme.bg,
        focusedBorderColor = stemTheme.ink,
        unfocusedBorderColor = stemTheme.borderSubtle,
        cursorColor = stemTheme.ink
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
    ) {
        // -------------------------------------------------------------
        // 1. ENGINE SELECTOR & CONFIGURATION
        // -------------------------------------------------------------
        item {
            Column {
                StemSectionHeader(
                    title = stringResource(R.string.engine_ai_provider_header),
                    subtitle = stringResource(R.string.engine_ai_provider_description)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal scrollable provider chips — clean & uncompressed on all screens
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(EngineMode.entries) { mode ->
                        val isSelected = userSettings.engineMode == mode
                        val targetBg = if (isSelected) stemTheme.ink else stemTheme.surface
                        val targetFg = if (isSelected) stemTheme.onInk else stemTheme.ink
                        val targetBorder = if (isSelected) stemTheme.ink else stemTheme.borderSubtle

                        val bg by animateColorAsState(targetBg, tween(150), label = "engineBg")
                        val fg by animateColorAsState(targetFg, tween(150), label = "engineFg")
                        val border by animateColorAsState(targetBorder, tween(150), label = "engineBorder")

                        val labelText = when (mode) {
                            EngineMode.LOCAL_RULES -> "Local Rules"
                            EngineMode.OLLAMA_AI -> "Ollama (LAN)"
                            EngineMode.GEMINI_AI -> "Gemini"
                            EngineMode.OPENAI_COMPATIBLE -> "OpenAI"
                            EngineMode.CLAUDE_AI -> "Claude"
                        }

                        Box(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 36.dp)
                                .clip(StemPillShape)
                                .background(bg)
                                .border(1.dp, border, StemPillShape)
                                .clickable(role = Role.RadioButton) { onSelectEngineMode(mode) }
                                .semantics { selected = isSelected }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = fg
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Engine configuration card
                StemCard {
                    when (userSettings.engineMode) {
                        EngineMode.LOCAL_RULES -> {
                            Text(
                                text = stringResource(R.string.engine_provider_local_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = stemTheme.inkMuted
                            )
                        }
                        EngineMode.OLLAMA_AI -> {
                            OutlinedTextField(
                                value = ollamaUrlInput,
                                onValueChange = { ollamaUrlInput = it },
                                label = { Text(stringResource(R.string.engine_server_url_label), style = MaterialTheme.typography.bodySmall) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onSaveOllamaUrl(ollamaUrlInput); focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged(rememberFocusLossSaver { onSaveOllamaUrl(ollamaUrlInput) }),
                                singleLine = true,
                                shape = StemSharpShape,
                                colors = textFieldColors
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = ollamaModelInput,
                                onValueChange = { ollamaModelInput = it },
                                label = { Text(stringResource(R.string.engine_model_name_label), style = MaterialTheme.typography.bodySmall) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onSaveOllamaModel(ollamaModelInput); focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged(rememberFocusLossSaver { onSaveOllamaModel(ollamaModelInput) }),
                                singleLine = true,
                                shape = StemSharpShape,
                                colors = textFieldColors
                            )
                            TestConnectionButton(
                                resetKey = ollamaUrlInput,
                                onTest = {
                                    OllamaClient.fetchAvailableModels(ollamaUrlInput).map { models ->
                                        val template = if (models.size == 1) ollamaModelsFoundOne else ollamaModelsFoundOther
                                        String.format(template, models.size)
                                    }
                                }
                            )
                        }
                        EngineMode.GEMINI_AI -> {
                            ApiKeyField(
                                value = geminiKeyInput,
                                onValueChange = { geminiKeyInput = it },
                                onSave = { onSaveGeminiSettings(geminiKeyInput, geminiModelInput) },
                                onClear = { geminiKeyInput = ""; onClearGeminiApiKey() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = geminiModelInput,
                                onValueChange = { geminiModelInput = it },
                                label = { Text(stringResource(R.string.engine_model_label), style = MaterialTheme.typography.bodySmall) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onSaveGeminiSettings(geminiKeyInput, geminiModelInput); focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged(rememberFocusLossSaver { onSaveGeminiSettings(geminiKeyInput, geminiModelInput) }),
                                singleLine = true,
                                shape = StemSharpShape,
                                colors = textFieldColors
                            )
                            TestConnectionButton(
                                resetKey = geminiKeyInput,
                                onTest = { GeminiClient.testConnection(geminiKeyInput).map { connectedMessage } }
                            )
                        }
                        EngineMode.OPENAI_COMPATIBLE -> {
                            OutlinedTextField(
                                value = openAiUrlInput,
                                onValueChange = { openAiUrlInput = it },
                                label = { Text(stringResource(R.string.engine_base_url_label), style = MaterialTheme.typography.bodySmall) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput); focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged(rememberFocusLossSaver { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput) }),
                                singleLine = true,
                                shape = StemSharpShape,
                                colors = textFieldColors
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ApiKeyField(
                                value = openAiKeyInput,
                                onValueChange = { openAiKeyInput = it },
                                onSave = { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput) },
                                onClear = { openAiKeyInput = ""; onClearOpenAIApiKey() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = openAiModelInput,
                                onValueChange = { openAiModelInput = it },
                                label = { Text(stringResource(R.string.engine_model_label), style = MaterialTheme.typography.bodySmall) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput); focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged(rememberFocusLossSaver { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput) }),
                                singleLine = true,
                                shape = StemSharpShape,
                                colors = textFieldColors
                            )
                            TestConnectionButton(
                                resetKey = openAiUrlInput to openAiKeyInput,
                                onTest = { OpenAIClient.testConnection(openAiUrlInput, openAiKeyInput).map { connectedMessage } }
                            )
                        }
                        EngineMode.CLAUDE_AI -> {
                            ApiKeyField(
                                value = claudeKeyInput,
                                onValueChange = { claudeKeyInput = it },
                                onSave = { onSaveClaudeSettings(claudeKeyInput, claudeModelInput) },
                                onClear = { claudeKeyInput = ""; onClearClaudeApiKey() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = claudeModelInput,
                                onValueChange = { claudeModelInput = it },
                                label = { Text(stringResource(R.string.engine_model_label), style = MaterialTheme.typography.bodySmall) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onSaveClaudeSettings(claudeKeyInput, claudeModelInput); focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged(rememberFocusLossSaver { onSaveClaudeSettings(claudeKeyInput, claudeModelInput) }),
                                singleLine = true,
                                shape = StemSharpShape,
                                colors = textFieldColors
                            )
                            TestConnectionButton(
                                resetKey = claudeKeyInput,
                                onTest = { ClaudeClient.testConnection(claudeKeyInput).map { connectedMessage } }
                            )
                        }
                    }

                    // Temperature & Master Directive (shown for AI engines)
                    if (userSettings.engineMode != EngineMode.LOCAL_RULES) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(stemTheme.borderSubtle)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        TemperatureControl(
                            temperature = userSettings.temperature,
                            onSaveTemperature = onSaveTemperature
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(stemTheme.borderSubtle)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        var isSaved by remember { mutableStateOf(false) }
                        Text(
                            text = stringResource(R.string.engine_master_directive_header),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = stemTheme.ink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customPromptInput,
                            onValueChange = {
                                customPromptInput = it
                                isSaved = false
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.engine_master_directive_placeholder),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = stemTheme.inkFaint
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged(rememberFocusLossSaver {
                                    onSaveCustomPromptInstruction(customPromptInput)
                                    isSaved = true
                                }),
                            shape = StemSharpShape,
                            minLines = 2,
                            maxLines = 3,
                            colors = textFieldColors
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSaved) {
                                Text(
                                    text = "✓ " + stringResource(R.string.engine_master_directive_saved),
                                    style = StemMonoBadge,
                                    color = stemTheme.add
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            StemButton(
                                text = stringResource(R.string.engine_master_directive_save),
                                onClick = {
                                    onSaveCustomPromptInstruction(customPromptInput)
                                    isSaved = true
                                    focusManager.clearFocus()
                                },
                                isPrimary = false
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. UNIFIED PREFERENCES CARD (Language, Preset, Theme, Haptics)
        // -------------------------------------------------------------
        item {
            Column {
                StemSectionHeader(title = stringResource(R.string.nav_tab_settings))

                Spacer(modifier = Modifier.height(8.dp))

                StemCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Language
                        Column {
                            Text(
                                text = stringResource(R.string.engine_language_header),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = stemTheme.ink
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val autoLabel = stringResource(R.string.engine_language_auto)
                            val englishLabel = stringResource(R.string.engine_language_english)
                            val spanishLabel = stringResource(R.string.engine_language_spanish)
                            val portugueseLabel = stringResource(R.string.engine_language_portuguese)

                            StemSegmentedGroup(
                                options = LanguagePreference.entries,
                                selected = userSettings.languagePreference,
                                onSelected = onSelectLanguagePreference,
                                label = { pref ->
                                    when (pref) {
                                        LanguagePreference.AUTO -> autoLabel
                                        LanguagePreference.ENGLISH -> englishLabel
                                        LanguagePreference.SPANISH -> spanishLabel
                                        LanguagePreference.PORTUGUESE -> portugueseLabel
                                    }
                                }
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(stemTheme.borderSubtle))

                        // Default Preset
                        Column {
                            Text(
                                text = stringResource(R.string.engine_default_preset_header),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = stemTheme.ink
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PresetChipsRow(
                                selectedPreset = userSettings.defaultPreset,
                                onPresetSelected = onSelectDefaultPreset,
                                compact = true,
                                presets = listOf(
                                    TransformPreset.FIX,
                                    TransformPreset.CONCISE,
                                    TransformPreset.PROFESSIONAL,
                                    TransformPreset.PUNCHY,
                                    TransformPreset.FRIENDLY,
                                    TransformPreset.SUMMARIZE,
                                    TransformPreset.BULLETIZE
                                )
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(stemTheme.borderSubtle))

                        // Appearance
                        Column {
                            Text(
                                text = stringResource(R.string.engine_appearance_header),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = stemTheme.ink
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val lightLabel = stringResource(R.string.engine_theme_light)
                            val darkLabel = stringResource(R.string.engine_theme_dark)
                            val systemLabel = stringResource(R.string.engine_theme_system)

                            StemSegmentedGroup(
                                options = ThemeMode.entries,
                                selected = userSettings.themeMode,
                                onSelected = onSelectThemeMode,
                                label = { mode ->
                                    when (mode) {
                                        ThemeMode.LIGHT -> lightLabel
                                        ThemeMode.DARK -> darkLabel
                                        ThemeMode.SYSTEM -> systemLabel
                                    }
                                }
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(stemTheme.borderSubtle))

                        // Haptic feedback
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.engine_haptic_feedback_title),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = stemTheme.ink
                                )
                                Text(
                                    text = stringResource(R.string.engine_haptic_feedback_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = stemTheme.inkMuted
                                )
                            }
                            Switch(
                                checked = userSettings.hapticFeedbackEnabled,
                                onCheckedChange = onToggleHaptics,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = stemTheme.onInk,
                                    checkedTrackColor = stemTheme.ink,
                                    uncheckedThumbColor = stemTheme.inkMuted,
                                    uncheckedTrackColor = stemTheme.surface2,
                                    uncheckedBorderColor = stemTheme.borderSubtle
                                )
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. EXCLUDED APPS CARD
        // -------------------------------------------------------------
        item {
            var showAppPicker by remember { mutableStateOf(false) }
            val excludedCount = userSettings.excludedPackages.size

            StemCard(
                onClick = { showAppPicker = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.engine_excluded_apps_header),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = stemTheme.ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (excludedCount == 0) {
                                stringResource(R.string.engine_excluded_apps_none)
                            } else {
                                pluralStringResource(R.plurals.engine_excluded_apps_count, excludedCount, excludedCount)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.inkMuted
                        )
                    }
                    Text(
                        text = stringResource(R.string.engine_manage_button) + " →",
                        style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                        color = stemTheme.ink
                    )
                }
            }

            if (showAppPicker) {
                AppExclusionDialog(
                    excludedPackages = userSettings.excludedPackages,
                    onSetExcluded = onSetPackageExcluded,
                    onDismiss = { showAppPicker = false }
                )
            }
        }

        // -------------------------------------------------------------
        // 4. ABOUT & OPEN SOURCE COMMUNITY
        // -------------------------------------------------------------
        item {
            StemCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StemLogoMark(size = 20.dp, tint = stemTheme.ink)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = stemTheme.ink
                        )
                    }
                    Text(
                        text = AppVersion.displayString,
                        style = StemMonoBadge,
                        color = stemTheme.inkFaint
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.engine_privacy_guarantee_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ko-fi action
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(StemSharpShape)
                            .background(stemTheme.surface2)
                            .border(1.dp, stemTheme.borderSubtle, StemSharpShape)
                            .clickable(role = Role.Button) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://ko-fi.com/X5R825DY4X".toUri())
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KoFiIcon(color = Color(0xFF72A4F2), size = 14.dp)
                            Text(text = "Ko-fi", style = StemMonoBadge.copy(fontWeight = FontWeight.Bold), color = stemTheme.ink)
                        }
                    }

                    // GitHub Sponsors action
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(StemSharpShape)
                            .background(stemTheme.surface2)
                            .border(1.dp, stemTheme.borderSubtle, StemSharpShape)
                            .clickable(role = Role.Button) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/sponsors/garc-kt".toUri())
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GitHubSponsorHeartIcon(color = Color(0xFFEA4AAA), size = 14.dp)
                            Text(text = stringResource(R.string.nav_sponsor_button), style = StemMonoBadge.copy(fontWeight = FontWeight.Bold), color = stemTheme.ink)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private sealed class TestConnectionStatus {
    object Idle : TestConnectionStatus()
    object Testing : TestConnectionStatus()
    data class Success(val message: String) : TestConnectionStatus()
    data class Failure(val message: String) : TestConnectionStatus()
}

@Composable
private fun TestConnectionButton(
    resetKey: Any,
    onTest: suspend () -> Result<String>
) {
    val stemTheme = LocalStemColors.current
    val scope = rememberCoroutineScope()
    var status by remember(resetKey) { mutableStateOf<TestConnectionStatus>(TestConnectionStatus.Idle) }
    val genericFailureMessage = stringResource(R.string.engine_test_connection_failed)

    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        StemButton(
            text = stringResource(if (status is TestConnectionStatus.Testing) R.string.engine_test_connection_testing else R.string.engine_test_connection_button),
            onClick = {
                status = TestConnectionStatus.Testing
                scope.launch {
                    val result = onTest()
                    status = result.fold(
                        onSuccess = { TestConnectionStatus.Success(it) },
                        onFailure = { TestConnectionStatus.Failure(it.message ?: genericFailureMessage) }
                    )
                }
            },
            enabled = status !is TestConnectionStatus.Testing,
            isPrimary = false
        )

        when (val s = status) {
            is TestConnectionStatus.Success -> {
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.engine_test_connection_success, s.message), style = MaterialTheme.typography.bodySmall, color = stemTheme.add)
            }
            is TestConnectionStatus.Failure -> {
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.engine_test_connection_failure, s.message), style = MaterialTheme.typography.bodySmall, color = stemTheme.remove, maxLines = 2)
            }
            else -> {}
        }
    }
}

@Composable
private fun TemperatureControl(
    temperature: Float,
    onSaveTemperature: (Float) -> Unit
) {
    val stemTheme = LocalStemColors.current
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.engine_temperature_label),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = stemTheme.ink
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    when {
                        temperature < 0.2f -> R.string.engine_temperature_precise
                        temperature < 0.5f -> R.string.engine_temperature_balanced
                        temperature < 0.8f -> R.string.engine_temperature_creative
                        else -> R.string.engine_temperature_experimental
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = stemTheme.inkMuted
            )
        }
        Text(
            text = String.format(java.util.Locale.US, "%.2f", temperature),
            style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
            color = stemTheme.ink
        )
    }
    Spacer(modifier = Modifier.height(2.dp))
    Slider(
        value = temperature,
        onValueChange = { onSaveTemperature(kotlin.math.round(it * 20f) / 20f) },
        valueRange = 0.0f..1.0f,
        steps = 19,
        colors = SliderDefaults.colors(
            thumbColor = stemTheme.ink,
            activeTrackColor = stemTheme.ink,
            inactiveTrackColor = stemTheme.surface2,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AppExclusionDialog(
    excludedPackages: Set<String>,
    onSetExcluded: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val stemTheme = LocalStemColors.current
    val context = LocalContext.current
    val apps = remember { InstalledAppsHelper.getLaunchableApps(context) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f),
            shape = StemCardShape,
            color = stemTheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.engine_excluded_apps_header),
                        style = StemMonoBadge,
                        color = stemTheme.inkFaint
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = stemTheme.inkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (apps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.engine_no_apps_found),
                        style = MaterialTheme.typography.bodySmall,
                        color = stemTheme.inkMuted,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(apps, key = { it.packageName }) { app ->
                            val isExcluded = app.packageName in excludedPackages
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Switch) { onSetExcluded(app.packageName, !isExcluded) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = stemTheme.ink,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Switch(
                                    checked = isExcluded,
                                    onCheckedChange = { checked -> onSetExcluded(app.packageName, checked) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = stemTheme.onInk,
                                        checkedTrackColor = stemTheme.ink,
                                        uncheckedThumbColor = stemTheme.inkMuted,
                                        uncheckedTrackColor = stemTheme.surface2,
                                        uncheckedBorderColor = stemTheme.borderSubtle
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
