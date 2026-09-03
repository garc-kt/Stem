package com.stem.ui.navigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.core.net.toUri
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stem.R
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
            Surface(
                color = stemTheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            color = stemTheme.borderSubtle,
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
                        .height(56.dp)
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
                                    StemLogoMark(size = 24.dp, tint = stemTheme.ink)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = stemTheme.ink
                                    )
                                }
                            }
                            StemTab.SNIPPETS -> {
                                Text(
                                    text = stringResource(R.string.nav_tab_snippets),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = stemTheme.ink
                                )
                            }
                            StemTab.HISTORY -> {
                                Text(
                                    text = stringResource(R.string.nav_tab_history),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = stemTheme.ink
                                )
                            }
                            StemTab.SETTINGS -> {
                                Text(
                                    text = stringResource(R.string.nav_tab_settings),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = stemTheme.ink
                                )
                            }
                        }

                        // Right: Minimalist subtle support button
                        val sponsorContentDescription = stringResource(R.string.nav_sponsor_button)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(stemTheme.surface2)
                                .border(1.dp, stemTheme.borderSubtle, CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/sponsors/garc-kt".toUri())
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                )
                                .clearAndSetSemantics {
                                    contentDescription = sponsorContentDescription
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            GitHubSponsorHeartIcon(color = stemTheme.inkMuted, size = 15.dp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = stemTheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val y = strokeWidth / 2
                        drawLine(
                            color = stemTheme.borderSubtle,
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
                        .height(60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StemTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        val targetColor = if (isSelected) stemTheme.ink else stemTheme.inkMuted
                        val animatedColor by androidx.compose.animation.animateColorAsState(
                            targetValue = targetColor,
                            animationSpec = androidx.compose.animation.core.tween(150),
                            label = "tabColor"
                        )
                        val indicatorAlpha by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = androidx.compose.animation.core.tween(150),
                            label = "tabIndicatorAlpha"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(role = Role.Tab) { currentTab = tab },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            StemTabIcon(tab = tab, color = animatedColor)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = stringResource(tab.titleRes).uppercase(),
                                style = StemMonoBadge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = animatedColor
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 2.dp)
                                    .clip(StemSharpShape)
                                    .background(stemTheme.ink.copy(alpha = indicatorAlpha))
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
                        onSelectLanguagePreference = viewModel::selectLanguagePreference,
                        onSelectDefaultPreset = viewModel::selectDefaultPreset,
                        onSaveTemperature = viewModel::saveTemperature,
                        onSaveOllamaUrl = viewModel::saveOllamaUrl,
                        onSaveOllamaModel = viewModel::saveOllamaModel,
                        onSaveGeminiSettings = viewModel::saveGeminiSettings,
                        onSaveOpenAISettings = viewModel::saveOpenAISettings,
                        onSaveClaudeSettings = viewModel::saveClaudeSettings,
                        onClearGeminiApiKey = viewModel::clearGeminiApiKey,
                        onClearOpenAIApiKey = viewModel::clearOpenAIApiKey,
                        onClearClaudeApiKey = viewModel::clearClaudeApiKey,
                        onSetPackageExcluded = viewModel::setPackageExcluded,
                        onSaveCustomPromptInstruction = viewModel::saveCustomPromptInstruction,
                        onSelectThemeMode = viewModel::selectThemeMode,
                        onToggleHaptics = viewModel::toggleHaptics
                    )
                }
            }
        }
    }
}

