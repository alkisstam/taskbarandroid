package com.alkisstam.taskbar.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.alkisstam.taskbar.data.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskBarAccessibilityService : AccessibilityService() {

    @Inject lateinit var prefsRepository: PreferencesRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val accessibilityWindowManager: WindowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onServiceConnected() {
        instance = this
        scope.launch {
            val overlayEnabled = prefsRepository.overlayEnabled.first()
            if (overlayEnabled && Settings.canDrawOverlays(this@TaskBarAccessibilityService)) {
                val intent = Intent(this@TaskBarAccessibilityService, OverlayService::class.java)
                startForegroundService(intent)
            }
        }
    }

    fun showPowerMenu() {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    fun takeScreenshot(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    fun lockScreen(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: TaskBarAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
