package com.veggiebit.sprout.features.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.core.version.AppVersion
import com.veggiebit.sprout.features.enhancement.data.api.ClaudeClient
import com.veggiebit.sprout.features.enhancement.data.api.GeminiClient
import com.veggiebit.sprout.features.enhancement.data.api.OpenAIClient
import com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider
import com.veggiebit.sprout.features.enhancement.data.models.EngineMode
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.enhancement.data.ollama.OllamaClient
import com.veggiebit.sprout.features.enhancement.ui.components.DiffViewer
import com.veggiebit.sprout.features.enhancement.ui.components.PresetChipsRow
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import com.veggiebit.sprout.features.settings.ui.components.PermissionStepCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    userSettings: SproutUserSettings,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onSelectDefaultPreset: (TransformPreset) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSelectEngineMode: (EngineMode) -> Unit,
    onSaveOllamaUrl: (String) -> Unit,
    onSaveOllamaModel: (String) -> Unit,
    onSaveGeminiSettings: (String, String) -> Unit,
    onSaveOpenAISettings: (String, String, String) -> Unit,
    onSaveClaudeSettings: (String, String) -> Unit,
    onSaveSnippet: (String, String) -> Unit,
    onDeleteSnippet: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Test Sandbox State
    var sandboxText by remember { mutableStateOf("I has an idea for the meting tomorow. It will be realy great.") }
    var sandboxPreset by remember { mutableStateOf(TransformPreset.FIX) }
    var sandboxResult by remember { mutableStateOf<TransformResult?>(null) }
    var isSandboxLoading by remember { mutableStateOf(false) }

    // Ollama Local State
    var ollamaUrlInput by remember(userSettings.ollamaBaseUrl) { mutableStateOf(userSettings.ollamaBaseUrl) }
    var ollamaModelInput by remember(userSettings.ollamaModel) { mutableStateOf(userSettings.ollamaModel) }
    var geminiKeyInput by remember(userSettings.geminiApiKey) { mutableStateOf(userSettings.geminiApiKey) }
    var geminiModelInput by remember(userSettings.geminiModel) { mutableStateOf(userSettings.geminiModel) }
    var openAiUrlInput by remember(userSettings.openaiBaseUrl) { mutableStateOf(userSettings.openaiBaseUrl) }
    var openAiKeyInput by remember(userSettings.openaiApiKey) { mutableStateOf(userSettings.openaiApiKey) }
    var openAiModelInput by remember(userSettings.openaiModel) { mutableStateOf(userSettings.openaiModel) }
    var claudeKeyInput by remember(userSettings.claudeApiKey) { mutableStateOf(userSettings.claudeApiKey) }
    var claudeModelInput by remember(userSettings.claudeModel) { mutableStateOf(userSettings.claudeModel) }

    var isPasswordVisible by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatusMessage by remember { mutableStateOf<String?>(null) }
    var isConnectionSuccess by remember { mutableStateOf(false) }
    var discoveredModels by remember { mutableStateOf<List<String>>(emptyList()) }

    // New Snippet Inputs
    var newSnippetKey by remember { mutableStateOf("") }
    var newSnippetValue by remember { mutableStateOf("") }

    LaunchedEffect(sandboxText, sandboxPreset, userSettings) {
        val payload = TextPayload(text = sandboxText)
        val engine = TextEngineProvider.getEngine(userSettings)
        sandboxResult = engine.transform(payload, sandboxPreset)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Spa,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sprout",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "VeggieBit Studios • ${AppVersion.displayString}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Setup Section
            Text(
                text = "Setup & Permissions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionStepCard(
                stepNumber = 1,
                title = "Accessibility Service",
                description = "Monitors text focus & provides inline replacement across apps.",
                icon = Icons.Rounded.TouchApp,
                isGranted = hasAccessibilityPermission,
                onGrantClick = onRequestAccessibilityPermission
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionStepCard(
                stepNumber = 2,
                title = "Display Over Other Apps",
                description = "Shows the floating 36dp pill & expanded suggestion capsule.",
                icon = Icons.Rounded.Layers,
                isGranted = hasOverlayPermission,
                onGrantClick = onRequestOverlayPermission
            )

            if (!hasAccessibilityPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "💡 Tip (Android 13–17): If Android shows 'Restricted setting', open App Info > tap 3 dots (⋮) in top right > tap 'Allow restricted settings'.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. AI Intelligence Mode & Ollama PC Integration
            Text(
                text = "AI Intelligence Engine",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Engine Mode Choice Cards
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
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
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
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
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

                    // Expandable Config for Non-Local Engines
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
                                        onValueChange = {
                                            ollamaUrlInput = it
                                            onSaveOllamaUrl(it)
                                        },
                                        label = { Text("Ollama Host URL") },
                                        placeholder = { Text("http://192.168.1.X:11434") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = ollamaModelInput,
                                        onValueChange = {
                                            ollamaModelInput = it
                                            onSaveOllamaModel(it)
                                        },
                                        label = { Text("Model Name") },
                                        placeholder = { Text("llama3.3, llama3.2, qwen2.5") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    // Quick Model Suggestion Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("llama3.3", "llama3.2", "qwen2.5", "deepseek-r1:8b").forEach { suggestion ->
                                            AssistChip(
                                                onClick = {
                                                    ollamaModelInput = suggestion
                                                    onSaveOllamaModel(suggestion)
                                                },
                                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }

                                EngineMode.GEMINI_AI -> {
                                    OutlinedTextField(
                                        value = geminiKeyInput,
                                        onValueChange = {
                                            geminiKeyInput = it
                                            onSaveGeminiSettings(it, geminiModelInput)
                                        },
                                        label = { Text("Google Gemini API Key") },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = geminiModelInput,
                                        onValueChange = {
                                            geminiModelInput = it
                                            onSaveGeminiSettings(geminiKeyInput, it)
                                        },
                                        label = { Text("Model") },
                                        placeholder = { Text("gemini-2.0-flash, gemini-2.5-flash") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    // Quick Model Suggestion Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("gemini-2.0-flash", "gemini-2.5-flash", "gemini-1.5-flash", "gemini-1.5-pro").forEach { suggestion ->
                                            AssistChip(
                                                onClick = {
                                                    geminiModelInput = suggestion
                                                    onSaveGeminiSettings(geminiKeyInput, suggestion)
                                                },
                                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }

                                EngineMode.OPENAI_COMPATIBLE -> {
                                    OutlinedTextField(
                                        value = openAiUrlInput,
                                        onValueChange = {
                                            openAiUrlInput = it
                                            onSaveOpenAISettings(it, openAiKeyInput, openAiModelInput)
                                        },
                                        label = { Text("Base URL") },
                                        placeholder = { Text("https://api.openai.com/v1") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = openAiKeyInput,
                                        onValueChange = {
                                            openAiKeyInput = it
                                            onSaveOpenAISettings(openAiUrlInput, it, openAiModelInput)
                                        },
                                        label = { Text("API Key") },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = openAiModelInput,
                                        onValueChange = {
                                            openAiModelInput = it
                                            onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, it)
                                        },
                                        label = { Text("Model") },
                                        placeholder = { Text("gpt-4o-mini, deepseek-chat, gpt-4o") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    // Quick Model Suggestion Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("gpt-4o-mini", "gpt-4o", "deepseek-chat", "deepseek-reasoner").forEach { suggestion ->
                                            AssistChip(
                                                onClick = {
                                                    openAiModelInput = suggestion
                                                    onSaveOpenAISettings(openAiUrlInput, openAiKeyInput, suggestion)
                                                },
                                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }

                                EngineMode.CLAUDE_AI -> {
                                    OutlinedTextField(
                                        value = claudeKeyInput,
                                        onValueChange = {
                                            claudeKeyInput = it
                                            onSaveClaudeSettings(it, claudeModelInput)
                                        },
                                        label = { Text("Anthropic API Key") },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = claudeModelInput,
                                        onValueChange = {
                                            claudeModelInput = it
                                            onSaveClaudeSettings(claudeKeyInput, it)
                                        },
                                        label = { Text("Model") },
                                        placeholder = { Text("claude-3-7-sonnet-20250219") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    // Quick Model Suggestion Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("claude-3-7-sonnet-20250219", "claude-3-5-haiku-20241022", "claude-3-5-sonnet-20241022").forEach { suggestion ->
                                            AssistChip(
                                                onClick = {
                                                    claudeModelInput = suggestion
                                                    onSaveClaudeSettings(claudeKeyInput, suggestion)
                                                },
                                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Test Connection Button & Status
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
                                                    val result = OllamaClient.fetchAvailableModels(ollamaUrlInput)
                                                    result.fold(
                                                        onSuccess = { models ->
                                                            isConnectionSuccess = true
                                                            discoveredModels = models
                                                            connectionStatusMessage = "Connected (${models.size} models)"
                                                            if (models.isNotEmpty() && !models.contains(ollamaModelInput)) {
                                                                ollamaModelInput = models.first()
                                                                onSaveOllamaModel(models.first())
                                                            }
                                                        },
                                                        onFailure = { error ->
                                                            isConnectionSuccess = false
                                                            discoveredModels = emptyList()
                                                            connectionStatusMessage = "Failed: ${error.localizedMessage}"
                                                        }
                                                    )
                                                }
                                                EngineMode.GEMINI_AI -> {
                                                    val result = GeminiClient.generate(
                                                        apiKey = geminiKeyInput,
                                                        model = geminiModelInput,
                                                        prompt = "Hello",
                                                        systemPrompt = "Respond with 'OK'."
                                                    )
                                                    result.fold(
                                                        onSuccess = {
                                                            isConnectionSuccess = true
                                                            connectionStatusMessage = "Gemini Connected ($it)"
                                                        },
                                                        onFailure = { error ->
                                                            isConnectionSuccess = false
                                                            connectionStatusMessage = "Failed: ${error.localizedMessage}"
                                                        }
                                                    )
                                                }
                                                EngineMode.OPENAI_COMPATIBLE -> {
                                                    val result = OpenAIClient.generate(
                                                        baseUrl = openAiUrlInput,
                                                        apiKey = openAiKeyInput,
                                                        model = openAiModelInput,
                                                        prompt = "Hello",
                                                        systemPrompt = "Respond with 'OK'."
                                                    )
                                                    result.fold(
                                                        onSuccess = {
                                                            isConnectionSuccess = true
                                                            connectionStatusMessage = "Connected ($it)"
                                                        },
                                                        onFailure = { error ->
                                                            isConnectionSuccess = false
                                                            connectionStatusMessage = "Failed: ${error.localizedMessage}"
                                                        }
                                                    )
                                                }
                                                EngineMode.CLAUDE_AI -> {
                                                    val result = ClaudeClient.generate(
                                                        apiKey = claudeKeyInput,
                                                        model = claudeModelInput,
                                                        prompt = "Hello",
                                                        systemPrompt = "Respond with 'OK'."
                                                    )
                                                    result.fold(
                                                        onSuccess = {
                                                            isConnectionSuccess = true
                                                            connectionStatusMessage = "Claude Connected ($it)"
                                                        },
                                                        onFailure = { error ->
                                                            isConnectionSuccess = false
                                                            connectionStatusMessage = "Failed: ${error.localizedMessage}"
                                                        }
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
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Testing...")
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Test Connection")
                                    }
                                }

                                connectionStatusMessage?.let { msg ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
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

            // 3. Privacy & Security Assurance
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = "Security",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Zero Telemetry & Private Processing",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (userSettings.engineMode == EngineMode.LOCAL_RULES) {
                                "100% on-device processing. No network permissions are used and zero data leaves your phone."
                            } else {
                                "Your API keys and text queries are sent directly and securely from your device to your selected provider without any intermediary servers."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Interaction Mode Preferences
            Text(
                text = "Interaction Mode",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Floating Overlay Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Pill Over Other Apps",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (userSettings.overlayEnabled) {
                                    "Floating 36dp pill appears over other apps when typing."
                                } else {
                                    "Quiet mode (SwiftSlate style): Sprout triggers purely via text selection menu (Process Text) and inline commands."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = userSettings.overlayEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasOverlayPermission) {
                                    onRequestOverlayPermission()
                                }
                                onToggleOverlay(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Haptic Feedback Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Haptic Tactile Feedback",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Vibrate lightly upon text replacements and triggers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = userSettings.hapticFeedbackEnabled,
                            onCheckedChange = onToggleHaptics,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Default Preset
                    Column {
                        Text(
                            text = "Default Preset Mode",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preferred tone when opening selection assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PresetChipsRow(
                            selectedPreset = userSettings.defaultPreset,
                            onPresetSelected = onSelectDefaultPreset
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Snippets / Text Expander Section
            Text(
                text = "Text Snippets & Expander",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instant Inline Triggers (Type in any app):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• ?fix — Corrects grammar & spelling immediately\n• ?concise — Trims text to core meaning\n• ?formal — Makes text executive & professional\n• ?punchy — Makes text energetic & active\n• ?calc: 25 * 4 + 10 — Evaluates math inline\n• ?now or ?date — Injects current timestamp\n• ?undo — Reverts last transformation",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Custom Text Snippets (Type ..key in any app):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    userSettings.snippets.forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "..$key",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSnippet(key) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Add Snippet Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSnippetKey,
                            onValueChange = { newSnippetKey = it },
                            placeholder = { Text("key (e.g. email)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newSnippetValue,
                            onValueChange = { newSnippetValue = it },
                            placeholder = { Text("expansion text") },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (newSnippetKey.isNotBlank() && newSnippetValue.isNotBlank()) {
                                    onSaveSnippet(newSnippetKey, newSnippetValue)
                                    newSnippetKey = ""
                                    newSnippetValue = ""
                                }
                            },
                            enabled = newSnippetKey.isNotBlank() && newSnippetValue.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Interactive Test Sandbox
            Text(
                text = "Interactive Test Sandbox",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Test your active engine (${userSettings.engineMode.title}) and presets live below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sandboxText,
                        onValueChange = { sandboxText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("Sample Text") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PresetChipsRow(
                        selectedPreset = sandboxPreset,
                        onPresetSelected = { sandboxPreset = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isSandboxLoading = true
                            scope.launch {
                                val engine = TextEngineProvider.getEngine(userSettings)
                                sandboxResult = engine.transform(TextPayload(text = sandboxText), sandboxPreset)
                                isSandboxLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSandboxLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isSandboxLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Enhance with ${userSettings.engineMode.title}")
                    }

                    sandboxResult?.let { res ->
                        Spacer(modifier = Modifier.height(12.dp))
                        if (res.diffTokens.isNotEmpty()) {
                            DiffViewer(diffTokens = res.diffTokens)
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = res.transformedText.ifBlank { "No changes needed." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = res.summaryNote ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            if (res.wordsSaved > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "-${res.wordsSaved} words",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌱 Sprout ${AppVersion.displayString}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Engineered by VeggieBit Studios • Apache 2.0 License",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
