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
            }
        }
    }

    fun getLaunchIntent(packageName: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageName)

}
