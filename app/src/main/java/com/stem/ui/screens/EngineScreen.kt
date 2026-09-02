package com.stem.ui.screens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemCardShape
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemSharpShape
import com.stem.ui.theme.ThemeMode
import com.stem.core.models.EngineMode
import com.stem.core.models.StemUserSettings
import com.stem.core.util.InstalledAppsHelper



/**
 * Returns an onFocusChanged callback that invokes [onSave] only on a genuine focused-to-
 * unfocused transition — never on initial composition. Plain `onFocusChanged { if
 * (!it.isFocused) save() }` also fires the moment the modifier attaches (its first reported
 * state is unfocused), so a field re-entering composition — e.g. its LazyColumn item being
 * recomposed after scrolling off/on screen — resaves its current value unprompted. If that
 * value happens to be a blank/failed decrypt of a Keystore-encrypted API key, this used to
 * overwrite the last-good ciphertext with an empty string.
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
 * Password-style API key field with an explicit "Clear key" action. Overwriting a stored key
 * with a blank value is refused by [com.stem.core.models.PreferencesRepository] (a blind blur
 * or a failed decrypt must never silently erase a working key) — clearing is only ever this
 * deliberate action.
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
            label = { Text("API Key", style = MaterialTheme.typography.bodySmall) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave(); focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged(rememberFocusLossSaver(onSave)),
            singleLine = true,
            shape = StemSharpShape
        )
        if (value.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Clear key",
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
 * - AI Provider selector & inline API configurations
 * - Appearance segmented mode toggle (Light / Dark)
 * - Haptic feedback toggle
 * - Privacy Guarantee card
 * Matches Stem.dc.html design specification.
 */
@Composable
fun EngineScreen(
    userSettings: StemUserSettings,
    onSelectEngineMode: (EngineMode) -> Unit,
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
    onToggleHaptics: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current
    val scope = rememberCoroutineScope()
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
    ) {
        // Section: AI Provider
        item {
            Column {
                Text(
                    text = "AI PROVIDER",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose how Stem processes text. On-device rules work offline with zero latency.",
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )
            }
        }

        // Provider Options List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. On-Device Rules
                ProviderCard(
                    title = "On-device rules",
                    subtitle = "Fast, private, zero setup. 40+ grammar & style rules.",
                    badge = "LOCAL",
                    isSelected = userSettings.engineMode == EngineMode.LOCAL_RULES,
                    onClick = { onSelectEngineMode(EngineMode.LOCAL_RULES) }
                )

                // 2. Ollama LAN
                ProviderCard(
                    title = "Ollama (Local LAN)",
                    subtitle = "Llama, Mistral, Gemma, local models",
                    badge = "LAN",
                    isSelected = userSettings.engineMode == EngineMode.OLLAMA_AI,
                    onClick = { onSelectEngineMode(EngineMode.OLLAMA_AI) }
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = ollamaUrlInput,
                            onValueChange = { ollamaUrlInput = it },
                            label = { Text("Server URL", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onSaveOllamaUrl(ollamaUrlInput); focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged(rememberFocusLossSaver { onSaveOllamaUrl(ollamaUrlInput) }),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = ollamaModelInput,
                            onValueChange = { ollamaModelInput = it },
                            label = { Text("Model Name", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onSaveOllamaModel(ollamaModelInput); focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged(rememberFocusLossSaver { onSaveOllamaModel(ollamaModelInput) }),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        TemperatureControl(
                            temperature = userSettings.temperature,
                            onSaveTemperature = onSaveTemperature
                        )
                    }
                }

                // 3. Google Gemini
                ProviderCard(
                    title = "Google Gemini",
                    subtitle = "Gemini Models (Flash, Pro)",
                    badge = "CLOUD",
                    isSelected = userSettings.engineMode == EngineMode.GEMINI_AI,
                    onClick = { onSelectEngineMode(EngineMode.GEMINI_AI) }
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        ApiKeyField(
                            value = geminiKeyInput,
                            onValueChange = { geminiKeyInput = it },
                            onSave = { onSaveGeminiSettings(geminiKeyInput, geminiModelInput) },
                            onClear = { geminiKeyInput = ""; onClearGeminiApiKey() }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = geminiModelInput,
                            onValueChange = { geminiModelInput = it },
                            label = { Text("Model", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onSaveGeminiSettings(geminiKeyInput, geminiModelInput); focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged(rememberFocusLossSaver { onSaveGeminiSettings(geminiKeyInput, geminiModelInput) }),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        TemperatureControl(
                            temperature = userSettings.temperature,
                            onSaveTemperature = onSaveTemperature
                        )
                    }
                }

                // 4. OpenAI-Compatible
                ProviderCard(
                    title = "OpenAI-compatible",
                    subtitle = "ChatGPT, DeepSeek, Groq, OpenRouter models",
                    badge = "CLOUD",
                    isSelected = userSettings.engineMode == EngineMode.OPENAI_COMPATIBLE,
                    onClick = { onSelectEngineMode(EngineMode.OPENAI_COMPATIBLE) }
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = openAiUrlInput,
                            onValueChange = { openAiUrlInput = it },
                            label = { Text("Base URL", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput); focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged(rememberFocusLossSaver { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput) }),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ApiKeyField(
                            value = openAiKeyInput,
                            onValueChange = { openAiKeyInput = it },
                            onSave = { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput) },
                            onClear = { openAiKeyInput = ""; onClearOpenAIApiKey() }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = openAiModelInput,
                            onValueChange = { openAiModelInput = it },
                            label = { Text("Model", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput); focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged(rememberFocusLossSaver { onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput) }),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        TemperatureControl(
                            temperature = userSettings.temperature,
                            onSaveTemperature = onSaveTemperature
                        )
                    }
                }

                // 5. Anthropic Claude
                ProviderCard(
                    title = "Anthropic Claude",
                    subtitle = "Claude Models (Sonnet, Haiku)",
                    badge = "CLOUD",
                    isSelected = userSettings.engineMode == EngineMode.CLAUDE_AI,
                    onClick = { onSelectEngineMode(EngineMode.CLAUDE_AI) }
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        ApiKeyField(
                            value = claudeKeyInput,
                            onValueChange = { claudeKeyInput = it },
                            onSave = { onSaveClaudeSettings(claudeKeyInput, claudeModelInput) },
                            onClear = { claudeKeyInput = ""; onClearClaudeApiKey() }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = claudeModelInput,
                            onValueChange = { claudeModelInput = it },
                            label = { Text("Model", style = MaterialTheme.typography.bodySmall) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onSaveClaudeSettings(claudeKeyInput, claudeModelInput); focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged(rememberFocusLossSaver { onSaveClaudeSettings(claudeKeyInput, claudeModelInput) }),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        // No TemperatureControl here: current Claude models (Sonnet 5 / Opus 5 /
                        // 4.7+) reject a temperature parameter with a 400, and ClaudeClient
                        // never sends one — a slider that does nothing is worse than no slider.
                    }
                }
            }
        }

        // Section: Appearance
        item {
            Column {
                Text(
                    text = "APPEARANCE",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StemCardShape)
                        .background(stemTheme.surface)
                        .border(1.dp, stemTheme.border, StemCardShape)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark",
                            ThemeMode.SYSTEM to "System"
                        ).forEach { (mode, label) ->
                            val isSelected = userSettings.themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(StemSharpShape)
                                    .background(if (isSelected) stemTheme.ink else stemTheme.surface2)
                                    .border(1.dp, if (isSelected) stemTheme.ink else stemTheme.border, StemSharpShape)
                                    .clickable(role = Role.RadioButton) { onSelectThemeMode(mode) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) stemTheme.onInk else stemTheme.ink
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Haptics
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic feedback",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = stemTheme.ink
                        )
                        Text(
                            text = "Gentle vibration on text replacement",
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
                            uncheckedTrackColor = stemTheme.surface2
                        )
                    )
                }
            }
        }

        // Section: Excluded Apps
        item {
            Column {
                Text(
                    text = "EXCLUDED APPS",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stem reads text from every app by default. Exclude apps you don't want it active in — password managers and banking apps are good candidates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = stemTheme.inkMuted
                )
            }
        }
        item {
            var showAppPicker by remember { mutableStateOf(false) }
            val excludedCount = userSettings.excludedPackages.size

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .clickable(role = Role.Button, onClick = { showAppPicker = true })
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (excludedCount == 0) "No apps excluded" else "$excludedCount app${if (excludedCount != 1) "s" else ""} excluded",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = stemTheme.ink
                    )
                    Text(
                        text = "MANAGE",
                        style = StemMonoBadge,
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

        // Section: Privacy Guarantee Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "PRIVACY GUARANTEE",
                        style = StemMonoBadge,
                        color = stemTheme.ink
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Text is only sent to the provider you select. Keystrokes are never logged or stored off-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = stemTheme.inkMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    title: String,
    subtitle: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    val stemTheme = LocalStemColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StemCardShape)
            .background(stemTheme.surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) stemTheme.ink else stemTheme.border,
                shape = StemCardShape
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.5.dp, if (isSelected) stemTheme.ink else stemTheme.border, StemSharpShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(stemTheme.ink)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = stemTheme.ink
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(StemSharpShape)
                        .background(stemTheme.surface2)
                        .border(1.dp, stemTheme.border, StemSharpShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = StemMonoBadge,
                        color = stemTheme.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = stemTheme.inkMuted,
                modifier = Modifier.padding(start = 26.dp)
            )

            if (isSelected && content != null) {
                Box(modifier = Modifier.padding(start = 26.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun TemperatureControl(
    temperature: Float,
    onSaveTemperature: (Float) -> Unit
) {
    val stemTheme = LocalStemColors.current
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Temperature",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = stemTheme.ink
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    temperature < 0.2f -> "(Precise)"
                    temperature < 0.5f -> "(Balanced)"
                    temperature < 0.8f -> "(Creative)"
                    else -> "(Experimental)"
                },
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
                        text = "EXCLUDED APPS",
                        style = StemMonoBadge,
                        color = stemTheme.inkFaint
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = stemTheme.inkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (apps.isEmpty()) {
                    Text(
                        text = "No other apps found on this device.",
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
                                        uncheckedTrackColor = stemTheme.surface2
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

