package com.veggiebit.sprout.features.settings.ui

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veggiebit.sprout.app.theme.LocalStemColors
import com.veggiebit.sprout.app.theme.StemCardShape
import com.veggiebit.sprout.app.theme.StemGeometricIcon
import com.veggiebit.sprout.app.theme.StemMonoBadge
import com.veggiebit.sprout.app.theme.StemSharpShape
import com.veggiebit.sprout.features.enhancement.data.engine.TextEngineProvider
import com.veggiebit.sprout.features.enhancement.data.engine.TransformHistory
import com.veggiebit.sprout.features.enhancement.data.models.TextPayload
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.data.models.TransformResult
import com.veggiebit.sprout.features.enhancement.ui.components.BeforeAfterDiffBlock
import com.veggiebit.sprout.features.enhancement.ui.components.DiffViewer
import com.veggiebit.sprout.features.enhancement.ui.components.PresetChipsRow
import com.veggiebit.sprout.features.settings.data.SproutUserSettings

/**
 * Stem Home Screen:
 * - Stem is active card with switch & status indicator
 * - Quick Presets horizontal row
 * - "Try it" live interactive transformation sandbox
 * - Recent Transformations list
 * Matches Stem.dc.html design specification.
 */
@Composable
fun HomeScreen(
    userSettings: SproutUserSettings,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    recentHistory: List<TransformHistory.Snapshot> = emptyList(),
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onSelectDefaultPreset: (TransformPreset) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stemTheme = LocalStemColors.current
    val isFullyEnabled = userSettings.overlayEnabled && hasOverlayPermission && hasAccessibilityPermission

    var sandboxText by remember { mutableStateOf("the team are meeting at 3pm to disscuss the quarterly resuts") }
    var sandboxPreset by remember { mutableStateOf(TransformPreset.FIX) }
    var sandboxResult by remember { mutableStateOf<TransformResult?>(null) }

    LaunchedEffect(sandboxText, sandboxPreset, userSettings.engineMode) {
        val payload = TextPayload(text = sandboxText)
        val engine = TextEngineProvider.getEngine(userSettings)
        sandboxResult = engine.transform(payload, sandboxPreset)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
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
                    .padding(16.dp)
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
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isFullyEnabled) stemTheme.add else stemTheme.remove)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFullyEnabled) "Stem is active" else "Stem is paused",
                                style = MaterialTheme.typography.titleMedium,
                                color = stemTheme.ink
                            )
                        }

                        Switch(
                            checked = userSettings.overlayEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasOverlayPermission) onRequestOverlayPermission()
                                if (enabled && !hasAccessibilityPermission) onRequestAccessibilityPermission()
                                onToggleOverlay(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = stemTheme.onInk,
                                checkedTrackColor = stemTheme.ink,
                                uncheckedThumbColor = stemTheme.inkMuted,
                                uncheckedTrackColor = stemTheme.surface2
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isFullyEnabled) "Floating helper enabled · auto-replace ready"
                        else "Enable permissions to activate floating helper",
                        style = MaterialTheme.typography.bodySmall,
                        color = stemTheme.inkMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Tap the 40px icon that floats near any active text field to rewrite instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = stemTheme.inkMuted
                    )

                    if (!hasAccessibilityPermission || !hasOverlayPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!hasAccessibilityPermission) {
                                Box(
                                    modifier = Modifier
                                        .clip(StemSharpShape)
                                        .background(stemTheme.surface2)
                                        .border(1.dp, stemTheme.border, StemSharpShape)
                                        .clickable(role = Role.Button, onClick = onRequestAccessibilityPermission)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Enable Accessibility",
                                        style = StemMonoBadge,
                                        color = stemTheme.ink
                                    )
                                }
                            }
                            if (!hasOverlayPermission) {
                                Box(
                                    modifier = Modifier
                                        .clip(StemSharpShape)
                                        .background(stemTheme.surface2)
                                        .border(1.dp, stemTheme.border, StemSharpShape)
                                        .clickable(role = Role.Button, onClick = onRequestOverlayPermission)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Allow Overlay",
                                        style = StemMonoBadge,
                                        color = stemTheme.ink
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Presets Row
        item {
            Column {
                Text(
                    text = "QUICK PRESETS",
                    style = StemMonoBadge,
                    color = stemTheme.inkFaint
                )

                Spacer(modifier = Modifier.height(8.dp))

                PresetChipsRow(
                    selectedPreset = userSettings.defaultPreset,
                    onPresetSelected = onSelectDefaultPreset,
                    presets = TransformPreset.entries
                )
            }
        }

        // 3. Try It Sandbox Card
        item {
            Column {
                Text(
                    text = "TRY IT",
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
                        .padding(16.dp)
                ) {
                    Column {
                        OutlinedTextField(
                            value = sandboxText,
                            onValueChange = { sandboxText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = stemTheme.ink),
                            shape = StemSharpShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = stemTheme.bg,
                                unfocusedContainerColor = stemTheme.bg,
                                focusedBorderColor = stemTheme.ink,
                                unfocusedBorderColor = stemTheme.border,
                                cursorColor = stemTheme.ink
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PresetChipsRow(
                            selectedPreset = sandboxPreset,
                            onPresetSelected = { sandboxPreset = it },
                            compact = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        sandboxResult?.let { res ->
                            if (sandboxPreset.useDiff && res.diffTokens.isNotEmpty()) {
                                DiffViewer(diffTokens = res.diffTokens)
                            } else {
                                BeforeAfterDiffBlock(
                                    beforeText = res.originalText,
                                    afterText = res.transformedText
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Recent Transformations Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RECENT",
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
                        .padding(14.dp)
                ) {
                    Text(
                        text = "No recent transformations yet. Type in any app to see results here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = stemTheme.inkMuted
                    )
                }
            }
        } else {
            items(recentHistory.take(3), key = { it.id }) { entry ->
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
                                text = entry.presetName.uppercase(),
                                style = StemMonoBadge,
                                color = stemTheme.inkMuted
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
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
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

