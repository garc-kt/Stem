package com.stem.ui.navigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stem.ui.theme.GitHubSponsorHeartIcon
import com.stem.ui.theme.KoFiIcon
import com.stem.ui.theme.LocalStemColors
import com.stem.ui.theme.StemLogoMark
import com.stem.ui.theme.StemMonoBadge
import com.stem.ui.theme.StemSharpShape
import com.stem.ui.theme.StemTab
import com.stem.ui.theme.StemTabIcon
import com.stem.ui.screens.HomeScreen
import com.stem.ui.screens.OnboardingScreen
import com.stem.ui.screens.EngineScreen
import com.stem.ui.screens.HistoryScreen
import com.stem.ui.screens.SnippetsScreen



/**
 * Hosts the Stem Main App with 4 Navigation Tabs:
 * - Home
 * - Snippets
 * - History
 * - Settings
 */
@Composable
fun StemNavDisplay(
    hasAccessibilityPermission: Boolean,
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
            onRequestAccessibilityPermission = onRequestAccessibilityPermission,
            onFinish = {
                viewModel.completeOnboarding()
            },
            modifier = modifier
        )
        return
    }

    var currentTab by rememberSaveable { mutableStateOf(StemTab.HOME) }

    // Without this, back from any non-Home tab exits the app instead of returning Home — the
    // expected behavior for a flat, tab-based (non-back-stack) navigation shell like this one.
    BackHandler(enabled = currentTab != StemTab.HOME) {
        currentTab = StemTab.HOME
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Clean TopBar with Status Bar Insets Protection
            Surface(
                color = stemTheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            color = stemTheme.border,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(58.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Current Tab Title
                        when (currentTab) {
                            StemTab.HOME -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StemLogoMark(size = 26.dp, tint = stemTheme.ink)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Stem",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = stemTheme.ink
                                    )
                                }
                            }
                            StemTab.SNIPPETS -> {
                                Text(
                                    text = "Snippets",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = stemTheme.ink
                                )
                            }
                            StemTab.HISTORY -> {
                                Text(
                                    text = "History",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = stemTheme.ink
                                )
                            }
                            StemTab.SETTINGS -> {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = stemTheme.ink
                                )
                            }
                        }

                        // Right: Top-Right Sponsor Icons & Direct Links
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Ko-fi direct button
                            Box(
                                modifier = Modifier
                                    .clip(StemSharpShape)
                                    .background(stemTheme.surface2)
                                    .border(1.dp, stemTheme.border, StemSharpShape)
                                    .clickable(role = Role.Button) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/X5R825DY4X"))
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 7.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    KoFiIcon(color = Color(0xFF72A4F2), size = 14.dp)
                                    Text(
                                        text = "KO-FI",
                                        style = StemMonoBadge,
                                        color = stemTheme.ink
                                    )
                                }
                            }

                            // GitHub Sponsors direct button
                            Box(
                                modifier = Modifier
                                    .clip(StemSharpShape)
                                    .background(stemTheme.surface2)
                                    .border(1.dp, stemTheme.border, StemSharpShape)
                                    .clickable(role = Role.Button) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/garc-kt"))
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 7.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    GitHubSponsorHeartIcon(color = Color(0xFFEA4AAA), size = 14.dp)
                                    Text(
                                        text = "SPONSOR",
                                        style = StemMonoBadge,
                                        color = stemTheme.ink
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Fixed BottomBar elevated above Android navigation bar
            Surface(
                color = stemTheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val y = strokeWidth / 2
                        drawLine(
                            color = stemTheme.border,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
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
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        recentHistory = history,
                        onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                        onToggleService = viewModel::toggleServiceEnabled,
                        onNavigateToSnippets = { currentTab = StemTab.SNIPPETS },
                        onNavigateToHistory = { currentTab = StemTab.HISTORY },
                        onNavigateToSettings = { currentTab = StemTab.SETTINGS }
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
                        onClearGeminiApiKey = viewModel::clearGeminiApiKey,
                        onClearOpenAIApiKey = viewModel::clearOpenAIApiKey,
                        onClearClaudeApiKey = viewModel::clearClaudeApiKey,
                        onSaveCustomPromptInstruction = viewModel::saveCustomPromptInstruction,
                        onSelectThemeMode = viewModel::selectThemeMode,
                        onToggleHaptics = viewModel::toggleHaptics
                    )
                }
            }
        }
    }
}

