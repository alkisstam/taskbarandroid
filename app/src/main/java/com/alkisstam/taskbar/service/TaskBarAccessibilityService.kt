package com.alkisstam.taskbar.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
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
        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
        scope.launch {
            try {
                val overlayEnabled = prefsRepository.overlayEnabled.first()
                if (overlayEnabled && Settings.canDrawOverlays(this@TaskBarAccessibilityService)) {
                    val intent = Intent(this@TaskBarAccessibilityService, OverlayService::class.java)
                    startForegroundService(intent)
                }
            } catch (e: Exception) {
                Log.w("TaskBarAccessibilityService", "Failed to start overlay service", e)
            }
        }
        sendBroadcast(
            Intent(OverlayService.ACTION_ACCESSIBILITY_CHANGED).setPackage(packageName)
        )
    }

    fun expandNotifications() { performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) }
    fun expandQuickSettings() { performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) }

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

    // Refreshed on a TTL instead of computed once: the default launcher can change while
    // the service runs, and an early-boot query can come back empty — either way a stale
    // set silently breaks dismiss-on-home for the rest of the service's life.
    private var launcherPackages: Set<String> = emptySet()
    private var launcherPackagesFetchedAt = 0L

    private fun launcherPackages(): Set<String> {
        val now = android.os.SystemClock.elapsedRealtime()
        if (launcherPackages.isEmpty() || now - launcherPackagesFetchedAt > 60_000) {
            launcherPackages = try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    .mapNotNull { it.activityInfo?.packageName }
                    .toSet()
            } catch (e: Exception) {
                Log.w("TaskBarAccessibilityService", "Failed to query launcher packages", e)
                emptySet()
            }
            launcherPackagesFetchedAt = now
        }
        return launcherPackages
    }

    private var lastForegroundPackage: String? = null
    private var onHome = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            // OneUI (and other launchers) refire WINDOW_STATE_CHANGED for the launcher package
            // while already in the foreground (icon layout/widget refresh etc). Only dismiss on
            // an actual transition into the launcher, or the pill triggers this while sitting on
            // the home screen and the dock collapses itself right after showing.
            val enteredFromElsewhere = pkg != lastForegroundPackage
            lastForegroundPackage = pkg
            val isLauncher = pkg in launcherPackages()
            if (isLauncher && enteredFromElsewhere) {
                sendBroadcast(
                    Intent(OverlayService.ACTION_DISMISS_ALL).setPackage(packageName)
                )
            }
            // Only an activity coming to the front leaves the home screen. The shade, keyboard,
            // our own overlays, widgets and launcher side pages (Discover, Samsung Free) all fire
            // window events over the home screen without leaving it.
            val home = if (isLauncher) true
                else if (pkg == packageName || pkg == "com.android.systemui" || pkg == currentImePackage()) onHome
                else if (!isActivity(pkg, event.className?.toString())) onHome
                else false
            if (home != onHome) {
                onHome = home
                sendBroadcast(
                    Intent(OverlayService.ACTION_HOME_STATE).setPackage(packageName)
                        .putExtra(OverlayService.EXTRA_ON_HOME, home)
                )
            }
        }
    }

    private val activityClassCache = HashMap<String, Boolean>()

    private fun isActivity(pkg: String, className: String?): Boolean {
        if (className == null) return false
        val key = "$pkg/$className"
        return activityClassCache.getOrPut(key) {
            try {
                packageManager.getActivityInfo(ComponentName(pkg, className), 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun currentImePackage(): String? =
        android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            if (OverlayService.isTaskbarVisibleForBack) {
                sendBroadcast(Intent(OverlayService.ACTION_DISMISS_ALL).setPackage(packageName))
                return true
            }
        }
        return false
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
