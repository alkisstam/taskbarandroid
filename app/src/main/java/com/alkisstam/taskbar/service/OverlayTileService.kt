package com.alkisstam.taskbar.service

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.data.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class OverlayTileService : TileService() {

    @Inject lateinit var prefsRepository: PreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val toggleMutex = Mutex()

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val enabled = prefsRepository.overlayEnabled.first()
            updateTile(enabled)
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            toggleMutex.withLock {
                val currentlyEnabled = prefsRepository.overlayEnabled.first()
                if (currentlyEnabled) {
                    prefsRepository.setOverlayEnabled(false)
                    stopService(Intent(this@OverlayTileService, OverlayService::class.java))
                    updateTile(false)
                } else {
                    prefsRepository.setOverlayEnabled(true)
                    startForegroundService(Intent(this@OverlayTileService, OverlayService::class.java))
                    updateTile(true)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun updateTile(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Floating Dock"
        tile.subtitle = if (enabled) "On" else "Off"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.updateTile()
    }
}
