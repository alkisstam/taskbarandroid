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
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
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
import com.alkisstam.taskbar.util.Constants
import com.alkisstam.taskbar.data.MediaRepository
import com.alkisstam.taskbar.data.PreferencesRepository
import com.alkisstam.taskbar.data.QuickControlsRepository
import com.alkisstam.taskbar.ui.appmenu.AppMenuPanel
import com.alkisstam.taskbar.ui.appmenu.BrightnessPanel
import com.alkisstam.taskbar.ui.appmenu.FloatingSearchBar
import com.alkisstam.taskbar.ui.appmenu.MusicPanel
import com.alkisstam.taskbar.ui.appmenu.VolumePanel
import com.alkisstam.taskbar.ui.taskbar.QuickStripView
import com.alkisstam.taskbar.ui.taskbar.TaskbarView
import com.alkisstam.taskbar.ui.taskbar.TriggerPillView
import com.alkisstam.taskbar.ui.theme.TaskBarTheme
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
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

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var _windowManager: WindowManager
    private val windowManager: WindowManager
        get() = TaskBarAccessibilityService.instance?.accessibilityWindowManager ?: _windowManager
    private var overlayView: View? = null
    private var pillView: View? = null
    private var searchView: View? = null
    private var quickStripView: View? = null
    private var volumePanelView: View? = null
    private var brightnessPanelView: View? = null
    private var volumeScrimView: View? = null
    private var musicPanelView: View? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observersStarted = false

    private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var overlayHiddenForLockscreen = false

    private fun showOverlay() {
        overlayHiddenForLockscreen = false

        // Check if views were removed from WindowManager during screen off/lock
        // and re-add them if necessary (similar to sidebar project approach)
        // Use isAttachedToWindow for more reliable state detection than windowToken
        if (overlayView?.isAttachedToWindow != true) addOverlayView()
        if (pillView?.isAttachedToWindow != true) addPillView()
        if (quickStripView?.isAttachedToWindow != true) addQuickStripView()

        overlayView?.visibility = View.VISIBLE
        pillView?.visibility = View.VISIBLE
        restoreQuickStripVisibility()
    }

    private fun hideOverlay() {
        overlayHiddenForLockscreen = true
        // Note: We intentionally do NOT set visibility to GONE here.
        // Setting views to GONE during screen off can cause WindowManager
        // to remove them on some Android versions/OEM skins, leading to
        // re-add race conditions on unlock. Just track the lockscreen state
        // and let the insets listener handle visibility when appropriate.
        // See: SidebarOverlayService for reference implementation.
    }

    private val lockscreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Just track that screen is off - don't manipulate visibility
                    // to avoid WindowManager issues on some Android versions
                    overlayHiddenForLockscreen = true
                }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                    // Simplified: just ensure views exist, let insets listener manage visibility
                    // This matches the sidebar project approach that works reliably
                    overlayHiddenForLockscreen = false
                    Handler(Looper.getMainLooper()).post {
                        if (overlayView?.isAttachedToWindow != true) addOverlayView()
                        if (pillView?.isAttachedToWindow != true) addPillView()
                        if (quickStripView?.isAttachedToWindow != true) addQuickStripView()
                        // Trigger a refresh of visibility based on current state
                        restoreQuickStripVisibility()
                    }
                }
                Intent.ACTION_CONFIGURATION_CHANGED -> {
                    if (taskbarViewModel.autoHideInLandscape.value) {
                        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        if (isLandscape) {
                            overlayView?.visibility = View.GONE
                            pillView?.visibility = View.GONE
                            quickStripView?.visibility = View.GONE
                            setQuickStripInteractive(false)
                        } else {
                            overlayView?.visibility = View.VISIBLE
                            pillView?.visibility = View.VISIBLE
                            restoreQuickStripVisibility()
                        }
                    }
                }
                ACTION_SETTINGS_OPEN -> {
                    taskbarViewModel.setSettingsOpen(true)
                    taskbarViewModel.showTaskbar()
                }
                ACTION_SETTINGS_CLOSE -> {
                    taskbarViewModel.setSettingsOpen(false)
                }
                ACTION_DISMISS_ALL -> {
                    if (this@OverlayService::appMenuViewModel.isInitialized) dismissAll()
                }
            }
        }
    }

    private fun dismissAll() {
        appMenuViewModel.dismissMenu()
        appMenuViewModel.dismissMusicPanel()
        taskbarViewModel.hideTaskbar()
    }

    private lateinit var taskbarViewModel: TaskbarViewModel
    private lateinit var appMenuViewModel: AppMenuViewModel

    private val overlayWindowType: Int
        get() = if (TaskBarAccessibilityService.isRunning())
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "taskbar_overlay_channel"
        const val ACTION_SETTINGS_OPEN = "com.alkisstam.taskbar.ACTION_SETTINGS_OPEN"
        const val ACTION_SETTINGS_CLOSE = "com.alkisstam.taskbar.ACTION_SETTINGS_CLOSE"
        const val ACTION_DISMISS_ALL = "com.alkisstam.taskbar.DISMISS_ALL"
    }

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        _windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            addAction(ACTION_SETTINGS_OPEN)
            addAction(ACTION_SETTINGS_CLOSE)
            addAction(ACTION_DISMISS_ALL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(lockscreenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(lockscreenReceiver, filter)
        }

        val factory = OverlayViewModelFactory(
            context = this,
            appRepository = appRepository,
            prefsRepository = prefsRepository,
            quickControlsRepository = quickControlsRepository,
            mediaRepository = mediaRepository
        )
        val provider = ViewModelProvider(this, factory)
        taskbarViewModel = provider[TaskbarViewModel::class.java]
        appMenuViewModel = provider[AppMenuViewModel::class.java]
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        addOverlayView()
        addPillView()
        addSearchView()
        addQuickStripView()
        addMusicPanelView()
        addVolumeScrimView()
        addVolumePanelView()
        addBrightnessPanelView()
        if (!observersStarted) {
            observersStarted = true
            observePillPosition()
            observeOverlayInteractivity()
            observeSearchVisibility()
            observeQuickStripVisibility()
            observeQuickStripPosition()
            observeMusicPanelPosition()
            observeVolumeAndBrightnessPanels()
            observeMusicPanelVisibility()
        }
        return START_STICKY
    }

    private fun overlayLayoutParams(interactive: Boolean = true, focusable: Boolean = false): WindowManager.LayoutParams {
        val usingAccessibility = TaskBarAccessibilityService.isRunning()
        val flags = (if (!focusable) WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0) or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                (if (!interactive) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0) or
                (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
        }
    }

    private fun quickStripLayoutParams(interactive: Boolean = false, yOffsetDp: Float = 0f): WindowManager.LayoutParams {
        val usingAccessibility = TaskBarAccessibilityService.isRunning()
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                (if (!interactive) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0) or
                (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
        val density = resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (yOffsetDp * density).toInt()
        }
    }

    private var quickStripYOffsetDp: Float = 0f
    private var quickStripInteractive: Boolean = false
    private var volumePanelYOffsetDp: Float = 0f
    private var musicPanelYOffsetDp: Float = 0f

    private fun setQuickStripInteractive(interactive: Boolean) {
        quickStripInteractive = interactive
        val view = quickStripView ?: return
        try { windowManager.updateViewLayout(view, quickStripLayoutParams(interactive, quickStripYOffsetDp)) }
        catch (e: Exception) { Log.w(TAG, "Failed to update quick strip layout flags", e) }
    }

    private fun restoreQuickStripVisibility() {
        val show = taskbarViewModel.isTaskbarVisible.value &&
                taskbarViewModel.quickControlsEnabled.value &&
                taskbarViewModel.quickControlsStripEnabled.value &&
                !appMenuViewModel.menuVisible.value &&
                !appMenuViewModel.isSearching.value
        quickStripView?.visibility = if (show) View.VISIBLE else View.GONE
        setQuickStripInteractive(show)
        if (!show) {
            appMenuViewModel.dismissVolumePanel()
            appMenuViewModel.dismissBrightnessPanel()
        }
    }

    private fun setOverlayFlags(interactive: Boolean, focusable: Boolean) {
        val view = overlayView ?: return
        try { windowManager.updateViewLayout(view, overlayLayoutParams(interactive, focusable)) }
        catch (e: Exception) { Log.w(TAG, "Failed to update overlay layout flags", e) }
    }

    private fun observeOverlayInteractivity() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                appMenuViewModel.menuVisible,
                taskbarViewModel.isTaskbarVisible,
                appMenuViewModel.isSearching,
                // Also consider quick strip visibility for interactivity
                taskbarViewModel.quickControlsEnabled,
                taskbarViewModel.quickControlsStripEnabled
            ) { menuOpen, taskbarVisible, searching, controlsEnabled, stripEnabled ->
                val stripVisible = taskbarVisible && controlsEnabled && stripEnabled && !menuOpen && !searching
                Triple(menuOpen || taskbarVisible || stripVisible, menuOpen && !searching, stripVisible)
            }
            .collect { (interactive, focusable, stripVisible) ->
                if (interactive) {
                    setOverlayFlags(interactive = true, focusable = focusable)
                } else {
                    kotlinx.coroutines.delay(Constants.OVERLAY_HIDE_DEBOUNCE_MS)
                    setOverlayFlags(interactive = false, focusable = false)
                }
            }
        }
    }

    private fun addOverlayView() {
        if (overlayView?.isAttachedToWindow == true) return
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    OverlayContent(
                        taskbarViewModel = taskbarViewModel,
                        appMenuViewModel = appMenuViewModel
                    )
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
            windowManager.addView(wrapper, overlayLayoutParams())
            attachInsetsListener(wrapper)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
            overlayView = null
        }
    }

    private fun attachInsetsListener(view: View) {
        // ViewCompat.setOnApplyWindowInsetsListener is dispatched from ViewRootImpl even
        // when the view is GONE, so the overlay can self-heal after the keyboard closes
        // rather than staying stuck in GONE state (the OnGlobalLayoutListener deadlock).
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            if (taskbarViewModel.autoHideInFullscreen.value) {
                val isFullscreen = !insets.isVisible(WindowInsetsCompat.Type.statusBars())
                if (isFullscreen) taskbarViewModel.hideTaskbar() else taskbarViewModel.showTaskbar()
            }
            if (!overlayHiddenForLockscreen) {
                if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                    // Make non-interactive so keyboard receives touches, but keep VISIBLE.
                    // Setting GONE would prevent the insets listener from firing on a GONE
                    // root view, permanently losing the overlay (deadlock).
                    setOverlayFlags(interactive = false, focusable = false)
                } else {
                    val menuOpen = appMenuViewModel.menuVisible.value
                    val taskbarVisible = taskbarViewModel.isTaskbarVisible.value
                    val searching = appMenuViewModel.isSearching.value
                    val controlsEnabled = taskbarViewModel.quickControlsEnabled.value
                    val stripEnabled = taskbarViewModel.quickControlsStripEnabled.value
                    val stripVisible = taskbarVisible && controlsEnabled && stripEnabled && !menuOpen && !searching
                    setOverlayFlags(
                        interactive = menuOpen || taskbarVisible || stripVisible,
                        focusable = menuOpen && !searching
                    )
                }
            }
            insets
        }
    }

    private fun pillLayoutParams(positionXDp: Float = 16f, positionYDp: Float = 80f): WindowManager.LayoutParams {
        val usingAccessibility = TaskBarAccessibilityService.isRunning()
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = (positionXDp * resources.displayMetrics.density).toInt()
            y = (positionYDp * resources.displayMetrics.density).toInt()
        }
    }

    private fun addPillView() {
        if (pillView?.isAttachedToWindow == true) return
        pillView?.let { runCatching { windowManager.removeView(it) } }
        pillView = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent { TriggerPillContent(taskbarViewModel = taskbarViewModel) }
            }
            pillView = composeView
            val initial = taskbarViewModel.pillSettings.value
            windowManager.addView(composeView, pillLayoutParams(initial.positionXDp, initial.positionYDp))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add pill view", e)
            pillView = null
        }
    }

    private fun observePillPosition() {
        serviceScope.launch {
            taskbarViewModel.pillSettings.collect { settings ->
                val view = pillView ?: return@collect
                try { windowManager.updateViewLayout(view, pillLayoutParams(settings.positionXDp, settings.positionYDp)) }
                catch (e: Exception) { Log.w(TAG, "Failed to update pill position", e) }
            }
        }
    }

    private fun searchLayoutParams(): WindowManager.LayoutParams {
        val usingAccessibility = TaskBarAccessibilityService.isRunning()
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
    }

    private fun addSearchView() {
        if (searchView?.isAttachedToWindow == true) return
        searchView?.let { runCatching { windowManager.removeView(it) } }
        searchView = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent { SearchOverlayContent(appMenuViewModel = appMenuViewModel, onHideTaskbar = taskbarViewModel::hideTaskbar) }
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
            searchView = wrapper
            windowManager.addView(wrapper, searchLayoutParams())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add search view", e)
            searchView = null
        }
    }

    private fun addQuickStripView() {
        if (quickStripView?.isAttachedToWindow == true) return
        quickStripView?.let { runCatching { windowManager.removeView(it) } }
        quickStripView = null
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    QuickStripContent(
                        taskbarViewModel = taskbarViewModel,
                        appMenuViewModel = appMenuViewModel,
                        onHideTaskbar = taskbarViewModel::hideTaskbar
                    )
                }
            }
            val initialSettings = taskbarViewModel.taskbarSettings.value
            quickStripYOffsetDp = initialSettings.positionYDp + initialSettings.heightDp + 2f
            composeView.visibility = View.GONE
            quickStripView = composeView
            windowManager.addView(composeView, quickStripLayoutParams(false, quickStripYOffsetDp))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add quick strip view", e)
            quickStripView = null
        }
    }

    private fun observeSearchVisibility() {
        serviceScope.launch {
            appMenuViewModel.isSearching.collect { searching ->
                searchView?.visibility = if (searching) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeQuickStripVisibility() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                taskbarViewModel.isTaskbarVisible,
                taskbarViewModel.quickControlsEnabled,
                taskbarViewModel.quickControlsStripEnabled,
                appMenuViewModel.menuVisible,
                appMenuViewModel.isSearching
            ) { values ->
                val taskbarVisible = values[0] as Boolean
                val controlsEnabled = values[1] as Boolean
                val stripEnabled = values[2] as Boolean
                val menuOpen = values[3] as Boolean
                val searching = values[4] as Boolean
                taskbarVisible && controlsEnabled && stripEnabled && !menuOpen && !searching
            }.collect { visible ->
                quickStripView?.visibility = if (visible) View.VISIBLE else View.GONE
                setQuickStripInteractive(visible)
            }
        }
    }

    private fun observeQuickStripPosition() {
        serviceScope.launch {
            taskbarViewModel.taskbarSettings.collect { settings ->
                quickStripYOffsetDp = settings.positionYDp + settings.heightDp + 2f
                volumePanelYOffsetDp = settings.positionYDp + settings.heightDp * 2 + 10f
                val view = quickStripView ?: return@collect
                try { windowManager.updateViewLayout(view, quickStripLayoutParams(quickStripInteractive, quickStripYOffsetDp)) }
                catch (e: Exception) { Log.w(TAG, "Failed to update quick strip position", e) }
            }
        }
    }

    private fun observeMusicPanelPosition() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                taskbarViewModel.taskbarSettings,
                taskbarViewModel.quickControlsStripEnabled,
                taskbarViewModel.quickControlsEnabled,
                appMenuViewModel.menuVisible
            ) { settings, stripEnabled, controlsEnabled, menuOpen ->
                val stripActive = stripEnabled && controlsEnabled
                when {
                    menuOpen -> settings.positionYDp + settings.heightDp + 420f
                    stripActive -> settings.positionYDp + settings.heightDp + 80f
                    else -> settings.positionYDp + settings.heightDp + 8f
                }
            }.collect { yOffset ->
                musicPanelYOffsetDp = yOffset
                val view = musicPanelView ?: return@collect
                try { windowManager.updateViewLayout(view, musicPanelLayoutParams(musicPanelYOffsetDp)) }
                catch (e: Exception) { Log.w(TAG, "Failed to update music panel position", e) }
            }
        }
    }

    private fun volumePanelLayoutParams(yOffsetDp: Float): WindowManager.LayoutParams {
        val usingAccessibility = TaskBarAccessibilityService.isRunning()
        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
        val density = resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (yOffsetDp * density).toInt()
        }
    }

    private fun volumeScrimLayoutParams(): WindowManager.LayoutParams {
        val usingAccessibility = TaskBarAccessibilityService.isRunning()
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun addVolumeScrimView() {
        if (volumeScrimView?.isAttachedToWindow == true) return
        volumeScrimView?.let { runCatching { windowManager.removeView(it) } }
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
        volumePanelView?.let { runCatching { windowManager.removeView(it) } }
        volumePanelView = null
        try {
            val initialSettings = taskbarViewModel.taskbarSettings.value
            volumePanelYOffsetDp = initialSettings.positionYDp + initialSettings.heightDp * 2 + 10f
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
            windowManager.addView(composeView, volumePanelLayoutParams(volumePanelYOffsetDp))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add volume panel view", e)
            volumePanelView = null
        }
    }

    private fun addBrightnessPanelView() {
        if (brightnessPanelView?.isAttachedToWindow == true) return
        brightnessPanelView?.let { runCatching { windowManager.removeView(it) } }
        brightnessPanelView = null
        try {
            val initialSettings = taskbarViewModel.taskbarSettings.value
            val yOffset = initialSettings.positionYDp + initialSettings.heightDp * 2 + 10f
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
            windowManager.addView(composeView, volumePanelLayoutParams(yOffset))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add brightness panel view", e)
            brightnessPanelView = null
        }
    }

    private fun musicPanelLayoutParams(yOffsetDp: Float): WindowManager.LayoutParams {
        val usingAccessibility = TaskBarAccessibilityService.isRunning()
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
        val density = resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (yOffsetDp * density).toInt()
        }
    }

    private fun addMusicPanelView() {
        if (musicPanelView?.isAttachedToWindow == true) return
        musicPanelView?.let { runCatching { windowManager.removeView(it) } }
        musicPanelView = null
        try {
            val initialSettings = taskbarViewModel.taskbarSettings.value
            musicPanelYOffsetDp = initialSettings.positionYDp + initialSettings.heightDp + 80f
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
            windowManager.addView(composeView, musicPanelLayoutParams(musicPanelYOffsetDp))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add music panel view", e)
            musicPanelView = null
        }
    }

    private fun observeMusicPanelVisibility() {
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                appMenuViewModel.mediaState,
                taskbarViewModel.musicPanelEnabled,
                taskbarViewModel.isTaskbarVisible,
                appMenuViewModel.musicPanelVisible,
                appMenuViewModel.isSearching
            ) { values ->
                val state = values[0] as com.alkisstam.taskbar.data.MediaState
                val enabled = values[1] as Boolean
                val taskbarVisible = values[2] as Boolean
                val userVisible = values[3] as Boolean
                val searching = values[4] as Boolean
                enabled && state.hasSession && taskbarVisible && userVisible && !searching
            }.collect { show ->
                musicPanelView?.visibility = if (show) View.VISIBLE else View.GONE
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
                // Manage scrim visibility atomically based on either panel being visible
                volumeScrimView?.visibility = if (volumeVisible || brightnessVisible) View.VISIBLE else View.GONE

                // Update volume panel
                volumePanelView?.visibility = if (volumeVisible) View.VISIBLE else View.GONE
                if (volumeVisible) {
                    val view = volumePanelView ?: return@collect
                    try { windowManager.updateViewLayout(view, volumePanelLayoutParams(volumePanelYOffsetDp)) }
                    catch (e: Exception) { Log.w(TAG, "Failed to update volume panel position", e) }
                }

                // Update brightness panel
                brightnessPanelView?.visibility = if (brightnessVisible) View.VISIBLE else View.GONE
                if (brightnessVisible) {
                    val view = brightnessPanelView ?: return@collect
                    try { windowManager.updateViewLayout(view, volumePanelLayoutParams(volumePanelYOffsetDp)) }
                    catch (e: Exception) { Log.w(TAG, "Failed to update brightness panel position", e) }
                }
            }
        }
    }

    private fun removeOverlayView() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, "Failed to remove overlay view", e) }
            overlayView = null
        }
        pillView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, "Failed to remove pill view", e) }
            pillView = null
        }
        searchView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, "Failed to remove search view", e) }
            searchView = null
        }
        quickStripView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, "Failed to remove quick strip view", e) }
            quickStripView = null
        }
        volumePanelView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, "Failed to remove volume panel view", e) }
            volumePanelView = null
        }
        brightnessPanelView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, "Failed to remove brightness panel view", e) }
            brightnessPanelView = null
        }
        volumeScrimView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, "Failed to remove volume scrim view", e) }
            volumeScrimView = null
        }
        musicPanelView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { Log.w(TAG, "Failed to remove music panel view", e) }
            musicPanelView = null
        }
    }

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

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        observersStarted = false
        unregisterReceiver(lockscreenReceiver)
        removeOverlayView()
        serviceScope.cancel()
        _viewModelStore.clear()
        appRepository.cleanup()
        quickControlsRepository.cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
private fun OverlayContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()
    val isSettingsOpen by taskbarViewModel.isSettingsOpen.collectAsState()

    TaskBarTheme(themeMode = themeMode) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (isTaskbarVisible && !isSettingsOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (menuVisible) appMenuViewModel.dismissMenu()
                            else taskbarViewModel.hideTaskbar()
                        }
                )
            }
            Column(modifier = Modifier.wrapContentHeight()) {
                AppMenuPanel(
                    viewModel = appMenuViewModel,
                    taskbarViewModel = taskbarViewModel,
                    onHideTaskbar = taskbarViewModel::hideTaskbar,
                    modifier = Modifier
                )
                AnimatedVisibility(
                    visible = isTaskbarVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    TaskbarView(
                        taskbarViewModel = taskbarViewModel,
                        appMenuViewModel = appMenuViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun TriggerPillContent(taskbarViewModel: TaskbarViewModel) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val pillSettings by taskbarViewModel.pillSettings.collectAsState()

    TaskBarTheme(themeMode = themeMode) {
        TriggerPillView(
            isCollapsed = !isTaskbarVisible,
            pillSettings = pillSettings,
            onExpand = { taskbarViewModel.showTaskbar() }
        )
    }
}

@Composable
private fun SearchOverlayContent(appMenuViewModel: AppMenuViewModel, onHideTaskbar: () -> Unit) {
    TaskBarTheme {
        FloatingSearchBar(viewModel = appMenuViewModel, onHideTaskbar = onHideTaskbar)
    }
}

@Composable
private fun QuickStripContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel,
    onHideTaskbar: () -> Unit = {}
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    TaskBarTheme(themeMode = themeMode) {
        QuickStripView(
            taskbarViewModel = taskbarViewModel,
            appMenuViewModel = appMenuViewModel,
            onHideTaskbar = onHideTaskbar
        )
    }
}

@Composable
private fun VolumePanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val streams by appMenuViewModel.volumeStreams.collectAsState()
    TaskBarTheme(themeMode = themeMode) {
        VolumePanel(
            streams = streams,
            onVolumeChange = { streamType, value ->
                appMenuViewModel.setStreamVolume(streamType, value)
            }
        )
    }
}

@Composable
private fun BrightnessPanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val brightnessLevel by appMenuViewModel.brightnessLevel.collectAsState()
    val quickControlsState by appMenuViewModel.quickControlsState.collectAsState()
    TaskBarTheme(themeMode = themeMode) {
        BrightnessPanel(
            brightnessLevel = brightnessLevel,
            onBrightnessChange = { value -> appMenuViewModel.setBrightnessLevel(value) },
            autoBrightnessEnabled = quickControlsState.autoBrightness,
            onAutoBrightnessToggle = { appMenuViewModel.toggleAutoBrightness() }
        )
    }
}

@Composable
private fun MusicPanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val mediaState by appMenuViewModel.mediaState.collectAsState()
    TaskBarTheme(themeMode = themeMode) {
        MusicPanel(
            mediaState = mediaState,
            onPlayPause = appMenuViewModel::playPause,
            onNext = appMenuViewModel::nextTrack,
            onPrev = appMenuViewModel::prevTrack
        )
    }
}
