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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemCardShape
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemSharpShape
import com.stem.engine.TransformHistory
import com.stem.core.models.EngineMode
import com.stem.core.models.StemUserSettings



/**
 * Stem Home Screen:
 * - Clean status card (Active / Paused)
 * - Active AI Engine summary card
 * - Recent Transformations list
 */
@Composable
fun HomeScreen(
    userSettings: StemUserSettings,
    hasAccessibilityPermission: Boolean,
    recentHistory: List<TransformHistory.Snapshot> = emptyList(),
    onRequestAccessibilityPermission: () -> Unit = {},
    onToggleService: (Boolean) -> Unit = {},
    onNavigateToSnippets: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
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
        // 1. Stem Active / Paused Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .padding(18.dp)
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
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (isFullyEnabled) stemTheme.add else stemTheme.remove)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isFullyEnabled) "Stem is active" else "Stem is paused",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = stemTheme.ink
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
                                uncheckedTrackColor = stemTheme.surface2
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isFullyEnabled) "Inline text assistant is ready across all apps."
                        else "Enable accessibility service to start transforming text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = stemTheme.inkMuted
                    )

                    if (!hasAccessibilityPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(StemSharpShape)
                                .background(stemTheme.ink)
                                .clickable(role = Role.Button, onClick = onRequestAccessibilityPermission)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Enable Accessibility",
                                style = StemMonoBadge,
                                color = stemTheme.onInk
                            )
                        }
                    }
                }
            }
        }

        // 2. Active AI Provider Overview Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StemCardShape)
                    .background(stemTheme.surface)
                    .border(1.dp, stemTheme.border, StemCardShape)
                    .clickable(role = Role.Button, onClick = onNavigateToSettings)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ACTIVE ENGINE",
                            style = StemMonoBadge,
                            color = stemTheme.inkFaint
                        )

                        Box(
                            modifier = Modifier
                                .clip(StemSharpShape)
                                .background(stemTheme.surface2)
                                .border(1.dp, stemTheme.border, StemSharpShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (userSettings.engineMode) {
                                    EngineMode.LOCAL_RULES -> "LOCAL"
                                    EngineMode.OLLAMA_AI -> "LAN"
                                    else -> "CLOUD"
                                },
                                style = StemMonoBadge,
                                color = stemTheme.ink
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (userSettings.engineMode) {
                            EngineMode.LOCAL_RULES -> "On-Device Rule Engine (Instant)"
                            EngineMode.OLLAMA_AI -> "Ollama Local AI (${userSettings.ollamaModel})"
                            EngineMode.GEMINI_AI -> "Google Gemini (${userSettings.geminiModel})"
                            EngineMode.OPENAI_COMPATIBLE -> "OpenAI Compatible (${userSettings.openaiModel})"
                            EngineMode.CLAUDE_AI -> "Anthropic Claude (${userSettings.claudeModel})"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = stemTheme.ink
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Tap to configure model keys and rules in Settings →",
                        style = MaterialTheme.typography.bodySmall,
                        color = stemTheme.inkMuted
                    )
                }
            }
        }

        // 3. Quick Action Tiles (Snippets & History)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(StemCardShape)
                        .background(stemTheme.surface)
                        .border(1.dp, stemTheme.border, StemCardShape)
                        .clickable(role = Role.Button, onClick = onNavigateToSnippets)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "SNIPPETS",
                            style = StemMonoBadge,
                            color = stemTheme.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Inline math, date, & custom triggers →",
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.inkMuted,
                            lineHeight = 16.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(StemCardShape)
                        .background(stemTheme.surface)
                        .border(1.dp, stemTheme.border, StemCardShape)
                        .clickable(role = Role.Button, onClick = onNavigateToHistory)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "HISTORY",
                            style = StemMonoBadge,
                            color = stemTheme.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Visual diffs and undo journal →",
                            style = MaterialTheme.typography.bodySmall,
                            color = stemTheme.inkMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 4. Recent Transformations Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RECENT TRANSFORMS",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )

                Text(
                    text = "See all →",
                    style = StemMonoBadge,
                    color = stemTheme.inkMuted,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onNavigateToHistory)
                        .padding(4.dp)
                )
            }
        }

        if (recentHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StemCardShape)
                        .background(stemTheme.surface)
                        .border(1.dp, stemTheme.border, StemCardShape)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "No recent transformations yet. Type in any app (e.g. text + ?fix or ?concise) to see results here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = stemTheme.inkMuted
                    )
                }
            }
        } else {
            items(recentHistory.take(4), key = { it.id }) { entry ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StemCardShape)
                        .background(stemTheme.surface)
                        .border(1.dp, stemTheme.border, StemCardShape)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatHistoryCommand(entry.presetName),
                                style = StemMonoBadge,
                                color = stemTheme.ink
                            )
                            Text(
                                text = "recent",
                                style = StemMonoBadge,
                                color = stemTheme.inkFaint
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = entry.originalText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = stemTheme.remove,
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.replacedText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = stemTheme.add,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

