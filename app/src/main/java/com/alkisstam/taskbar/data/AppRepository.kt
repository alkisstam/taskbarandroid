package com.alkisstam.taskbar.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private fun loadApps() {
        scope.launch {
            try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                _apps.value = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                    .mapNotNull { resolveInfo ->
                        try {
                            AppInfo(
                                packageName = resolveInfo.activityInfo.packageName,
                                label = resolveInfo.loadLabel(pm).toString(),
                                // toBitmap() can return the system's cached Bitmap instance without
                                // copying (androidx shortcut when config already matches); that shared
                                // bitmap can later be recycled by the OS, so copy it to own our instance.
                                icon = resolveInfo.loadIcon(pm).toBitmap().copy(Bitmap.Config.ARGB_8888, false)
                            )
                        } catch (e: Throwable) {
                            // Throwable, not Exception: MIUI's IconCustomizer inflates huge
                            // bitmaps and throws OutOfMemoryError (an Error) on some devices;
                            // skip that app instead of crashing the whole load.
                            Log.w("AppRepository", "Skipping app with broken resources: ${resolveInfo.activityInfo?.packageName}", e)
                            null
                        }
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            } catch (e: Exception) {
                Log.w("AppRepository", "Failed to load installed apps", e)
            }
        }
    }

    fun getLaunchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

}
