package com.veggiebit.sprout.features.settings.ui.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.app.theme.LocalStemColors
import com.veggiebit.sprout.app.theme.StemCardShape
import com.veggiebit.sprout.app.theme.StemMonoBadge
import com.veggiebit.sprout.app.theme.StemSharpShape
import com.veggiebit.sprout.app.theme.ThemeMode
import com.veggiebit.sprout.features.enhancement.data.api.ClaudeClient
import com.veggiebit.sprout.features.enhancement.data.api.GeminiClient
import com.veggiebit.sprout.features.enhancement.data.api.OpenAIClient
import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.enhancement.data.ollama.OllamaClient
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import kotlinx.coroutines.launch

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
    userSettings: SproutUserSettings,
    onSelectEngineMode: (EngineMode) -> Unit,
    onSaveTemperature: (Float) -> Unit = {},
    onSaveOllamaUrl: (String) -> Unit = {},
    onSaveOllamaModel: (String) -> Unit = {},
    onSaveGeminiSettings: (String, String) -> Unit = { _, _ -> },
    onSaveOpenAISettings: (String, String, String) -> Unit = { _, _, _ -> },
    onSaveClaudeSettings: (String, String) -> Unit = { _, _ -> },
    onSaveCustomPromptInstruction: (String) -> Unit = {},
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onToggleHaptics: (Boolean) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current
    val scope = rememberCoroutineScope()

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

    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatusMessage by remember { mutableStateOf<String?>(null) }
    var isConnectionSuccess by remember { mutableStateOf(false) }

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
                    subtitle = "Run Llama 3, Mistral, Gemma on your home server.",
                    badge = "LAN",
                    isSelected = userSettings.engineMode == EngineMode.OLLAMA_AI,
                    onClick = { onSelectEngineMode(EngineMode.OLLAMA_AI) }
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = ollamaUrlInput,
                            onValueChange = {
                                ollamaUrlInput = it
                                onSaveOllamaUrl(it)
                            },
                            label = { Text("Server URL", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = ollamaModelInput,
                            onValueChange = {
                                ollamaModelInput = it
                                onSaveOllamaModel(it)
                            },
                            label = { Text("Model Name", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                    }
                }

                // 3. Google Gemini
                ProviderCard(
                    title = "Google Gemini",
                    subtitle = "Gemini 1.5 Flash / Pro via API key.",
                    badge = "CLOUD",
                    isSelected = userSettings.engineMode == EngineMode.GEMINI_AI,
                    onClick = { onSelectEngineMode(EngineMode.GEMINI_AI) }
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = geminiKeyInput,
                            onValueChange = {
                                geminiKeyInput = it
                                onSaveGeminiSettings(it, geminiModelInput)
                            },
                            label = { Text("API Key", style = MaterialTheme.typography.bodySmall) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = geminiModelInput,
                            onValueChange = {
                                geminiModelInput = it
                                onSaveGeminiSettings(geminiKeyInput, it)
                            },
                            label = { Text("Model", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                    }
                }

                // 4. OpenAI-Compatible
                ProviderCard(
                    title = "OpenAI-compatible",
                    subtitle = "OpenAI, Groq, Together, DeepSeek, LocalAI.",
                    badge = "CLOUD",
                    isSelected = userSettings.engineMode == EngineMode.OPENAI_COMPATIBLE,
                    onClick = { onSelectEngineMode(EngineMode.OPENAI_COMPATIBLE) }
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = openAiUrlInput,
                            onValueChange = {
                                openAiUrlInput = it
                                onSaveOpenAISettings(it, openAiKeyInput, openAiModelInput)
                            },
                            label = { Text("Base URL", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = openAiKeyInput,
                            onValueChange = {
                                openAiKeyInput = it
                                onSaveOpenAISettings(openAiUrlInput, it, openAiModelInput)
                            },
                            label = { Text("API Key", style = MaterialTheme.typography.bodySmall) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = openAiModelInput,
                            onValueChange = {
                                openAiModelInput = it
                                onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, it)
                            },
                            label = { Text("Model", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                    }
                }

                // 5. Anthropic Claude
                ProviderCard(
                    title = "Anthropic Claude",
                    subtitle = "Claude 3.5 Sonnet / Haiku via API key.",
                    badge = "CLOUD",
                    isSelected = userSettings.engineMode == EngineMode.CLAUDE_AI,
                    onClick = { onSelectEngineMode(EngineMode.CLAUDE_AI) }
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        OutlinedTextField(
                            value = claudeKeyInput,
                            onValueChange = {
                                claudeKeyInput = it
                                onSaveClaudeSettings(it, claudeModelInput)
                            },
                            label = { Text("API Key", style = MaterialTheme.typography.bodySmall) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = StemSharpShape
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = claudeModelInput,
                            onValueChange = {
                                claudeModelInput = it
                                onSaveClaudeSettings(claudeKeyInput, it)
                            },
                            label = { Text("Model", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = StemSharpShape
                        )
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
