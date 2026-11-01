package com.alkisstam.taskbar.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.alkisstam.taskbar.MainActivity
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
                } else if (!Settings.canDrawOverlays(this@OverlayTileService)) {
                    // The service would run with no windows and the tile would claim "on".
                    openApp()
                } else {
                    prefsRepository.setOverlayEnabled(true)
                    try {
                        startForegroundService(Intent(this@OverlayTileService, OverlayService::class.java))
                        updateTile(true)
                    } catch (e: Exception) {
                        Log.w("OverlayTileService", "Failed to start overlay service", e)
                        prefsRepository.setOverlayEnabled(false)
                        updateTile(false)
                    }
                }
            }
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun updateTile(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        // No subtitle: System UI already shows a localized On/Off for the tile state.
        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        try {
            tile.updateTile()
        } catch (e: IllegalStateException) {
            Log.w("OverlayTileService", "updateTile failed, tile may be unbound", e)
        }
    }
}
