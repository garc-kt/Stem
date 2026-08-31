package com.veggiebit.sprout.features.overlay.service

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.veggiebit.sprout.R
import com.veggiebit.sprout.app.SproutApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile allowing instant toggle of Sprout floating overlay.
 */
class SproutTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val repo = SproutApplication.instance.preferencesRepository
            val currentSettings = repo.settingsFlow.first()
            val newEnabled = !currentSettings.overlayEnabled
            repo.setOverlayEnabled(newEnabled)
            updateTileState(newEnabled)
        }
    }

    private fun updateTileState(forcedState: Boolean? = null) {
        serviceScope.launch {
            val tile = qsTile ?: return@launch
            val isEnabled = forcedState ?: SproutApplication.instance.preferencesRepository.settingsFlow.first().overlayEnabled

            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Sprout Assistant"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isEnabled) "Active" else "Paused"
            }
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
