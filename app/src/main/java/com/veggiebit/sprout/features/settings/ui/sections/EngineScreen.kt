package com.veggiebit.sprout.features.settings.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.veggiebit.sprout.features.enhancement.data.api.ClaudeClient
import com.veggiebit.sprout.features.enhancement.data.api.GeminiClient
import com.veggiebit.sprout.features.enhancement.data.api.OpenAIClient
import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.enhancement.data.ollama.OllamaClient
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import com.veggiebit.sprout.features.settings.ui.components.SproutSubScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EngineScreen(
    userSettings: SproutUserSettings,
    onSelectEngineMode: (EngineMode) -> Unit,
    onSaveTemperature: (Float) -> Unit,
    onSaveOllamaUrl: (String) -> Unit,
    onSaveOllamaModel: (String) -> Unit,
    onSaveGeminiSettings: (String, String) -> Unit,
    onSaveOpenAISettings: (String, String, String) -> Unit,
    onSaveClaudeSettings: (String, String) -> Unit,
    onSaveCustomPromptInstruction: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var ollamaUrlInput by remember(userSettings.ollamaBaseUrl) { mutableStateOf(userSettings.ollamaBaseUrl) }
    var ollamaModelInput by remember(userSettings.ollamaModel) { mutableStateOf(userSettings.ollamaModel) }
    var geminiKeyInput by remember(userSettings.geminiApiKey) { mutableStateOf(userSettings.geminiApiKey) }
    var geminiModelInput by remember(userSettings.geminiModel) { mutableStateOf(userSettings.geminiModel) }
    var openAiUrlInput by remember(userSettings.openaiBaseUrl) { mutableStateOf(userSettings.openaiBaseUrl) }
    var openAiKeyInput by remember(userSettings.openaiApiKey) { mutableStateOf(userSettings.openaiApiKey) }
    var openAiModelInput by remember(userSettings.openaiModel) { mutableStateOf(userSettings.openaiModel) }
    var claudeKeyInput by remember(userSettings.claudeApiKey) { mutableStateOf(userSettings.claudeApiKey) }
    var claudeModelInput by remember(userSettings.claudeModel) { mutableStateOf(userSettings.claudeModel) }
    var temperatureInput by remember(userSettings.temperature) { mutableFloatStateOf(userSettings.temperature) }
    var customPromptInput by remember(userSettings.customPromptInstruction) { mutableStateOf(userSettings.customPromptInstruction) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatusMessage by remember { mutableStateOf<String?>(null) }
    var isConnectionSuccess by remember { mutableStateOf(false) }

    SproutSubScreen(title = "AI Intelligence Engine", onBack = onBack, modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EngineMode.entries.forEach { mode ->
                        val isSelected = userSettings.engineMode == mode
                        val icon: ImageVector = when (mode) {
                            EngineMode.LOCAL_RULES -> Icons.Rounded.Spa
                            EngineMode.OLLAMA_AI -> Icons.Rounded.Computer
                            EngineMode.GEMINI_AI -> Icons.Rounded.AutoAwesome
                            EngineMode.OPENAI_COMPATIBLE -> Icons.Rounded.Code
                            EngineMode.CLAUDE_AI -> Icons.Rounded.Psychology
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelectEngineMode(mode) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (userSettings.engineMode != EngineMode.LOCAL_RULES) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = "${userSettings.engineMode.title} Configuration",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            when (userSettings.engineMode) {
                                EngineMode.OLLAMA_AI -> {
                                    OutlinedTextField(
                                        value = ollamaUrlInput,
                                        onValueChange = { ollamaUrlInput = it },
                                        label = { Text("Ollama Host URL") },
                                        placeholder = { Text("http://192.168.1.X:11434") },
                                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                                            if (!it.isFocused) onSaveOllamaUrl(ollamaUrlInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = ollamaModelInput,
                                        onValueChange = { ollamaModelInput = it },
                                        label = { Text("Model Name") },
                                        placeholder = { Text("llama3.3, deepseek-r1:8b, qwen2.5") },
                                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                                            if (!it.isFocused) onSaveOllamaModel(ollamaModelInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("llama3.3", "llama3.2", "deepseek-r1:8b", "deepseek-r1:14b", "qwen2.5", "phi4").forEach { suggestion ->
                                            AssistChip(
                                                onClick = { ollamaModelInput = suggestion; onSaveOllamaModel(suggestion) },
                                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }

                                EngineMode.GEMINI_AI -> {
                                    OutlinedTextField(
                                        value = geminiKeyInput,
                                        onValueChange = { geminiKeyInput = it },
                                        label = { Text("Google Gemini API Key") },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                                            if (!it.isFocused) onSaveGeminiSettings(geminiKeyInput, geminiModelInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = geminiModelInput,
                                        onValueChange = { geminiModelInput = it },
                                        label = { Text("Model") },
                                        placeholder = { Text("gemini-2.0-flash, gemini-2.5-flash") },
                                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                                            if (!it.isFocused) onSaveGeminiSettings(geminiKeyInput, geminiModelInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-2.5-flash", "gemini-2.5-pro").forEach { suggestion ->
                                            AssistChip(
                                                onClick = { geminiModelInput = suggestion; onSaveGeminiSettings(geminiKeyInput, suggestion) },
                                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }

                                EngineMode.OPENAI_COMPATIBLE -> {
                                    OutlinedTextField(
                                        value = openAiUrlInput,
                                        onValueChange = { openAiUrlInput = it },
                                        label = { Text("Base URL") },
                                        placeholder = { Text("https://api.openai.com/v1") },
                                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                                            if (!it.isFocused) onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = openAiKeyInput,
                                        onValueChange = { openAiKeyInput = it },
                                        label = { Text("API Key") },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                                            if (!it.isFocused) onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = openAiModelInput,
                                        onValueChange = { openAiModelInput = it },
                                        label = { Text("Model") },
                                        placeholder = { Text("gpt-4o-mini, deepseek-chat, o3-mini") },
                                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                                            if (!it.isFocused) onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("gpt-4o-mini", "gpt-4o", "o3-mini", "deepseek-chat", "deepseek-reasoner").forEach { suggestion ->
                                            AssistChip(
                                                onClick = { openAiModelInput = suggestion; onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, suggestion) },
                                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }

                                EngineMode.CLAUDE_AI -> {
                                    OutlinedTextField(
                                        value = claudeKeyInput,
                                        onValueChange = { claudeKeyInput = it },
                                        label = { Text("Anthropic API Key") },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                                            if (!it.isFocused) onSaveClaudeSettings(claudeKeyInput, claudeModelInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = claudeModelInput,
                                        onValueChange = { claudeModelInput = it },
                                        label = { Text("Model") },
                                        placeholder = { Text("claude-3-7-sonnet-20250219, claude-3-5-sonnet-20241022") },
                                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                                            if (!it.isFocused) onSaveClaudeSettings(claudeKeyInput, claudeModelInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("claude-3-7-sonnet-20250219", "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022").forEach { suggestion ->
                                            AssistChip(
                                                onClick = { claudeModelInput = suggestion; onSaveClaudeSettings(claudeKeyInput, suggestion) },
                                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }

                                EngineMode.LOCAL_RULES -> {}
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Temperature (Creativity / Randomness) Controls
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Temperature (Creativity)",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val tempLabel = when {
                                            temperatureInput <= 0.2f -> "Precise (${String.format(java.util.Locale.US, "%.2f", temperatureInput)})"
                                            temperatureInput <= 0.6f -> "Balanced (${String.format(java.util.Locale.US, "%.2f", temperatureInput)})"
                                            else -> "Creative (${String.format(java.util.Locale.US, "%.2f", temperatureInput)})"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = tempLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Controls AI output predictability. Lower values (0.0–0.3) provide precise, deterministic corrections. Higher values (0.7–1.0) encourage creative, varied phrasing.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Slider(
                                        value = temperatureInput,
                                        onValueChange = {
                                            temperatureInput = it
                                            onSaveTemperature(it)
                                        },
                                        valueRange = 0.0f..1.0f,
                                        steps = 19
                                    )

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            0.1f to "Precise (0.1)",
                                            0.3f to "Balanced (0.3)",
                                            0.7f to "Creative (0.7)",
                                            1.0f to "Max Diversity (1.0)"
                                        ).forEach { (tVal, tName) ->
                                            AssistChip(
                                                onClick = {
                                                    temperatureInput = tVal
                                                    onSaveTemperature(tVal)
                                                },
                                                label = { Text(tName, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Master Enhancement Directive Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Master Enhancing Directive",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (customPromptInput.isNotBlank()) {
                                            TextButton(
                                                onClick = {
                                                    customPromptInput = ""
                                                    onSaveCustomPromptInstruction("")
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Injected into all AI transformations to elevate phrasing, style, and tone beyond standard grammar fixes.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = customPromptInput,
                                        onValueChange = {
                                            customPromptInput = it
                                            onSaveCustomPromptInstruction(it)
                                        },
                                        placeholder = {
                                            Text(
                                                "e.g. Elevate vocabulary, refine flow, and ensure confident, engaging tone",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        minLines = 2,
                                        maxLines = 4
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Quick Enhancement Styles:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "Elevate vocabulary, flow, and sentence rhythm with high elegance" to "✨ Deep Polish",
                                            "Be crystal clear, direct, and eliminate all passive filler" to "🎯 Clear & Direct",
                                            "Write with charismatic, persuasive, and engaging energy" to "🔥 Persuasive",
                                            "Ensure native, natural phrasing with polished phrasing" to "🌍 Native Phrasing",
                                            "Speak with executive authority, diplomatic tact, and status" to "💼 Executive"
                                        ).forEach { (instruction, label) ->
                                            AssistChip(
                                                onClick = {
                                                    customPromptInput = instruction
                                                    onSaveCustomPromptInstruction(instruction)
                                                },
                                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isTestingConnection = true
                                        connectionStatusMessage = null
                                        scope.launch {
                                            when (userSettings.engineMode) {
                                                EngineMode.OLLAMA_AI -> {
                                                    onSaveOllamaUrl(ollamaUrlInput)
                                                    val result = OllamaClient.fetchAvailableModels(ollamaUrlInput)
                                                    result.fold(
                                                        onSuccess = { models ->
                                                            isConnectionSuccess = true
                                                            connectionStatusMessage = "Connected (${models.size} models)"
                                                            if (models.isNotEmpty() && !models.contains(ollamaModelInput)) {
                                                                ollamaModelInput = models.first()
                                                                onSaveOllamaModel(models.first())
                                                            }
                                                        },
                                                        onFailure = { error ->
                                                            isConnectionSuccess = false
                                                            connectionStatusMessage = "Failed: ${error.localizedMessage}"
                                                        }
                                                    )
                                                }
                                                EngineMode.GEMINI_AI -> {
                                                    onSaveGeminiSettings(geminiKeyInput, geminiModelInput)
                                                    val result = GeminiClient.generate(geminiKeyInput, geminiModelInput, "Hello", "Respond with 'OK'.", temperature = temperatureInput)
                                                    result.fold(
                                                        onSuccess = { isConnectionSuccess = true; connectionStatusMessage = "Gemini Connected ($it)" },
                                                        onFailure = { error -> isConnectionSuccess = false; connectionStatusMessage = "Failed: ${error.localizedMessage}" }
                                                    )
                                                }
                                                EngineMode.OPENAI_COMPATIBLE -> {
                                                    onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, openAiModelInput)
                                                    val result = OpenAIClient.generate(openAiUrlInput, openAiKeyInput, openAiModelInput, "Hello", "Respond with 'OK'.", temperature = temperatureInput)
                                                    result.fold(
                                                        onSuccess = { isConnectionSuccess = true; connectionStatusMessage = "Connected ($it)" },
                                                        onFailure = { error -> isConnectionSuccess = false; connectionStatusMessage = "Failed: ${error.localizedMessage}" }
                                                    )
                                                }
                                                EngineMode.CLAUDE_AI -> {
                                                    onSaveClaudeSettings(claudeKeyInput, claudeModelInput)
                                                    val result = ClaudeClient.generate(claudeKeyInput, claudeModelInput, "Hello", "Respond with 'OK'.", temperature = temperatureInput)
                                                    result.fold(
                                                        onSuccess = { isConnectionSuccess = true; connectionStatusMessage = "Claude Connected ($it)" },
                                                        onFailure = { error -> isConnectionSuccess = false; connectionStatusMessage = "Failed: ${error.localizedMessage}" }
                                                    )
                                                }
                                                EngineMode.LOCAL_RULES -> {}
                                            }
                                            isTestingConnection = false
                                        }
                                    },
                                    enabled = !isTestingConnection,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isTestingConnection) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Testing...")
                                    } else {
                                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Test Connection")
                                    }
                                }

                                connectionStatusMessage?.let { msg ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                                        Icon(
                                            imageVector = if (isConnectionSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = if (isConnectionSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isConnectionSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Custom Prompt",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Used by the Custom preset — write your own instruction and any AI engine will follow it (local rules apply Fix & Polish instead, since there's no model to interpret it).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = customPromptInput,
                onValueChange = { customPromptInput = it },
                modifier = Modifier.fillMaxWidth().onFocusChanged {
                    if (!it.isFocused) onSaveCustomPromptInstruction(customPromptInput)
                },
                placeholder = { Text("e.g. \"Rewrite as a haiku\" or \"Translate to French\"") },
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
