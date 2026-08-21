package com.alkisstam.taskbar.data

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
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

    private var loadJob: Job? = null

    // Decoded icons keyed by packageName, so a package-changed broadcast or an
    // icon-skip retry doesn't re-decode apps that already succeeded. Bounded by
    // installed-app count, so no eviction policy beyond invalidation below is needed.
    private val iconCache = ConcurrentHashMap<String, Bitmap>()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            iconPackRepository.invalidateCache()
            // Icon may have changed (or the package is gone); only that entry is stale.
            intent.data?.schemeSpecificPart?.let { iconCache.remove(it) }
            loadApps()
        }
    }

    init {
        // First emission covers the initial load; later emissions re-theme icons live,
        // so every cached icon is stale (drawn from a different pack) and must be dropped.
        scope.launch {
            preferencesRepository.iconPackPackage.distinctUntilChanged().collect {
                iconCache.clear()
                loadApps()
            }
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

    // Single-flight: concurrent loads (startup collect + package broadcast + retry)
    // would draw VectorDrawables sharing one native tree from two threads at once,
    // which segfaults in hwui (Tree::drawStaging → getSkBitmap).
    private fun loadApps() {
        loadJob?.cancel()
        loadJob = scope.launch {
            var retriesLeft = 3
            var iconRetriesLeft = 8
            while (true) {
                try {
                    val pm = context.packageManager
                    val intent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val iconPack = preferencesRepository.iconPackPackage.first()
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val memInfo = ActivityManager.MemoryInfo()
                    var memoryLow = false
                    var iconsSkipped = false
                    _apps.value = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                        .mapNotNull { resolveInfo ->
                            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                            // Icon decode under system-wide memory pressure can kill the
                            // process natively (scudo aborts on mmap failure inside
                            // ImageDecoder) — uncatchable from Java. Poll lowMemory and
                            // skip decoding while it holds; a delayed reload below fills
                            // the missing icons in once memory recovers. lowMemory alone
                            // lags (flips only below the OEM threshold, and other processes
                            // can eat the remainder between our check and the allocation),
                            // so also skip while availMem is within 1.25× of the threshold.
                            am.getMemoryInfo(memInfo)
                            memoryLow = memInfo.lowMemory || memInfo.availMem < memInfo.threshold * 5 / 4
                            // Load the icon separately so a broken icon doesn't drop the whole app
                            // from the list: MIUI's IconCustomizer inflates huge bitmaps and throws
                            // OutOfMemoryError (an Error, hence Throwable) on some devices. Keep the
                            // app with a null icon (UI renders a placeholder) instead of hiding it.
                            val icon = iconCache[activityInfo.packageName] ?: if (memoryLow) {
                                iconsSkipped = true
                                null
                            } else try {
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
                                // mutate() first: Resources caches drawable ConstantState, and a
                                // VectorDrawable's native tree lives in that state — drawing a
                                // shared tree from two threads crashes in hwui.
                                val mutated = drawable.mutate()
                                val bmp = mutated.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
                                // toBitmap() returns the drawable's own cached Bitmap without
                                // copying when it's a BitmapDrawable already at the target size;
                                // that shared bitmap can later be recycled by the OS. Copy only
                                // in that case — unconditional copy doubled native allocations
                                // per icon, which matters when this crashes the process under
                                // memory pressure (scudo abort in allocateHeapBitmap).
                                val result = if (bmp === (mutated as? BitmapDrawable)?.bitmap) {
                                    bmp.copy(Bitmap.Config.ARGB_8888, false)
                                } else bmp
                                iconCache[activityInfo.packageName] = result
                                result
                            } catch (e: Throwable) {
                                Log.w("AppRepository", "Icon load failed, keeping app without icon: ${activityInfo.packageName}", e)
                                null
                            }
                            val installTime = try {
                                pm.getPackageInfo(activityInfo.packageName, 0).firstInstallTime
                            } catch (e: Exception) {
                                0L
                            }
                            AppInfo(
                                packageName = activityInfo.packageName,
                                label = resolveInfo.loadLabel(pm).toString(),
                                icon = icon,
                                firstInstallTime = installTime
                            )
                        }
                        .distinctBy { it.packageName }
                        .sortedBy { it.label.lowercase() }
                    if (iconsSkipped && iconRetriesLeft > 0) {
                        iconRetriesLeft--
                        delay(5000)
                        continue
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("AppRepository", "Failed to load installed apps", e)
                    // queryIntentActivities can fail with a transient binder error
                    // (DeadObjectException / binder buffer overflow). If we have no apps
                    // yet, back off and retry so the dock isn't left empty until the next
                    // package broadcast.
                    if (_apps.value.isEmpty() && retriesLeft > 0) {
                        delay(1000L * (4 - retriesLeft))
                        retriesLeft--
                        continue
                    }
                }
                return@launch
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
