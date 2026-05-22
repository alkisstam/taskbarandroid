package com.alkisstam.taskbar.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        sendBroadcast(
            Intent(OverlayService.ACTION_ACCESSIBILITY_CHANGED).setPackage(packageName)
        )
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

    private val launcherPackages: Set<String> by lazy {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg in launcherPackages) {
                sendBroadcast(
                    Intent(OverlayService.ACTION_DISMISS_ALL).setPackage(packageName)
                )
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        scope.cancel()
        sendBroadcast(
            Intent(OverlayService.ACTION_ACCESSIBILITY_CHANGED).setPackage(packageName)
        )
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: TaskBarAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
