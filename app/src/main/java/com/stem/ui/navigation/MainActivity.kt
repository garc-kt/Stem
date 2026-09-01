package com.stem.ui.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stem.app.StemApplication
import com.stem.core.models.StemUserSettings
import com.stem.core.util.PermissionHelper
import com.stem.service.StemAccessibilityService
import com.stem.ui.theme.StemTheme



class MainActivity : ComponentActivity() {

    private var hasAccessibilityPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferencesRepo = StemApplication.instance.preferencesRepository
            val settings by preferencesRepo.settingsFlow.collectAsStateWithLifecycle(
                initialValue = StemUserSettings()
            )

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
