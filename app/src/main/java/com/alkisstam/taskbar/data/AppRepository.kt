package com.alkisstam.taskbar.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val ICON_SIZE_PX = 160

@Singleton
class AppRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val iconPackRepository: IconPackRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            iconPackRepository.invalidateCache()
            loadApps()
        }
    }

    init {
        // First emission covers the initial load; later emissions re-theme icons live.
        scope.launch {
            preferencesRepository.iconPackPackage.distinctUntilChanged().collect { loadApps() }
        }
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
                val iconPack = preferencesRepository.iconPackPackage.first()
                _apps.value = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                    .mapNotNull { resolveInfo ->
                        val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                        // Load the icon separately so a broken icon doesn't drop the whole app
                        // from the list: MIUI's IconCustomizer inflates huge bitmaps and throws
                        // OutOfMemoryError (an Error, hence Throwable) on some devices. Keep the
                        // app with a null icon (UI renders a placeholder) instead of hiding it.
                        val icon = try {
                            // Load the icon resource directly instead of loadIcon(): on MIUI,
                            // loadIcon() routes through IconCustomizer.composeIcon which draws
                            // full-size themed bitmaps in-process and can abort() the whole
                            // process on native allocation failure (uncatchable). Direct
                            // Resources access bypasses that hook; DENSITY_XHIGH caps the
                            // source bitmap near our 160px target.
                            val packIcon = if (iconPack.isNotEmpty()) {
                                iconPackRepository.getIcon(iconPack, activityInfo.packageName, activityInfo.name)
                            } else null
                            val drawable = packIcon ?: try {
                                val res = pm.getResourcesForApplication(activityInfo.applicationInfo)
                                val resId = resolveInfo.iconResource
                                if (resId != 0) res.getDrawableForDensity(resId, DisplayMetrics.DENSITY_XHIGH, null) else null
                            } catch (e: Exception) {
                                null
                            } ?: resolveInfo.loadIcon(pm)
                            // Rendered at a fixed size: intrinsic-size bitmaps for every installed
                            // app add up to tens of MB (and themed OEM icons can be 1024px+).
                            // 160px covers the largest dock icon setting on 1080p densities.
                            // toBitmap() can return the system's cached Bitmap instance without
                            // copying (androidx shortcut when config already matches); that shared
                            // bitmap can later be recycled by the OS, so copy it to own our instance.
                            drawable.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
                                .copy(Bitmap.Config.ARGB_8888, false)
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

    fun launchApp(packageName: String) {
        val intent = getLaunchIntent(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("AppRepository", "Failed to launch $packageName", e)
        }
    }

}
