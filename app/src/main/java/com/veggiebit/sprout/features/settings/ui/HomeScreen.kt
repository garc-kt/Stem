package com.veggiebit.sprout.features.settings.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veggiebit.sprout.app.theme.SproutLargeIncreasedShape
import com.veggiebit.sprout.core.version.AppVersion
import com.veggiebit.sprout.features.enhancement.data.models.TransformPreset
import com.veggiebit.sprout.features.enhancement.ui.components.PresetChipsRow
import com.veggiebit.sprout.features.settings.data.SproutUserSettings
import com.veggiebit.sprout.features.settings.ui.components.PermissionStepCard

// LargeFlexibleTopAppBar (with its dedicated subtitle slot) is part of the M3 Expressive
// surface that's internal in this project's resolved material3:1.4.0, so this uses a plain
// stable TopAppBar with the subtitle stacked under the title instead.
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
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
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
                            Text("Sprout", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "VeggieBit Studios • ${AppVersion.displayString}",
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier.padding(innerPadding)
        ) {
            item {
                SectionLabel("Setup & Permissions")
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
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                SectionLabel("Interaction Mode")
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = SproutLargeIncreasedShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    text = if (userSettings.overlayEnabled) "Floating 36dp pill appears over other apps when typing."
                                    else "Quiet mode: Sprout triggers via text selection and inline commands.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = userSettings.overlayEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !hasOverlayPermission) onRequestOverlayPermission()
                                    onToggleOverlay(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Haptic Feedback",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = userSettings.hapticFeedbackEnabled,
                                onCheckedChange = onToggleHaptics,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Default Preset",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
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

            item {
                SectionLabel("More")
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(homeNavItems) { navItem ->
                NavCard(navItem = navItem, onClick = { onNavigate(navItem.route) })
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
}

private data class HomeNavItem(val route: SproutRoute, val title: String, val description: String, val icon: ImageVector)

private val homeNavItems = listOf(
    HomeNavItem(Engine, "AI Intelligence Engine", "Choose and configure the transformation engine", Icons.Rounded.Psychology),
    HomeNavItem(Appearance, "Appearance", "Theme mode and offline language", Icons.Rounded.Palette),
    HomeNavItem(AppRules, "Per-App Rules", "Control which apps show the floating pill", Icons.Rounded.Apps),
    HomeNavItem(Snippets, "Snippets & Triggers", "Inline commands and custom text expansions", Icons.Rounded.Style),
    HomeNavItem(History, "Session History", "This session's transformations", Icons.Rounded.History),
    HomeNavItem(Sandbox, "Test Sandbox", "Try presets and engines on sample text", Icons.Rounded.Science)
)

@Composable
private fun NavCard(navItem: HomeNavItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = navItem.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(navItem.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(navItem.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}
