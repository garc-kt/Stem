package com.veggiebit.sprout.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.veggiebit.sprout.app.theme.LocalStemColors
import com.veggiebit.sprout.app.theme.StemLogoMark
import com.veggiebit.sprout.app.theme.StemMonoBadge
import com.veggiebit.sprout.app.theme.StemTab
import com.veggiebit.sprout.app.theme.StemTabIcon
import com.veggiebit.sprout.features.settings.ui.onboarding.OnboardingScreen
import com.veggiebit.sprout.features.settings.ui.sections.EngineScreen
import com.veggiebit.sprout.features.settings.ui.sections.HistoryScreen
import com.veggiebit.sprout.features.settings.ui.sections.SnippetsScreen

/**
 * Hosts the Stem Main App with 4 Navigation Tabs:
 * - Home
 * - Snippets
 * - History
 * - Settings
 * Matches Stem.dc.html design specification.
 */
@Composable
fun SproutNavDisplay(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel()
    val userSettings by viewModel.settings.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val stemTheme = LocalStemColors.current

    if (!isReady) {
        Surface(color = stemTheme.bg, modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize())
        }
        return
    }

    if (!userSettings.hasCompletedOnboarding) {
        OnboardingScreen(
            hasAccessibilityPermission = hasAccessibilityPermission,
            hasOverlayPermission = hasOverlayPermission,
            onRequestAccessibilityPermission = onRequestAccessibilityPermission,
            onRequestOverlayPermission = onRequestOverlayPermission,
            onFinish = {
                viewModel.completeOnboarding()
            },
            modifier = modifier
        )
        return
    }

    var currentTab by remember { mutableStateOf(StemTab.HOME) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Stem TopBar with Status Bar Insets Protection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(stemTheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .border(width = 1.dp, color = stemTheme.border)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        when (currentTab) {
                            StemTab.HOME -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StemLogoMark(size = 28.dp, tint = stemTheme.ink)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Stem",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = stemTheme.ink
                                        )
                                        Text(
                                            text = "ambient writing",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = stemTheme.inkMuted
                                        )
                                    }
                                }
                            }
                            StemTab.SNIPPETS -> {
                                Column {
                                    Text(
                                        text = "Snippets",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = stemTheme.ink
                                    )
                                    Text(
                                        text = "auto-expansions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = stemTheme.inkMuted
                                    )
                                }
                            }
                            StemTab.HISTORY -> {
                                Column {
                                    Text(
                                        text = "History",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = stemTheme.ink
                                    )
                                    Text(
                                        text = "recent transforms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = stemTheme.inkMuted
                                    )
                                }
                            }
                            StemTab.SETTINGS -> {
                                Column {
                                    Text(
                                        text = "Settings",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = stemTheme.ink
                                    )
                                    Text(
                                        text = "models & rules",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = stemTheme.inkMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Stem BottomBar with Navigation Bar Insets Protection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(stemTheme.surface)
                    .border(width = 1.dp, color = stemTheme.border)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StemTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        val color = if (isSelected) stemTheme.ink else stemTheme.inkMuted

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(role = Role.Tab) { currentTab = tab },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            StemTabIcon(tab = tab, color = color)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tab.title.uppercase(),
                                style = StemMonoBadge,
                                color = color
                            )
                        }
                    }
                }
            }
        },
        containerColor = stemTheme.bg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                StemTab.HOME -> {
                    HomeScreen(
                        userSettings = userSettings,
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        recentHistory = history,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                        onToggleOverlay = viewModel::toggleOverlay,
                        onSelectDefaultPreset = viewModel::selectDefaultPreset,
                        onNavigateToHistory = { currentTab = StemTab.HISTORY }
                    )
                }
                StemTab.SNIPPETS -> {
                    SnippetsScreen(
                        snippets = userSettings.snippets,
                        customCommands = userSettings.customCommands,
                        onSaveSnippet = viewModel::saveSnippet,
                        onDeleteSnippet = viewModel::deleteSnippet,
                        onSaveCustomCommand = viewModel::saveCustomCommand,
                        onDeleteCustomCommand = viewModel::deleteCustomCommand
                    )
                }
                StemTab.HISTORY -> {
                    HistoryScreen(
                        history = history,
                        onCopy = { text -> clipboardManager.setText(AnnotatedString(text)) },
                        onClearHistory = viewModel::clearHistory
                    )
                }
                StemTab.SETTINGS -> {
                    EngineScreen(
                        userSettings = userSettings,
                        onSelectEngineMode = viewModel::selectEngineMode,
                        onSaveTemperature = viewModel::saveTemperature,
                        onSaveOllamaUrl = viewModel::saveOllamaUrl,
                        onSaveOllamaModel = viewModel::saveOllamaModel,
                        onSaveGeminiSettings = viewModel::saveGeminiSettings,
                        onSaveOpenAISettings = viewModel::saveOpenAISettings,
                        onSaveClaudeSettings = viewModel::saveClaudeSettings,
                        onSaveCustomPromptInstruction = viewModel::saveCustomPromptInstruction,
                        onSelectThemeMode = viewModel::selectThemeMode,
                        onToggleHaptics = viewModel::toggleHaptics
                    )
                }
            }
        }
    }
}
