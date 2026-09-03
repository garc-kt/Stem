package com.stem.ui.screens

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.R
import com.stem.core.models.StemUserSettings
import com.stem.core.models.TextPayload
import com.stem.core.models.TransformPreset
import com.stem.core.models.TransformResult
import com.stem.engine.DiffCalculator
import com.stem.engine.InlineCommandEngine
import com.stem.engine.LocalRuleEngine
import com.stem.engine.TextEngineProvider
import com.stem.engine.TransformHistory
import com.stem.ui.components.BeforeAfterDiffBlock
import com.stem.ui.components.DiffViewer
import com.stem.ui.components.PresetChipsRow
import com.stem.ui.components.StemButton
import com.stem.ui.components.StemCard
import com.stem.ui.components.StemSectionHeader
import com.stem.ui.components.StemStatusPill
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemPillShape
import com.stem.ui.theme.StemSharpShape
import kotlinx.coroutines.launch

/**
 * Stem Home Screen:
 * Clean, minimalist dashboard featuring:
 * - Unified Status Hero (Pulsing live status, active engine pill, switch)
 * - Interactive Try Stem Sandbox (Instant keystroke testing & live diff)
 * - Compact Recent Activity
 */
@Composable
fun HomeScreen(
    userSettings: StemUserSettings,
    hasAccessibilityPermission: Boolean,
    modifier: Modifier = Modifier,
    recentHistory: List<TransformHistory.Snapshot> = emptyList(),
    onRequestAccessibilityPermission: () -> Unit = {},
    onToggleService: (Boolean) -> Unit = {},
    onNavigateToSnippets: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val stemTheme = LocalStemColors.current
    val isFullyEnabled = userSettings.serviceEnabled && hasAccessibilityPermission

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(stemTheme.bg)
    ) {
        // 1. Unified Hero Status Card (Status + Engine + Master Switch)
        item {
            StemCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StemStatusPill(
                            text = stringResource(if (isFullyEnabled) R.string.home_status_active else R.string.home_status_paused),
                            dotColor = if (isFullyEnabled) stemTheme.add else stemTheme.remove,
                            isPulsing = isFullyEnabled,
                            backgroundColor = stemTheme.surface2
                        )

                        // Active engine badge with navigation affordance
                        StemStatusPill(
                            text = userSettings.engineMode.title,
                            dotColor = if (userSettings.engineMode.isCloud) stemTheme.inkMuted else stemTheme.add,
                            trailingText = "→",
                            onClick = onNavigateToSettings,
                            backgroundColor = stemTheme.surface2
                        )
                    }

                    Switch(
                        checked = userSettings.serviceEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasAccessibilityPermission) onRequestAccessibilityPermission()
                            onToggleService(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = stemTheme.onInk,
                            checkedTrackColor = stemTheme.ink,
                            uncheckedThumbColor = stemTheme.inkMuted,
                            uncheckedTrackColor = stemTheme.surface2,
                            uncheckedBorderColor = stemTheme.borderSubtle
                        )
                    )
                }

                if (!hasAccessibilityPermission) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(StemSharpShape)
                            .background(stemTheme.surface2)
                            .border(1.dp, stemTheme.borderSubtle, StemSharpShape)
                            .clickable(role = Role.Button, onClick = onRequestAccessibilityPermission)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.home_enable_accessibility_button),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = stemTheme.ink
                        )
                        Text(
                            text = stringResource(R.string.action_grant) + " →",
                            style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                            color = stemTheme.ink
                        )
                    }
                }
            }
        }

        // 2. Interactive Transformation Sandbox (Clean & Minimalist)
        item {
            val scope = rememberCoroutineScope()
            val sampleText = stringResource(R.string.home_sandbox_sample_text)
            var inputText by remember { mutableStateOf(sampleText) }
            var selectedPreset by remember(userSettings.defaultPreset) { mutableStateOf(userSettings.defaultPreset) }
            var transformResult by remember { mutableStateOf<TransformResult?>(null) }
            var isRunning by remember { mutableStateOf(false) }

            fun runTransformation(preset: TransformPreset = selectedPreset) {
                if (inputText.isBlank() || isRunning) return
                isRunning = true
                scope.launch {
                    try {
                        val engine = TextEngineProvider.getEngine(userSettings)
                        val payload = TextPayload(inputText)
                        val res = engine.transform(
                            payload = payload,
                            preset = preset,
                            languagePreference = userSettings.languagePreference
                        )
                        transformResult = res
                    } catch (e: Exception) {
                        val res = LocalRuleEngine.transform(
                            payload = TextPayload(inputText),
                            preset = preset,
                            languagePreference = userSettings.languagePreference
                        )
                        transformResult = res
                    } finally {
                        isRunning = false
                    }
                }
            }

            StemCard {
                StemSectionHeader(
                    title = stringResource(R.string.home_sandbox_header),
                    action = {
                        if (inputText != sampleText || transformResult != null) {
                            Text(
                                text = stringResource(R.string.home_sandbox_reset_button),
                                style = StemMonoBadge,
                                color = stemTheme.remove,
                                modifier = Modifier
                                    .clickable(role = Role.Button, onClick = {
                                        inputText = sampleText
                                        transformResult = null
                                    })
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newText ->
                        inputText = newText
                        val cmd = InlineCommandEngine.evaluate(
                            text = newText,
                            nodeHashCode = 0,
                            snippets = userSettings.snippets,
                            customCommands = userSettings.customCommands
                        )
                        when (cmd) {
                            is InlineCommandEngine.CommandResult.Replaced -> {
                                inputText = cmd.newText
                                transformResult = null
                            }
                            is InlineCommandEngine.CommandResult.RunAIPreset -> {
                                selectedPreset = cmd.preset
                                inputText = cmd.body
                                runTransformation(cmd.preset)
                            }
                            else -> {}
                        }
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.home_sandbox_placeholder),
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.inkFaint
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = StemSharpShape,
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = stemTheme.bg,
                        unfocusedContainerColor = stemTheme.bg,
                        focusedBorderColor = stemTheme.ink,
                        unfocusedBorderColor = stemTheme.borderSubtle,
                        cursorColor = stemTheme.ink
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                PresetChipsRow(
                    selectedPreset = selectedPreset,
                    onPresetSelected = { preset ->
                        selectedPreset = preset
                        runTransformation(preset)
                    },
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    StemButton(
                        text = stringResource(R.string.home_sandbox_enhance_button),
                        onClick = { runTransformation(selectedPreset) },
                        enabled = inputText.isNotBlank(),
                        isLoading = isRunning,
                        loadingText = stringResource(R.string.thinking_default_label)
                    )
                }

                transformResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    if (selectedPreset == TransformPreset.SUMMARIZE || selectedPreset == TransformPreset.BULLETIZE || selectedPreset == TransformPreset.EXPAND) {
                        BeforeAfterDiffBlock(
                            beforeText = inputText,
                            afterText = result.transformedText
                        )
                    } else {
                        val tokens = remember(result) {
                            if (result.diffTokens.isNotEmpty()) {
                                result.diffTokens
                            } else {
                                DiffCalculator.calculateDiff(inputText, result.transformedText)
                            }
                        }
                        DiffViewer(diffTokens = tokens)
                    }
                }
            }
        }

        // 3. Recent Activity (Clean & Compact)
        if (recentHistory.isNotEmpty()) {
            item {
                StemSectionHeader(
                    title = stringResource(R.string.home_recent_transforms_header),
                    action = {
                        Text(
                            text = stringResource(R.string.home_see_all_button),
                            style = StemMonoBadge,
                            color = stemTheme.inkMuted,
                            modifier = Modifier
                                .clickable(role = Role.Button, onClick = onNavigateToHistory)
                                .padding(4.dp)
                        )
                    }
                )
            }

            items(recentHistory.take(3), key = { it.id }) { entry ->
                StemCard(contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = entry.presetName.uppercase(),
                            style = StemMonoBadge.copy(fontWeight = FontWeight.Bold),
                            color = stemTheme.ink
                        )
                        Text(
                            text = stringResource(R.string.home_recent_badge),
                            style = StemMonoBadge,
                            color = stemTheme.inkFaint
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.originalText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = stemTheme.remove,
                            textDecoration = TextDecoration.LineThrough
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.replacedText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = stemTheme.add,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2
                    )
                }
            }
        }
    }
}
