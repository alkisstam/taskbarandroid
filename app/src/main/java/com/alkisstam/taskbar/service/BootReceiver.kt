package com.alkisstam.taskbar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.alkisstam.taskbar.data.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prefsRepository: PreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val overlayEnabled = prefsRepository.overlayEnabled.first()
                if (overlayEnabled && Settings.canDrawOverlays(context)) {
                    val serviceIntent = Intent(context, OverlayService::class.java)
                    context.startForegroundService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.w("BootReceiver", "Failed to start overlay service on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
