package com.stem.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.stem.app.StemApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch



class StemTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val repo = StemApplication.instance.preferencesRepository
            val currentSettings = repo.settingsFlow.first()
            val newEnabled = !currentSettings.overlayEnabled
            repo.setOverlayEnabled(newEnabled)
            updateTileState(newEnabled)
        }
    }

    private fun updateTileState(forcedState: Boolean? = null) {
        serviceScope.launch {
            val tile = qsTile ?: return@launch
            val isEnabled = forcedState ?: StemApplication.instance.preferencesRepository.settingsFlow.first().overlayEnabled

            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "Stem Assistant"
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
