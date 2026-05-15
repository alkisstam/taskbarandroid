package com.taskbar.app.service

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
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
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
import com.taskbar.app.MainActivity
import com.taskbar.app.R
import com.taskbar.app.data.AppRepository
import com.taskbar.app.util.Constants
import com.taskbar.app.data.PreferencesRepository
import com.taskbar.app.data.QuickControlsRepository
import com.taskbar.app.ui.appmenu.AppMenuPanel
import com.taskbar.app.ui.appmenu.FloatingSearchBar
import com.taskbar.app.ui.taskbar.QuickStripView
import com.taskbar.app.ui.taskbar.TaskbarView
import com.taskbar.app.ui.taskbar.TriggerPillView
import com.taskbar.app.ui.theme.TaskBarTheme
import com.taskbar.app.viewmodel.AppMenuViewModel
import com.taskbar.app.viewmodel.TaskbarViewModel
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
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observersStarted = false

    private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    private val handler = Handler(Looper.getMainLooper())

    // True while the screen is off or the lockscreen is showing; the keyboard
    // visibility observer must not show the overlay while this is set.
    @Volatile private var overlayHiddenForLockscreen = false

    private fun showOverlay() {
        overlayHiddenForLockscreen = false
        overlayView?.visibility = View.VISIBLE
        pillView?.visibility = View.VISIBLE
        restoreQuickStripVisibility()
    }

    private val lockscreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    handler.removeCallbacksAndMessages(null)
                    overlayHiddenForLockscreen = true
                    overlayView?.visibility = View.GONE
                    pillView?.visibility = View.GONE
                    searchView?.visibility = View.GONE
                    quickStripView?.visibility = View.GONE
                    setQuickStripInteractive(false)
                }
                Intent.ACTION_USER_PRESENT -> {
                    handler.removeCallbacksAndMessages(null)
                    showOverlay()
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Fallback for devices where ACTION_USER_PRESENT fires late or not at all
                    // (e.g. no screen lock set). ACTION_USER_PRESENT cancels this if it arrives first.
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({
                        if (overlayHiddenForLockscreen && !keyguardManager.isKeyguardLocked) {
                            showOverlay()
                        }
                    }, 300)
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
            }
        }
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
        const val ACTION_SETTINGS_OPEN = "com.taskbar.app.ACTION_SETTINGS_OPEN"
        const val ACTION_SETTINGS_CLOSE = "com.taskbar.app.ACTION_SETTINGS_CLOSE"
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
        }
        registerReceiver(lockscreenReceiver, filter, RECEIVER_NOT_EXPORTED)

        val factory = OverlayViewModelFactory(
            context = this,
            appRepository = appRepository,
            prefsRepository = prefsRepository,
            quickControlsRepository = quickControlsRepository
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
        if (!observersStarted) {
            observersStarted = true
            observeKeyboardVisibility()
            observePillPosition()
            observeOverlayInteractivity()
            observeSearchVisibility()
            observeQuickStripVisibility()
            observeQuickStripPosition()
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

    private fun setQuickStripInteractive(interactive: Boolean) {
        quickStripInteractive = interactive
        val view = quickStripView ?: return
        try { windowManager.updateViewLayout(view, quickStripLayoutParams(interactive, quickStripYOffsetDp)) }
        catch (e: Exception) { Log.w(TAG, "Failed to update quick strip layout flags", e) }
    }

    private fun restoreQuickStripVisibility() {
        val show = taskbarViewModel.isTaskbarVisible.value &&
                taskbarViewModel.quickControlsStripEnabled.value &&
                !appMenuViewModel.menuVisible.value &&
                !appMenuViewModel.isSearching.value
        quickStripView?.visibility = if (show) View.VISIBLE else View.GONE
        setQuickStripInteractive(show)
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
                appMenuViewModel.isSearching
            ) { menuOpen, taskbarVisible, searching ->
                Triple(menuOpen, taskbarVisible, searching)
            }
            .collect { (menuOpen, taskbarVisible, searching) ->
                val interactive = menuOpen || taskbarVisible
                if (interactive) {
                    setOverlayFlags(interactive = true, focusable = menuOpen && !searching)
                } else {
                    kotlinx.coroutines.delay(Constants.OVERLAY_HIDE_DEBOUNCE_MS)
                    setOverlayFlags(interactive = false, focusable = false)
                }
            }
        }
    }

    private fun addOverlayView() {
        if (overlayView != null) return
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
            overlayView = composeView
            windowManager.addView(composeView, overlayLayoutParams())
            attachFullscreenObserver(composeView)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
            overlayView = null
        }
    }

    private fun observeKeyboardVisibility() {
        val view = overlayView ?: return
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val v = overlayView ?: return@OnGlobalLayoutListener
            if (overlayHiddenForLockscreen) return@OnGlobalLayoutListener
            val rect = Rect()
            v.getWindowVisibleDisplayFrame(rect)
            val screenHeight = v.rootView?.height ?: return@OnGlobalLayoutListener
            val keypadHeight = screenHeight - rect.bottom
            val keyboardVisible = keypadHeight > screenHeight * Constants.KEYBOARD_VISIBLE_THRESHOLD
            v.visibility = if (keyboardVisible) View.GONE else View.VISIBLE
        }
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
            }
            override fun onViewDetachedFromWindow(v: View) {
                v.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            }
        })
        if (view.isAttachedToWindow) {
            view.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
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
        if (pillView != null) return
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
        if (searchView != null) return
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
        if (quickStripView != null) return
        try {
            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    QuickStripContent(
                        taskbarViewModel = taskbarViewModel,
                        appMenuViewModel = appMenuViewModel
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
                taskbarViewModel.quickControlsStripEnabled,
                appMenuViewModel.menuVisible,
                appMenuViewModel.isSearching
            ) { taskbarVisible, stripEnabled, menuOpen, searching ->
                taskbarVisible && stripEnabled && !menuOpen && !searching
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
                val view = quickStripView ?: return@collect
                try { windowManager.updateViewLayout(view, quickStripLayoutParams(quickStripInteractive, quickStripYOffsetDp)) }
                catch (e: Exception) { Log.w(TAG, "Failed to update quick strip position", e) }
            }
        }
    }

    private var fullscreenInsetsListener: View.OnApplyWindowInsetsListener? = null

    private fun attachFullscreenObserver(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        fullscreenInsetsListener = View.OnApplyWindowInsetsListener { v, insets ->
            if (taskbarViewModel.autoHideInFullscreen.value) {
                val isFullscreen = !insets.isVisible(android.view.WindowInsets.Type.statusBars())
                if (isFullscreen) {
                    taskbarViewModel.hideTaskbar()
                } else {
                    taskbarViewModel.showTaskbar()
                }
            }
            insets
        }
        view.setOnApplyWindowInsetsListener(fullscreenInsetsListener)
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
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    TaskBarTheme(themeMode = themeMode) {
        QuickStripView(
            taskbarViewModel = taskbarViewModel,
            appMenuViewModel = appMenuViewModel
        )
    }
}
