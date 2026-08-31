package com.veggiebit.sprout.features.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.veggiebit.sprout.features.settings.ui.onboarding.OnboardingScreen
import com.veggiebit.sprout.features.settings.ui.sections.AppRulesScreen
import com.veggiebit.sprout.features.settings.ui.sections.AppearanceScreen
import com.veggiebit.sprout.features.settings.ui.sections.EngineScreen
import com.veggiebit.sprout.features.settings.ui.sections.HistoryScreen
import com.veggiebit.sprout.features.settings.ui.sections.SandboxScreen
import com.veggiebit.sprout.features.settings.ui.sections.SnippetsScreen

/**
 * Hosts the settings app's Navigation 3 graph. `androidx.navigation3` was a declared
 * dependency with zero usages before this — the settings UI was a single 1,150-line composable.
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

    if (!isReady) {
        // DataStore's first emission hasn't landed yet — avoid guessing Onboarding vs Home.
        Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize())
        }
        return
    }

    val backStack = rememberNavBackStack(if (userSettings.hasCompletedOnboarding) Home else Onboarding)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryProvider = entryProvider {
            entry<Onboarding> {
                OnboardingScreen(
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onFinish = {
                        viewModel.completeOnboarding()
                        backStack.clear()
                        backStack.add(Home)
                    }
                )
            }
            entry<Home> {
                HomeScreen(
                    userSettings = userSettings,
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                    onToggleOverlay = viewModel::toggleOverlay,
                    onSelectDefaultPreset = viewModel::selectDefaultPreset,
                    onToggleHaptics = viewModel::toggleHaptics,
                    onNavigate = { route -> backStack.add(route) }
                )
            }
            entry<Engine> {
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
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<Appearance> {
                AppearanceScreen(
                    themeMode = userSettings.themeMode,
                    languagePreference = userSettings.languagePreference,
                    onSelectThemeMode = viewModel::selectThemeMode,
                    onSelectLanguagePreference = viewModel::selectLanguagePreference,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<AppRules> {
                AppRulesScreen(
                    appRules = userSettings.appRules,
                    onSetAppRule = viewModel::setAppRule,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<Snippets> {
                SnippetsScreen(
                    snippets = userSettings.snippets,
                    customCommands = userSettings.customCommands,
                    onSaveSnippet = viewModel::saveSnippet,
                    onDeleteSnippet = viewModel::deleteSnippet,
                    onSaveCustomCommand = viewModel::saveCustomCommand,
                    onDeleteCustomCommand = viewModel::deleteCustomCommand,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<History> {
                HistoryScreen(
                    history = history,
                    onCopy = { text -> clipboardManager.setText(AnnotatedString(text)) },
                    onClearHistory = viewModel::clearHistory,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<Sandbox> {
                SandboxScreen(
                    userSettings = userSettings,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
