package com.stem.ui.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stem.core.util.PermissionHelper
import com.stem.service.StemAccessibilityService
import com.stem.ui.theme.StemTheme



class MainActivity : ComponentActivity() {

    private var hasAccessibilityPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Shares SettingsViewModel (and its single settingsFlow subscription) with
            // StemNavDisplay below, rather than each independently collecting
            // PreferencesRepository.settingsFlow — every emission there decrypts 3 API keys, so
            // two parallel subscriptions meant doing that work twice on every settings change.
            val viewModel: SettingsViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            StemTheme(themeMode = settings.themeMode) {
                StemNavDisplay(
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onRequestAccessibilityPermission = {
                        PermissionHelper.openAccessibilitySettings(this)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasAccessibilityPermission = PermissionHelper.isAccessibilityServiceEnabled(
            this,
            StemAccessibilityService::class.java
        )
    }
}
