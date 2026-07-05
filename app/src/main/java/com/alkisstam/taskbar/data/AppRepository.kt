package com.alkisstam.taskbar.data

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.alkisstam.taskbar.service.TaskBarAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            loadApps()
        }
    }

    init {
        loadApps()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(packageReceiver, filter)
        }
    }

    private fun loadApps(retriesLeft: Int = 3) {
        scope.launch {
            try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                _apps.value = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                    .mapNotNull { resolveInfo ->
                        val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                        // Load the icon separately so a broken icon doesn't drop the whole app
                        // from the list: MIUI's IconCustomizer inflates huge bitmaps and throws
                        // OutOfMemoryError (an Error, hence Throwable) on some devices. Keep the
                        // app with a null icon (UI renders a placeholder) instead of hiding it.
                        val icon = try {
                            // toBitmap() can return the system's cached Bitmap instance without
                            // copying (androidx shortcut when config already matches); that shared
                            // bitmap can later be recycled by the OS, so copy it to own our instance.
                            resolveInfo.loadIcon(pm).toBitmap().copy(Bitmap.Config.ARGB_8888, false)
                        } catch (e: Throwable) {
                            Log.w("AppRepository", "Icon load failed, keeping app without icon: ${activityInfo.packageName}", e)
                            null
                        }
                        AppInfo(
                            packageName = activityInfo.packageName,
                            label = resolveInfo.loadLabel(pm).toString(),
                            icon = icon
                        )
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            } catch (e: Exception) {
                Log.w("AppRepository", "Failed to load installed apps", e)
                // queryIntentActivities can fail with a transient binder error
                // (DeadObjectException / binder buffer overflow). If we have no apps
                // yet, back off and retry so the dock isn't left empty until the next
                // package broadcast.
                if (_apps.value.isEmpty() && retriesLeft > 0) {
                    delay(1000L * (4 - retriesLeft))
                    loadApps(retriesLeft - 1)
                }
            }
        }
    }

    fun getLaunchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

    /**
     * Launch an app in a given windowing mode.
     *
     * - [LaunchMode.NORMAL] — regular fullscreen launch.
     * - [LaunchMode.FLOATING] — request a freeform/floating window via
     *   [ActivityOptions.setLaunchBounds]. Only takes effect on devices where
     *   freeform windowing is enabled (Samsung DeX, some OEM ROMs, or the
     *   "Enable freeform windows" developer option); otherwise the system
     *   ignores the bounds and the app opens fullscreen.
     * - [LaunchMode.SPLIT_SCREEN] — toggle split-screen on the current
     *   foreground app via the accessibility service, then launch this app
     *   into the adjacent pane. Requires the accessibility service to be on.
     */
    fun launchApp(packageName: String, mode: LaunchMode = LaunchMode.NORMAL) {
        when (mode) {
            LaunchMode.NORMAL -> launchNormal(packageName)
            LaunchMode.FLOATING -> launchFloating(packageName)
            LaunchMode.SPLIT_SCREEN -> launchSplitScreen(packageName)
        }
    }

    private fun launchNormal(packageName: String) {
        val intent = getLaunchIntent(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("AppRepository", "Failed to launch $packageName", e)
        }
    }

    private fun launchFloating(packageName: String) {
        val intent = getLaunchIntent(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val metrics = context.resources.displayMetrics
        val w = (metrics.widthPixels * 0.6f).toInt()
        val h = (metrics.heightPixels * 0.6f).toInt()
        val left = (metrics.widthPixels - w) / 2
        val top = (metrics.heightPixels - h) / 2
        val options = ActivityOptions.makeBasic().apply {
            launchBounds = Rect(left, top, left + w, top + h)
        }
        try {
            context.startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            Log.w("AppRepository", "Failed to launch $packageName in floating window", e)
        }
    }

    private fun launchSplitScreen(packageName: String) {
        val service = TaskBarAccessibilityService.instance
        if (service == null) {
            Log.w("AppRepository", "Split-screen needs the accessibility service; launching normally")
            launchNormal(packageName)
            return
        }
        // Put the current foreground app into split-screen, then launch this app
        // into the adjacent pane once the split animation has settled.
        service.enterSplitScreen()
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = getLaunchIntent(packageName) ?: return@postDelayed
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w("AppRepository", "Failed to launch $packageName in split-screen", e)
            }
        }, SPLIT_SCREEN_LAUNCH_DELAY_MS)
    }

    companion object {
        private const val SPLIT_SCREEN_LAUNCH_DELAY_MS = 350L
    }
}

enum class LaunchMode { NORMAL, SPLIT_SCREEN, FLOATING }
