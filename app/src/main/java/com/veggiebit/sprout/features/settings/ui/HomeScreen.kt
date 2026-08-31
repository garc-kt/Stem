package com.veggiebit.sprout.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veggiebit.sprout.app.theme.SproutLargeIncreasedShape
import com.veggiebit.sprout.core.version.AppVersion
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.ui.components.PresetChipsRow
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import com.veggiebit.sprout.features.settings.ui.components.PermissionStepCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userSettings: SproutUserSettings,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onSelectDefaultPreset: (TransformPreset) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onNavigate: (SproutRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Spa,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sprout",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier.padding(innerPadding)
        ) {
            // Section 1: System Readiness & Permissions (Distilled)
            item {
                if (hasAccessibilityPermission && hasOverlayPermission) {
                    Surface(
                        shape = SproutLargeIncreasedShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Sprout is Active & Ambient",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Ready to enhance text via selection menu, floating pill & ?triggers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                } else {
                    SectionLabel("Permissions Setup")
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!hasAccessibilityPermission) {
                        PermissionStepCard(
                            stepNumber = 1,
                            title = "Accessibility Service",
                            description = "Enables text detection and inline ?commands across apps.",
                            icon = Icons.Rounded.TouchApp,
                            isGranted = false,
                            onGrantClick = onRequestAccessibilityPermission
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (!hasOverlayPermission) {
                        PermissionStepCard(
                            stepNumber = 2,
                            title = "Display Over Other Apps",
                            description = "Enables the floating pill and live thinking HUD capsule.",
                            icon = Icons.Rounded.Layers,
                            isGranted = false,
                            onGrantClick = onRequestOverlayPermission
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section 2: Interaction & Core Settings
            item {
                SectionLabel("Preferences")
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = SproutLargeIncreasedShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = userSettings.overlayEnabled,
                                    role = Role.Switch,
                                    onValueChange = { enabled ->
                                        if (enabled && !hasOverlayPermission) onRequestOverlayPermission()
                                        onToggleOverlay(enabled)
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Floating Indicator",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (userSettings.overlayEnabled) "Floating 36dp pill appears when typing in apps."
                                    else "Quiet mode: Triggers via text selection and ?commands.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = userSettings.overlayEnabled,
                                // Toggling is handled by the Row's toggleable() above so TalkBack
                                // announces "Floating Indicator, switch, on/off" as one unit
                                // instead of a bare unlabeled "Switch" — null here means this
                                // Switch is purely the visual indicator, not a second click target.
                                onCheckedChange = null,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = userSettings.hapticFeedbackEnabled,
                                    role = Role.Switch,
                                    onValueChange = onToggleHaptics
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Haptic Feedback",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vibrates gently on trigger injection and action taps.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = userSettings.hapticFeedbackEnabled,
                                onCheckedChange = null,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            text = "Default Transform Preset",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PresetChipsRow(
                            selectedPreset = userSettings.defaultPreset,
                            onPresetSelected = onSelectDefaultPreset,
                            presets = userSettings.orderedPresets
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section 3: Grouped Material 3 Expressive Navigation Hub
            item {
                SectionLabel("Features & Configuration")
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = SproutLargeIncreasedShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        homeNavItems.forEachIndexed { index, navItem ->
                            GroupedNavRow(
                                navItem = navItem,
                                onClick = { onNavigate(navItem.route) }
                            )
                            if (index < homeNavItems.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HomeNavItem(
    val route: SproutRoute,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val containerColor: @Composable () -> Color,
    val iconColor: @Composable () -> Color
)

private val homeNavItems = listOf(
    HomeNavItem(
        Engine,
        "AI Intelligence Engine",
        "Configure Gemini 3.5+, GPT-5+, Claude 4.5+ or local AI",
        Icons.Rounded.Psychology,
        containerColor = { MaterialTheme.colorScheme.primaryContainer },
        iconColor = { MaterialTheme.colorScheme.onPrimaryContainer }
    ),
    HomeNavItem(
        Snippets,
        "Commands & Snippets",
        "Custom ?triggers, dynamic ?ai: prompts, and text expansion",
        Icons.Rounded.Style,
        containerColor = { MaterialTheme.colorScheme.secondaryContainer },
        iconColor = { MaterialTheme.colorScheme.onSecondaryContainer }
    ),
    HomeNavItem(
        History,
        "Session History",
        "Search and review recent transformations",
        Icons.Rounded.History,
        containerColor = { MaterialTheme.colorScheme.secondaryContainer },
        iconColor = { MaterialTheme.colorScheme.onSecondaryContainer }
    ),
    HomeNavItem(
        Sandbox,
        "Test Sandbox",
        "Interactive preview and preset comparison playground",
        Icons.Rounded.Science,
        containerColor = { MaterialTheme.colorScheme.primaryContainer },
        iconColor = { MaterialTheme.colorScheme.onPrimaryContainer }
    ),
    HomeNavItem(
        AppRules,
        "Per-App Rules",
        "Customize floating pill behavior per installed app",
        Icons.Rounded.Apps,
        containerColor = { MaterialTheme.colorScheme.primaryContainer },
        iconColor = { MaterialTheme.colorScheme.onPrimaryContainer }
    ),
    HomeNavItem(
        Appearance,
        "Appearance & Language",
        "Theme mode and offline rule engine dictionary",
        Icons.Rounded.Palette,
        containerColor = { MaterialTheme.colorScheme.secondaryContainer },
        iconColor = { MaterialTheme.colorScheme.onSecondaryContainer }
    )
)

@Composable
private fun GroupedNavRow(
    navItem: HomeNavItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(navItem.containerColor()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = navItem.icon,
                contentDescription = null,
                tint = navItem.iconColor(),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = navItem.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = navItem.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}
