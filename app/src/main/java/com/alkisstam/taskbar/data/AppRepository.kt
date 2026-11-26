package com.alkisstam.taskbar.data

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val ICON_SIZE_PX = 160
private const val SHIZUKU_PERMISSION_CODE = 1001

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

    private val savedFloatingBounds: StateFlow<Map<String, String>> = preferencesRepository.floatingWindowBounds
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())
    private val boundsTrackers = ConcurrentHashMap<String, Job>()
    private val freeformMutex = Mutex()
    // Original global settings changed by a Shizuku freeform launch. Not in backup_rules,
    // so originals from one device never restore onto another.
    private val freeformRestore = context.getSharedPreferences("freeform_restore", Context.MODE_PRIVATE)

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
        Shizuku.addBinderReceivedListenerSticky {
            if (freeformRestore.all.isEmpty()) return@addBinderReceivedListenerSticky
            scope.launch {
                freeformMutex.withLock {
                    val granted = runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
                    if (granted) restoreFreeformGlobals()
                }
            }
        }
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
                    val loaded = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                        .mapNotNull { resolveInfo ->
                            // The loop never suspends, so without this cancel() can't stop a superseded load.
                            ensureActive()
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
                            val icon = iconCache[activityInfo.packageName] ?: if (memoryLow) {
                                iconsSkipped = true
                                null
                            } else loadIcon(pm, resolveInfo, activityInfo, iconPack)
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
                    ensureActive()
                    _apps.value = loaded
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

    // Load the icon separately so a broken icon doesn't drop the whole app
    // from the list: MIUI's IconCustomizer inflates huge bitmaps and throws
    // OutOfMemoryError (an Error, hence Throwable) on some devices. Keep the
    // app with a null icon (UI renders a placeholder) instead of hiding it.
    private fun loadIcon(pm: PackageManager, resolveInfo: ResolveInfo, activityInfo: ActivityInfo, iconPack: String): Bitmap? = try {
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

    fun launchAppFloating(packageName: String) {
        val intent = getLaunchIntent(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val savedBounds = savedFloatingBounds.value[packageName]?.let { Rect.unflattenFromString(it) }
        val shizukuState = try {
            if (!Shizuku.pingBinder()) null else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) { null }
        // Shizuku also remembers window sizes on devices with native freeform, so ask for it there too,
        // unless the user already chose "deny" in Shizuku's dialog.
        if (shizukuState == false) runCatching {
            if (!Shizuku.shouldShowRequestPermissionRationale()) Shizuku.requestPermission(SHIZUKU_PERMISSION_CODE)
        }
        val component = intent.component?.flattenToString()
        if (!freeformSupported() && shizukuState == true && component != null) {
            scope.launch { shizukuFreeformLaunch(packageName, component, savedBounds) }
            return
        }
        val opts = ActivityOptions.makeBasic()
        if (savedBounds != null) opts.setLaunchBounds(savedBounds)
        // Hidden API; WINDOWING_MODE_FREEFORM = 5. Without saved bounds we let the system pick the size.
        runCatching {
            ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                .invoke(opts, 5)
        }.onFailure {
            Log.w("AppRepository", "setLaunchWindowingMode unavailable", it)
            if (savedBounds == null) opts.setLaunchBounds(floatingBounds())
        }
        try {
            context.startActivity(intent, opts.toBundle())
        } catch (e: Exception) {
            Log.w("AppRepository", "Failed to launch $packageName floating", e)
            return
        }
        if (shizukuState == true) trackFloatingBounds(packageName, savedBounds)
    }

    private fun freeformSupported(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT) ||
            Settings.Global.getInt(context.contentResolver, "enable_freeform_support", 0) == 1

    private fun floatingBounds(): Rect {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels * 7 / 10
        val h = dm.heightPixels * 6 / 10
        val left = (dm.widthPixels - w) / 2
        val top = (dm.heightPixels - h) / 2
        return Rect(left, top, left + w, top + h)
    }

    @Suppress("DEPRECATION")
    private fun shell(vararg cmd: String): Pair<Int, String> {
        val proc = Shizuku.newProcess(arrayOf(*cmd), null, null)
        val out = proc.inputStream.bufferedReader().readText().trim()
        return proc.waitFor() to out
    }

    // Overlapping launches would read each other's "1" as the original value.
    private suspend fun shizukuFreeformLaunch(packageName: String, component: String, savedBounds: Rect?) = freeformMutex.withLock {
        val keys = listOf("enable_freeform_support", "force_resizable_activities")
        try {
            // Empty output means unset; store it as "null" so restore deletes the key instead of writing "".
            val changed = keys.associateWith { shell("settings", "get", "global", it).second.ifEmpty { "null" } }.filterValues { it != "1" }
            // Persisted before the change: if the process dies before finally, the next start puts them back.
            freeformRestore.edit().apply { changed.forEach { (key, value) -> putString(key, value) } }.commit()
            changed.keys.forEach { shell("settings", "put", "global", it, "1") }
            if (shell("am", "start", "--windowingMode", "5", "-n", component).first == 0) {
                trackFloatingBounds(packageName, savedBounds)
                // Put the device-wide flags back once the window exists, so nothing else on the device changes.
                delay(3000)
            } else {
                launchApp(packageName)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AppRepository", "Shizuku freeform launch failed for $packageName", e)
            launchApp(packageName)
        } finally {
            withContext(NonCancellable) { restoreFreeformGlobals() }
        }
    }

    // Keys are only dropped once restored, so a failed restore is retried on the next launch or start.
    private fun restoreFreeformGlobals() {
        val editor = freeformRestore.edit()
        freeformRestore.all.forEach { (key, value) ->
            runCatching {
                val exit = if (value == "null") shell("settings", "delete", "global", key).first
                else shell("settings", "put", "global", key, value.toString()).first
                if (exit == 0) editor.remove(key)
            }
        }
        editor.commit()
    }

    // Polls the app's freeform task via Shizuku while its window is open and saves the bounds, so the
    // next floating launch reopens it where the user left it. Stops once the task is gone or hidden.
    private fun trackFloatingBounds(packageName: String, restore: Rect?) {
        boundsTrackers.remove(packageName)?.cancel()
        boundsTrackers[packageName] = scope.launch {
            delay(1000)
            var pendingRestore = restore
            var misses = 0
            while (misses < 3) {
                val task = try { findFreeformTask(packageName) } catch (e: Exception) { null }
                if (task == null) {
                    misses++
                } else {
                    misses = 0
                    val (taskId, bounds) = task
                    val target = pendingRestore
                    pendingRestore = null
                    // setLaunchBounds isn't honoured everywhere; resize once if the window opened elsewhere.
                    if (target != null && target != bounds) {
                        runCatching {
                            shell("am", "task", "resize", "$taskId",
                                "${target.left}", "${target.top}", "${target.right}", "${target.bottom}")
                        }
                    } else if (bounds.flattenToString() != savedFloatingBounds.value[packageName]) {
                        preferencesRepository.setFloatingWindowBounds(packageName, bounds.flattenToString())
                    }
                }
                delay(2000)
            }
            boundsTrackers.remove(packageName, coroutineContext[Job])
        }
    }

    private fun findFreeformTask(packageName: String): Pair<Int, Rect>? {
        val taskLine = Regex("""taskId=(\d+): ${Regex.escape(packageName)}/\S* bounds=\[(-?\d+),(-?\d+)]\[(-?\d+),(-?\d+)].*visible=true""")
        for (block in shell("am", "stack", "list").second.split(Regex("(?=RootTask id=)"))) {
            if (!block.contains("mWindowingMode=freeform")) continue
            val m = taskLine.find(block) ?: continue
            val (id, l, t, r, b) = m.destructured
            return id.toInt() to Rect(l.toInt(), t.toInt(), r.toInt(), b.toInt())
        }
        return null
    }

}
