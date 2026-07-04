package com.alkisstam.taskbar.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.alkisstam.taskbar.ui.common.LocalHapticEnabled
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.alkisstam.taskbar.MainActivity
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.data.AppRepository
import com.alkisstam.taskbar.data.ClipboardRepository
import com.alkisstam.taskbar.data.MediaRepository
import com.alkisstam.taskbar.data.PreferencesRepository
import com.alkisstam.taskbar.data.QuickControlsRepository
import com.alkisstam.taskbar.util.Constants
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.ClipboardViewModel
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var prefsRepository: PreferencesRepository
    @Inject lateinit var quickControlsRepository: QuickControlsRepository
    @Inject lateinit var mediaRepository: MediaRepository
    @Inject lateinit var clipboardRepository: ClipboardRepository

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var _windowManager: WindowManager
    private var activeWM: WindowManager? = null
    private val windowManager: WindowManager
        get() = activeWM ?: TaskBarAccessibilityService.instance?.accessibilityWindowManager ?: _windowManager

    private fun removeViewFromAnyWM(view: View) {
        val wms = listOfNotNull(_windowManager, TaskBarAccessibilityService.instance?.accessibilityWindowManager)
        for ((index, wm) in wms.withIndex()) {
            try {
                wm.removeView(view)
                return
            } catch (e: Exception) {
                if (index == wms.lastIndex) Log.w(TAG, "Failed to remove view from any WindowManager", e)
            }
        }
    }

    // WindowManager forbids changing LayoutParams.type via updateViewLayout on an already-added
    // window. overlayWindowType() reads live accessibility-service state, so a stale type here
    // would crash later on the deferred relayout traversal. Only refreshAllViews() (remove+re-add)
    // is allowed to change a window's type.
    private fun WindowManager.updateViewLayoutKeepingType(view: View, params: WindowManager.LayoutParams) {
        (view.layoutParams as? WindowManager.LayoutParams)?.let { params.type = it.type }
        updateViewLayout(view, params)
    }
    private var overlayView: View? = null
    private var taskbarView: View? = null
    private var pillView: View? = null
    private var pillView2: View? = null
    private var searchView: View? = null
    private var volumePanelView: View? = null
    private var brightnessPanelView: View? = null
    private var volumeScrimView: View? = null
    private var musicPanelView: View? = null
    private var clipboardPanelView: View? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observersStarted = false

    private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var overlayHiddenForLockscreen = false
    @Volatile private var hiddenForLandscape = false

    private val lockscreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    overlayHiddenForLockscreen = true
                    if (this@OverlayService::appMenuViewModel.isInitialized) appMenuViewModel.deactivateCaffeine()
                    handler.post {
                        overlayView?.visibility = View.GONE
                        taskbarView?.visibility = View.GONE
                        pillView?.visibility = View.GONE
                        pillView2?.visibility = View.GONE
                        searchView?.visibility = View.GONE
                        musicPanelView?.visibility = View.GONE
                        volumeScrimView?.visibility = View.GONE
                        volumePanelView?.visibility = View.GONE
                        brightnessPanelView?.visibility = View.GONE
                        clipboardPanelView?.visibility = View.GONE
                    }
                }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                    overlayHiddenForLockscreen = false
                    handler.post {
                        if (overlayView?.isAttachedToWindow != true) addOverlayView()
                        if (taskbarView?.isAttachedToWindow != true) addTaskbarView()
                        if (pillView?.isAttachedToWindow != true) addPillView()
                        musicPanelView?.visibility = View.VISIBLE
                        if (!hiddenForLandscape) {
                            taskbarView?.visibility = View.VISIBLE
                            pillView?.visibility = View.VISIBLE
                            pillView2?.visibility = View.VISIBLE
                        }
                    }
                }
                Intent.ACTION_CONFIGURATION_CHANGED -> {
                    val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    hiddenForLandscape = isLandscape && taskbarViewModel.autoHideInLandscape.value
                    if (hiddenForLandscape) {
                        overlayView?.visibility = View.GONE
                        taskbarView?.visibility = View.GONE
                        pillView?.visibility = View.GONE
                        pillView2?.visibility = View.GONE
                    } else if (!overlayHiddenForLockscreen) {
                        if (appMenuViewModel.menuVisible.value) overlayView?.visibility = View.VISIBLE
                        taskbarView?.visibility = View.VISIBLE
                        pillView?.visibility = View.VISIBLE
                        pillView2?.visibility = View.VISIBLE
                    }
                }
                ACTION_SETTINGS_OPEN -> {
                    taskbarViewModel.setSettingsOpen(true)
                }
                ACTION_SETTINGS_CLOSE -> {
                    taskbarViewModel.setSettingsOpen(false)
                }
                ACTION_DISMISS_ALL -> {
                    if (this@OverlayService::appMenuViewModel.isInitialized) dismissAll()
                }
                ACTION_ACCESSIBILITY_CHANGED -> {
                    Handler(Looper.getMainLooper()).post { refreshAllViews() }
                }
                ACTION_CLIPBOARD_PANEL_SHOW -> {
                    if (this@OverlayService::appMenuViewModel.isInitialized)
                        appMenuViewModel.toggleClipboardPanel()
                }
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL
            if (level >= 0 && scale > 0) taskbarViewModel.updateBattery(level * 100 / scale, charging)
        }
    }

    private fun dismissAll() {
        appMenuViewModel.dismissMenu()
        taskbarViewModel.hideTaskbar()
    }

    private lateinit var taskbarViewModel: TaskbarViewModel
    private lateinit var appMenuViewModel: AppMenuViewModel
    private lateinit var clipboardViewModel: ClipboardViewModel

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "taskbar_overlay_channel"
        const val ACTION_SETTINGS_OPEN = "com.alkisstam.taskbar.ACTION_SETTINGS_OPEN"
        const val ACTION_SETTINGS_CLOSE = "com.alkisstam.taskbar.ACTION_SETTINGS_CLOSE"
        const val ACTION_DISMISS_ALL = "com.alkisstam.taskbar.DISMISS_ALL"
        const val ACTION_ACCESSIBILITY_CHANGED = "com.alkisstam.taskbar.ACCESSIBILITY_CHANGED"
        const val ACTION_CLIPBOARD_PANEL_SHOW = "com.alkisstam.taskbar.CLIPBOARD_PANEL_SHOW"

        @Volatile var isTaskbarVisibleForBack = false
    }

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        _windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        val factory = OverlayViewModelFactory(
            context = this,
            appRepository = appRepository,
            prefsRepository = prefsRepository,
            quickControlsRepository = quickControlsRepository,
            mediaRepository = mediaRepository,
            clipboardRepository = clipboardRepository
        )
        val provider = ViewModelProvider(this, factory)
        taskbarViewModel = provider[TaskbarViewModel::class.java]
        appMenuViewModel = provider[AppMenuViewModel::class.java]
        clipboardViewModel = provider[ClipboardViewModel::class.java]

        // Register receivers only after the ViewModels exist: the battery filter is a sticky
        // broadcast whose onReceive touches taskbarViewModel, and other actions reference the
        // ViewModels too.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            addAction(ACTION_SETTINGS_OPEN)
            addAction(ACTION_SETTINGS_CLOSE)
            addAction(ACTION_DISMISS_ALL)
            addAction(ACTION_ACCESSIBILITY_CHANGED)
            addAction(ACTION_CLIPBOARD_PANEL_SHOW)
        }
        ContextCompat.registerReceiver(this, lockscreenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (background start without exemption) or
            // a missing-notification-permission error would otherwise crash the process.
            Log.w(TAG, "startForeground failed; stopping service", e)
            stopSelf()
            return START_NOT_STICKY
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        hiddenForLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && taskbarViewModel.autoHideInLandscape.value
        activeWM = TaskBarAccessibilityService.instance?.accessibilityWindowManager ?: _windowManager
        addTaskbarView()
        addOverlayView()
        addPillView()
        addSearchView()
        addMusicPanelView()
        addVolumeScrimView()
        addVolumePanelView()
        addBrightnessPanelView()
        addClipboardPanelView()
        if (!observersStarted) {
            observersStarted = true
            observePillPosition()
            observeOverlayInteractivity()
            observeTaskbarInteractivity()
            observeSearchVisibility()
            observeMusicPanelPosition()
            observeVolumeAndBrightnessPanels()
            observeMusicPanelVisibility()
            observeTranslucentMode()
            observeClipboardPanel()
        }
        return START_STICKY
    }

    // region Quick strip state

    private var volumePanelYOffsetDp: Float = 0f
    private var musicPanelYOffsetDp: Float = 0f
    private var translucentModeEnabled: Boolean = false
    private var taskbarInteractive: Boolean = true

    private fun setOverlayFlags(interactive: Boolean, focusable: Boolean) {
        val view = overlayView ?: return
        try { windowManager.updateViewLayoutKeepingType(view, overlayLayoutParams(interactive, focusable)) }
        catch (e: Exception) { Log.w(TAG, "Failed to update overlay layout flags", e) }
    }

    private fun setTaskbarFlags(interactive: Boolean) {
        taskbarInteractive = interactive
        val view = taskbarView ?: return
        try { windowManager.updateViewLayoutKeepingType(view, taskbarLayoutParams(interactive)) }
        catch (e: Exception) { Log.w(TAG, "Failed to update taskbar layout flags", e) }
    }

    private fun setSearchFlags(active: Boolean) {
        val view = searchView ?: return
        try { windowManager.updateViewLayoutKeepingType(view, searchLayoutParams(focusable = active)) }
        catch (e: Exception) { Log.w(TAG, "Failed to update search layout flags", e) }
    }

    private fun setVolumeScrimActive(active: Boolean) {
        val view = volumeScrimView ?: return
        try { windowManager.updateViewLayoutKeepingType(view, volumeScrimLayoutParams(active = active)) }
        catch (e: Exception) { Log.w(TAG, "Failed to update volume scrim flags", e) }
    }

    // endregion

    // region Observers

    private fun observeOverlayInteractivity() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                appMenuViewModel.menuVisible,
                appMenuViewModel.isSearching
            ) { menuOpen, searching ->
                Pair(menuOpen, menuOpen && !searching)
            }
            .collect { (interactive, focusable) ->
                if (interactive) {
                    overlayView?.visibility = View.VISIBLE
                    setOverlayFlags(interactive = true, focusable = focusable)
                } else {
                    setOverlayFlags(interactive = false, focusable = false)
                    kotlinx.coroutines.delay(Constants.OVERLAY_HIDE_DEBOUNCE_MS)
                    overlayView?.visibility = View.GONE
                }
            }
        }
    }

    private fun observeTaskbarInteractivity() {
        serviceScope.launch {
            taskbarViewModel.isTaskbarVisible.collect { interactive ->
                isTaskbarVisibleForBack = interactive
                setTaskbarFlags(interactive = interactive)
            }
        }
    }

    private fun observePillPosition() {
        serviceScope.launch {
            taskbarViewModel.pillSettings.collect { settings ->
                val view = pillView ?: return@collect
                try { windowManager.updateViewLayoutKeepingType(view, pillLayoutParams(settings, isRight = false)) }
                catch (e: Exception) { Log.w(TAG, "Failed to update pill position", e) }

                if (settings.edgePosition == com.alkisstam.taskbar.data.PillEdgePosition.BOTH) {
                    ensurePillView2()
                    val v2 = pillView2 ?: return@collect
                    try { windowManager.updateViewLayoutKeepingType(v2, pillLayoutParams(settings, isRight = true)) }
                    catch (e: Exception) { Log.w(TAG, "Failed to update pill2 position", e) }
                } else {
                    removePillView2()
                }
            }
        }
    }

    private fun ensurePillView2() {
        if (pillView2?.isAttachedToWindow == true) return
        pillView2?.let { removeViewFromAnyWM(it) }
        pillView2 = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent { TriggerPillContent(taskbarViewModel = taskbarViewModel) }
            }
            pillView2 = composeView
            if (hiddenForLandscape) composeView.visibility = View.GONE
            val settings = taskbarViewModel.pillSettings.value
            windowManager.addView(composeView, pillLayoutParams(settings, isRight = true))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add pill2 view", e)
            pillView2 = null
        }
    }

    private fun removePillView2() {
        pillView2?.let { removeViewFromAnyWM(it) }
        pillView2 = null
    }

    private fun observeSearchVisibility() {
        serviceScope.launch {
            appMenuViewModel.isSearching.collect { searching ->
                searchView?.visibility = if (searching) View.VISIBLE else View.GONE
                setSearchFlags(active = searching)
            }
        }
    }

    private fun observeMusicPanelPosition() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                taskbarViewModel.taskbarSettings,
                appMenuViewModel.menuVisible,
                taskbarViewModel.dockExpandProgress
            ) { settings, menuOpen, expandProgress ->
                val baseY = 20f + settings.heightDp + 16f + 24f + expandProgress * (settings.heightDp + 1f)
                volumePanelYOffsetDp = baseY
                if (menuOpen) baseY + 400f else baseY
            }.collect { yOffset ->
                musicPanelYOffsetDp = yOffset
                val view = musicPanelView ?: return@collect
                try { windowManager.updateViewLayoutKeepingType(view, musicPanelLayoutParams(musicPanelYOffsetDp, translucentModeEnabled)) }
                catch (e: Exception) { Log.w(TAG, "Failed to update music panel position", e) }
            }
        }
    }

    private fun observeMusicPanelVisibility() {
        musicPanelView?.visibility = View.VISIBLE
    }

    private fun observeTranslucentMode() {
        serviceScope.launch {
            taskbarViewModel.translucentMode.collect { enabled ->
                translucentModeEnabled = enabled
                val yOffset = volumePanelYOffsetDp
                volumePanelView?.let { view ->
                    try { windowManager.updateViewLayoutKeepingType(view, volumePanelLayoutParams(yOffset, translucentModeEnabled)) }
                    catch (e: Exception) { Log.w(TAG, "Failed to update volume panel blur", e) }
                }
                brightnessPanelView?.let { view ->
                    try { windowManager.updateViewLayoutKeepingType(view, volumePanelLayoutParams(yOffset, translucentModeEnabled)) }
                    catch (e: Exception) { Log.w(TAG, "Failed to update brightness panel blur", e) }
                }
                musicPanelView?.let { view ->
                    try { windowManager.updateViewLayoutKeepingType(view, musicPanelLayoutParams(musicPanelYOffsetDp, translucentModeEnabled)) }
                    catch (e: Exception) { Log.w(TAG, "Failed to update music panel blur", e) }
                }
            }
        }
    }

    private fun observeVolumeAndBrightnessPanels() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                appMenuViewModel.volumePanelVisible,
                appMenuViewModel.brightnessPanelVisible
            ) { volumeVisible, brightnessVisible ->
                Pair(volumeVisible, brightnessVisible)
            }.collect { (volumeVisible, brightnessVisible) ->
                val settings = taskbarViewModel.taskbarSettings.value
                val dockExpanded = taskbarViewModel.isDockExpanded.value
                val yOffset = 20f + settings.heightDp + 16f + (if (dockExpanded) settings.heightDp + 25f else 24f)

                val scrimActive = volumeVisible || brightnessVisible
                setVolumeScrimActive(scrimActive)
                volumeScrimView?.visibility = if (scrimActive) View.VISIBLE else View.GONE

                volumePanelView?.visibility = if (volumeVisible) View.VISIBLE else View.GONE
                if (volumeVisible) {
                    val view = volumePanelView ?: return@collect
                    try { windowManager.updateViewLayoutKeepingType(view, volumePanelLayoutParams(yOffset, translucentModeEnabled)) }
                    catch (e: Exception) { Log.w(TAG, "Failed to update volume panel position", e) }
                }

                brightnessPanelView?.visibility = if (brightnessVisible) View.VISIBLE else View.GONE
                if (brightnessVisible) {
                    val view = brightnessPanelView ?: return@collect
                    try { windowManager.updateViewLayoutKeepingType(view, volumePanelLayoutParams(yOffset, translucentModeEnabled)) }
                    catch (e: Exception) { Log.w(TAG, "Failed to update brightness panel position", e) }
                }
            }
        }
    }

    // endregion

    // region Add views

    private fun addOverlayView() {
        if (overlayView?.isAttachedToWindow == true) return
        overlayView?.let { removeViewFromAnyWM(it) }
        overlayView = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    val hapticEnabled by taskbarViewModel.hapticFeedbackEnabled.collectAsState()
                    CompositionLocalProvider(LocalHapticEnabled provides hapticEnabled) {
                        OverlayContent(
                            taskbarViewModel = taskbarViewModel,
                            appMenuViewModel = appMenuViewModel
                        )
                    }
                }
            }
            val wrapper = object : FrameLayout(this) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        dismissAll()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }
            wrapper.setViewTreeLifecycleOwner(this@OverlayService)
            wrapper.setViewTreeViewModelStoreOwner(this@OverlayService)
            wrapper.setViewTreeSavedStateRegistryOwner(this@OverlayService)
            wrapper.addView(composeView)
            overlayView = wrapper
            if (hiddenForLandscape || !appMenuViewModel.menuVisible.value) wrapper.visibility = View.GONE
            windowManager.addView(wrapper, overlayLayoutParams(interactive = false))
            attachInsetsListener(wrapper)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
            overlayView = null
        }
    }

    private fun attachInsetsListener(view: View) {
        // ViewCompat.setOnApplyWindowInsetsListener is dispatched from ViewRootImpl even when the
        // view is GONE, so the overlay can self-heal after the keyboard closes rather than staying
        // stuck in GONE state (the OnGlobalLayoutListener deadlock).
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            if (taskbarViewModel.autoHideInFullscreen.value) {
                val isFullscreen = !insets.isVisible(WindowInsetsCompat.Type.statusBars())
                if (isFullscreen) taskbarViewModel.hideTaskbar() else taskbarViewModel.showTaskbar()
            }
            if (!overlayHiddenForLockscreen) {
                if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                    // Make non-interactive so keyboard receives touches, but keep VISIBLE.
                    // Setting GONE prevents the insets listener from firing, permanently losing
                    // the overlay (deadlock).
                    setOverlayFlags(interactive = false, focusable = false)
                } else {
                    val menuOpen = appMenuViewModel.menuVisible.value
                    val searching = appMenuViewModel.isSearching.value
                    setOverlayFlags(
                        interactive = menuOpen,
                        focusable = menuOpen && !searching
                    )
                }
            }
            insets
        }
    }

    private fun addTaskbarView() {
        if (taskbarView?.isAttachedToWindow == true) return
        taskbarView?.let { removeViewFromAnyWM(it) }
        taskbarView = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    val hapticEnabled by taskbarViewModel.hapticFeedbackEnabled.collectAsState()
                    CompositionLocalProvider(LocalHapticEnabled provides hapticEnabled) {
                        TaskbarContent(
                            taskbarViewModel = taskbarViewModel,
                            appMenuViewModel = appMenuViewModel
                        )
                    }
                }
            }
            val wrapper = object : FrameLayout(this) {
                override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                    val result = super.dispatchTouchEvent(ev)
                    if (!result && ev.action == MotionEvent.ACTION_DOWN
                        && taskbarViewModel.isTaskbarVisible.value
                        && !appMenuViewModel.menuVisible.value) {
                        taskbarViewModel.hideTaskbar()
                        // Apply non-interactive flags synchronously so any new window
                        // appearing after this touch is not blocked by our overlay.
                        setTaskbarFlags(interactive = false)
                    }
                    return result
                }
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        dismissAll()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }
            wrapper.setViewTreeLifecycleOwner(this@OverlayService)
            wrapper.setViewTreeViewModelStoreOwner(this@OverlayService)
            wrapper.setViewTreeSavedStateRegistryOwner(this@OverlayService)
            wrapper.addView(composeView)
            taskbarView = wrapper
            if (hiddenForLandscape) wrapper.visibility = View.GONE
            val initialInteractive = taskbarViewModel.isTaskbarVisible.value && !appMenuViewModel.menuVisible.value
            taskbarInteractive = initialInteractive
            windowManager.addView(wrapper, taskbarLayoutParams(initialInteractive))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add taskbar view", e)
            taskbarView = null
        }
    }

    private fun addPillView() {
        if (pillView?.isAttachedToWindow == true) return
        pillView?.let { removeViewFromAnyWM(it) }
        pillView = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent { TriggerPillContent(taskbarViewModel = taskbarViewModel) }
            }
            pillView = composeView
            if (hiddenForLandscape) composeView.visibility = View.GONE
            val initial = taskbarViewModel.pillSettings.value
            windowManager.addView(composeView, pillLayoutParams(initial, isRight = false))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add pill view", e)
            pillView = null
        }
    }

    private fun addSearchView() {
        if (searchView?.isAttachedToWindow == true) return
        searchView?.let { removeViewFromAnyWM(it) }
        searchView = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    val hapticEnabled by taskbarViewModel.hapticFeedbackEnabled.collectAsState()
                    CompositionLocalProvider(LocalHapticEnabled provides hapticEnabled) {
                        SearchOverlayContent(appMenuViewModel = appMenuViewModel, taskbarViewModel = taskbarViewModel, onHideTaskbar = taskbarViewModel::hideTaskbar)
                    }
                }
            }
            val wrapper = object : FrameLayout(this) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        appMenuViewModel.closeSearch()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }
            wrapper.setViewTreeLifecycleOwner(this@OverlayService)
            wrapper.setViewTreeViewModelStoreOwner(this@OverlayService)
            wrapper.setViewTreeSavedStateRegistryOwner(this@OverlayService)
            wrapper.addView(composeView)
            wrapper.visibility = if (appMenuViewModel.isSearching.value) View.VISIBLE else View.GONE
            searchView = wrapper
            windowManager.addView(wrapper, searchLayoutParams(focusable = appMenuViewModel.isSearching.value))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add search view", e)
            searchView = null
        }
    }

    private fun addVolumeScrimView() {
        if (volumeScrimView?.isAttachedToWindow == true) return
        volumeScrimView?.let { removeViewFromAnyWM(it) }
        volumeScrimView = null
        try {
            val view = android.view.View(this).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setOnClickListener {
                    appMenuViewModel.dismissVolumePanel()
                    appMenuViewModel.dismissBrightnessPanel()
                }
            }
            view.visibility = View.GONE
            volumeScrimView = view
            windowManager.addView(view, volumeScrimLayoutParams())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add volume scrim view", e)
            volumeScrimView = null
        }
    }

    private fun addVolumePanelView() {
        if (volumePanelView?.isAttachedToWindow == true) return
        volumePanelView?.let { removeViewFromAnyWM(it) }
        volumePanelView = null
        try {
            val initialSettings = taskbarViewModel.taskbarSettings.value
            volumePanelYOffsetDp = 20f + initialSettings.heightDp + 16f + 24f
            val composeView = ComposeView(this).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    VolumePanelContent(
                        taskbarViewModel = taskbarViewModel,
                        appMenuViewModel = appMenuViewModel
                    )
                }
            }
            composeView.visibility = View.GONE
            volumePanelView = composeView
            windowManager.addView(composeView, volumePanelLayoutParams(volumePanelYOffsetDp, translucentModeEnabled))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add volume panel view", e)
            volumePanelView = null
        }
    }

    private fun addBrightnessPanelView() {
        if (brightnessPanelView?.isAttachedToWindow == true) return
        brightnessPanelView?.let { removeViewFromAnyWM(it) }
        brightnessPanelView = null
        try {
            val initialSettings = taskbarViewModel.taskbarSettings.value
            val yOffset = 20f + initialSettings.heightDp + 16f + 24f
            val composeView = ComposeView(this).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    BrightnessPanelContent(
                        taskbarViewModel = taskbarViewModel,
                        appMenuViewModel = appMenuViewModel
                    )
                }
            }
            composeView.visibility = View.GONE
            brightnessPanelView = composeView
            windowManager.addView(composeView, volumePanelLayoutParams(yOffset, translucentModeEnabled))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add brightness panel view", e)
            brightnessPanelView = null
        }
    }

    private fun addMusicPanelView() {
        if (musicPanelView?.isAttachedToWindow == true) return
        musicPanelView?.let { removeViewFromAnyWM(it) }
        musicPanelView = null
        try {
            val initialSettings = taskbarViewModel.taskbarSettings.value
            musicPanelYOffsetDp = 20f + initialSettings.heightDp + 16f + 24f
            val composeView = ComposeView(this).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    MusicPanelContent(
                        taskbarViewModel = taskbarViewModel,
                        appMenuViewModel = appMenuViewModel
                    )
                }
            }
            composeView.visibility = View.GONE
            musicPanelView = composeView
            windowManager.addView(composeView, musicPanelLayoutParams(musicPanelYOffsetDp, translucentModeEnabled))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add music panel view", e)
            musicPanelView = null
        }
    }

    private fun addClipboardPanelView() {
        if (clipboardPanelView?.isAttachedToWindow == true) return
        clipboardPanelView?.let { removeViewFromAnyWM(it) }
        clipboardPanelView = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    val hapticEnabled by taskbarViewModel.hapticFeedbackEnabled.collectAsState()
                    CompositionLocalProvider(LocalHapticEnabled provides hapticEnabled) {
                        ClipboardPanelContent(
                            taskbarViewModel = taskbarViewModel,
                            appMenuViewModel = appMenuViewModel,
                            clipboardViewModel = clipboardViewModel
                        )
                    }
                }
            }
            val wrapper = object : FrameLayout(this) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        appMenuViewModel.dismissClipboardPanel()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }
            wrapper.setViewTreeLifecycleOwner(this@OverlayService)
            wrapper.setViewTreeViewModelStoreOwner(this@OverlayService)
            wrapper.setViewTreeSavedStateRegistryOwner(this@OverlayService)
            wrapper.addView(composeView)
            wrapper.visibility = View.GONE
            clipboardPanelView = wrapper
            windowManager.addView(wrapper, searchLayoutParams(focusable = false))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add clipboard panel view", e)
            clipboardPanelView = null
        }
    }

    private fun observeClipboardPanel() {
        serviceScope.launch {
            appMenuViewModel.clipboardPanelVisible.collect { visible ->
                clipboardPanelView?.visibility = if (visible) View.VISIBLE else View.GONE
                val view = clipboardPanelView ?: return@collect
                try { windowManager.updateViewLayoutKeepingType(view, searchLayoutParams(focusable = visible)) }
                catch (e: Exception) { Log.w(TAG, "Failed to update clipboard panel flags", e) }
            }
        }
    }

    // endregion

    // region Remove / refresh views

    private fun removeOverlayView() {
        overlayView?.let { removeViewFromAnyWM(it) }; overlayView = null
        taskbarView?.let { removeViewFromAnyWM(it) }; taskbarView = null
        pillView?.let { removeViewFromAnyWM(it) }; pillView = null
        removePillView2()
        searchView?.let { removeViewFromAnyWM(it) }; searchView = null
        volumePanelView?.let { removeViewFromAnyWM(it) }; volumePanelView = null
        brightnessPanelView?.let { removeViewFromAnyWM(it) }; brightnessPanelView = null
        volumeScrimView?.let { removeViewFromAnyWM(it) }; volumeScrimView = null
        musicPanelView?.let { removeViewFromAnyWM(it) }; musicPanelView = null
        clipboardPanelView?.let { removeViewFromAnyWM(it) }; clipboardPanelView = null
    }

    private fun refreshAllViews() {
        if (!observersStarted) return
        removeOverlayView()
        activeWM = TaskBarAccessibilityService.instance?.accessibilityWindowManager ?: _windowManager
        addTaskbarView()
        addOverlayView()
        addPillView()
        if (taskbarViewModel.pillSettings.value.edgePosition == com.alkisstam.taskbar.data.PillEdgePosition.BOTH) ensurePillView2()
        addSearchView()
        addMusicPanelView()
        addVolumeScrimView()
        addVolumePanelView()
        addBrightnessPanelView()
        addClipboardPanelView()
    }

    // endregion

    // region Notification

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.overlay_channel_desc)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    // endregion

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        observersStarted = false
        unregisterReceiver(lockscreenReceiver)
        unregisterReceiver(batteryReceiver)
        removeOverlayView()
        activeWM = null
        serviceScope.cancel()
        _viewModelStore.clear()
        // AppRepository / QuickControlsRepository are @Singleton (process-scoped) and register
        // their receivers/observers once in init{}. Tearing them down here left them dead after
        // a service restart (dock toggled off/on) because init{} never re-runs on the reused
        // singleton — silently breaking app-list refresh and live control state. Let them live
        // for the process lifetime instead.
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
