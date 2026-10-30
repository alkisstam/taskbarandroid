package com.alkisstam.taskbar.service

import android.view.WindowManager
import android.graphics.Point
import android.os.Build
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.data.AppMenuButtonSide
import com.alkisstam.taskbar.data.bottomGapDp
import com.alkisstam.taskbar.data.widthFraction
import com.alkisstam.taskbar.data.GestureAction
import com.alkisstam.taskbar.data.PillHoldDragAction
import com.alkisstam.taskbar.ui.appmenu.AppMenuPanel
import com.alkisstam.taskbar.ui.appmenu.BrightnessPanel
import com.alkisstam.taskbar.ui.appmenu.CalculatorPanel
import com.alkisstam.taskbar.ui.appmenu.FloatingSearchBar
import com.alkisstam.taskbar.ui.appmenu.MusicPanel
import com.alkisstam.taskbar.ui.appmenu.VolumePanel
import com.alkisstam.taskbar.ui.clipboard.ClipboardPanel
import com.alkisstam.taskbar.ui.clipboard.NotesPanel
import com.alkisstam.taskbar.ui.notifications.NotificationHistoryPanel
import com.alkisstam.taskbar.ui.settings.QuickSettingsPanel
import com.alkisstam.taskbar.ui.taskbar.QuickControlsButtonGap
import com.alkisstam.taskbar.ui.taskbar.TaskbarView
import com.alkisstam.taskbar.ui.taskbar.TriggerPillView
import com.alkisstam.taskbar.ui.theme.TaskBarTheme
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.ClipboardViewModel
import com.alkisstam.taskbar.viewmodel.LevelIndicator
import com.alkisstam.taskbar.viewmodel.NotificationHistoryViewModel
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import kotlin.math.roundToInt

private fun dockAwarePanelBottomPadding(isTaskbarVisible: Boolean, heightDp: Float, expandedRows: Int, bottomGapDp: Float) =
    if (isTaskbarVisible) (bottomGapDp + heightDp * (1 + expandedRows) + 4f * expandedRows + 16f + 18f).dp else 0.dp

@Composable
internal fun OverlayContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val navBarInsetDp by taskbarViewModel.navBarInsetDp.collectAsState()
    val statusBarInsetDp by taskbarViewModel.statusBarInsetDp.collectAsState()
    // Accessibility overlays get zero dispatched insets, so the status bar/cutout height comes
    // from WindowManager metrics; the floor covers devices where neither reports anything.
    val topInset = maxOf(WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), statusBarInsetDp.dp, 32.dp)
    val bottomInset = maxOf(taskbarSettings.dockPadding.bottomGapDp, navBarInsetDp).dp
    val edgeGap = maxOf(topInset + 8.dp, bottomInset + 12.dp)
    // The window may not span the whole screen (e.g. it starts below the status bar or ends
    // above the nav bar), so shift the bottom padding by both offsets to keep the gaps equal.
    val view = LocalView.current
    val density = LocalDensity.current
    var windowTopOffset by remember { mutableStateOf(0.dp) }
    var windowBottomOffset by remember { mutableStateOf(0.dp) }

    TaskBarTheme(themeMode = themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    val screenHeightPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds.height()
                    } else {
                        Point().also { @Suppress("DEPRECATION") view.display?.getRealSize(it) }.y
                    }
                    windowTopOffset = with(density) { location[1].toDp() }
                    windowBottomOffset = with(density) {
                        (screenHeightPx - location[1] - view.height).coerceAtLeast(0).toDp()
                    }
                }
        ) {
            if (menuVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { appMenuViewModel.dismissMenu() }
                )
            }
            AppMenuPanel(
                viewModel = appMenuViewModel,
                taskbarViewModel = taskbarViewModel,
                onHideTaskbar = taskbarViewModel::hideTaskbar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = edgeGap, bottom = (edgeGap + windowTopOffset - windowBottomOffset).coerceAtLeast(0.dp))
            )
        }
    }
}

@Composable
internal fun TaskbarContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val dockRevealProgress by taskbarViewModel.dockRevealProgress.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val navBarInsetDp by taskbarViewModel.navBarInsetDp.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()
    val dockBottomGap = maxOf(taskbarSettings.dockPadding.bottomGapDp, navBarInsetDp).dp

    val revealAnim = remember { Animatable(0f) }
    var taskbarHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(menuVisible) {
        if (menuVisible) taskbarViewModel.collapseDock()
    }

    // The fullscreen app menu covers the dock; it slides back once the menu closes.
    LaunchedEffect(isTaskbarVisible, dockRevealProgress, menuVisible) {
        when {
            menuVisible ->
                revealAnim.animateTo(0f, tween(150))
            isTaskbarVisible ->
                revealAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            dockRevealProgress == 0f ->
                revealAnim.animateTo(0f, tween(220))
            else ->
                revealAnim.snapTo(dockRevealProgress)
        }
    }

    TaskBarTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (revealAnim.value > 0.001f) {
                // A pinned dock's wrapper ignores outside taps, so the scrim closes the panel itself.
                if (isDockExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { taskbarViewModel.hideTaskbar() }
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = dockBottomGap)
                ) {
                    TaskbarView(
                        taskbarViewModel = taskbarViewModel,
                        appMenuViewModel = appMenuViewModel,
                        modifier = Modifier
                            .onGloballyPositioned { taskbarHeightPx = it.size.height.toFloat() }
                            .graphicsLayer {
                                translationY = taskbarHeightPx * (1f - revealAnim.value)
                                alpha = revealAnim.value
                            }
                    )
                }
            }
        }
    }
}

@Composable
internal fun TriggerPillContent(taskbarViewModel: TaskbarViewModel, appMenuViewModel: AppMenuViewModel) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val pillSettings by taskbarViewModel.pillSettings.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val pillHiddenForScreenshot by appMenuViewModel.pillHiddenForScreenshot.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val dockRevealMaxDragPx = remember(taskbarSettings.heightDp, density) {
        with(density) { taskbarSettings.heightDp.dp.toPx() }
    }
    var holdBase by remember { mutableIntStateOf(-1) }
    var holdMax by remember { mutableIntStateOf(0) }
    var holdLast by remember { mutableIntStateOf(-1) }

    TaskBarTheme(themeMode = themeMode) {
        TriggerPillView(
            isCollapsed = !isTaskbarVisible && !pillHiddenForScreenshot,
            pillSettings = pillSettings,
            dockRevealMaxDragPx = dockRevealMaxDragPx,
            onRevealProgress = { taskbarViewModel.setRevealProgress(it) },
            onRevealCommit = { taskbarViewModel.showTaskbar() },
            onRevealCancel = { taskbarViewModel.cancelReveal() },
            onHoldDragStart = {
                holdLast = -1
                when (pillSettings.holdDragAction) {
                    PillHoldDragAction.BRIGHTNESS -> {
                        holdBase = appMenuViewModel.beginBrightnessDrag() ?: -1
                        holdMax = 255
                        if (holdBase >= 0) appMenuViewModel.showLevelIndicator(true, holdBase, holdMax)
                    }
                    PillHoldDragAction.VOLUME -> {
                        val (current, max) = appMenuViewModel.musicVolumeSnapshot()
                        holdBase = current
                        holdMax = max
                        appMenuViewModel.showLevelIndicator(false, current, max)
                    }
                    PillHoldDragAction.DISABLED -> holdBase = -1
                }
            },
            onHoldDrag = { upwardPx ->
                if (holdBase >= 0) {
                    val span = context.resources.displayMetrics.heightPixels * 0.6f
                    val min = if (pillSettings.holdDragAction == PillHoldDragAction.BRIGHTNESS) 1 else 0
                    val target = (holdBase + upwardPx / span * holdMax).roundToInt().coerceIn(min, holdMax)
                    if (target != holdLast) {
                        holdLast = target
                        val isBrightness = pillSettings.holdDragAction == PillHoldDragAction.BRIGHTNESS
                        if (isBrightness) appMenuViewModel.setBrightnessLevel(target)
                        else appMenuViewModel.setStreamVolume(AudioManager.STREAM_MUSIC, target)
                        appMenuViewModel.showLevelIndicator(isBrightness, target, holdMax)
                    }
                }
            },
            onHoldDragEnd = { appMenuViewModel.hideLevelIndicatorSoon() },
            onAction = { action ->
                when (action) {
                    GestureAction.SHOW_DOCK -> taskbarViewModel.showTaskbar()
                    GestureAction.SHOW_NOTIFICATIONS -> {
                        val svc = TaskBarAccessibilityService.instance
                        if (svc != null) svc.expandNotifications()
                        else Toast.makeText(context, context.getString(R.string.pill_action_requires_accessibility), Toast.LENGTH_SHORT).show()
                    }
                    GestureAction.SHOW_QUICK_SETTINGS -> {
                        val svc = TaskBarAccessibilityService.instance
                        if (svc != null) svc.expandQuickSettings()
                        else Toast.makeText(context, context.getString(R.string.pill_action_requires_accessibility), Toast.LENGTH_SHORT).show()
                    }
                    GestureAction.POWER_MENU -> {
                        val svc = TaskBarAccessibilityService.instance
                        if (svc != null) svc.showPowerMenu()
                        else Toast.makeText(context, context.getString(R.string.pill_action_requires_accessibility), Toast.LENGTH_SHORT).show()
                    }
                    GestureAction.TAKE_SCREENSHOT -> {
                        val svc = TaskBarAccessibilityService.instance
                        if (svc != null) appMenuViewModel.captureScreenshotHidingPill { svc.takeScreenshot() }
                        else Toast.makeText(context, context.getString(R.string.pill_action_requires_accessibility), Toast.LENGTH_SHORT).show()
                    }
                    GestureAction.LOCK_SCREEN -> {
                        val svc = TaskBarAccessibilityService.instance
                        if (svc != null) svc.lockScreen()
                        else Toast.makeText(context, context.getString(R.string.pill_action_requires_accessibility), Toast.LENGTH_SHORT).show()
                    }
                    GestureAction.TOGGLE_FLASHLIGHT -> appMenuViewModel.toggleTorch()
                    GestureAction.SHOW_APP_MENU -> {
                        if (!appMenuViewModel.menuVisible.value) appMenuViewModel.toggleMenu()
                    }
                    GestureAction.SHOW_SEARCH -> {
                        appMenuViewModel.openSearch()
                    }
                    GestureAction.DISABLED -> {}
                }
            }
        )
    }
}

@Composable
internal fun LevelIndicatorContent(taskbarViewModel: TaskbarViewModel, appMenuViewModel: AppMenuViewModel) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val indicator by appMenuViewModel.levelIndicator.collectAsState()
    var shown by remember { mutableStateOf<LevelIndicator?>(null) }
    if (indicator != null) shown = indicator
    TaskBarTheme(themeMode = themeMode) {
        AnimatedVisibility(visible = indicator != null, enter = fadeIn(), exit = fadeOut()) {
            shown?.let { level ->
                val fraction = if (level.max > 0) level.level.toFloat() / level.max else 0f
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (level.isBrightness) Icons.Filled.BrightnessMedium else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.padding(horizontal = 12.dp).width(140.dp),
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "${(fraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchOverlayContent(
    appMenuViewModel: AppMenuViewModel,
    taskbarViewModel: TaskbarViewModel,
    onHideTaskbar: () -> Unit
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    TaskBarTheme(themeMode = themeMode) {
        FloatingSearchBar(
            viewModel = appMenuViewModel,
            onHideTaskbar = onHideTaskbar,
            translucentMode = translucentMode,
            translucentAlpha = translucentAlpha,
            grainAlpha = grainAlpha,
            surfaceTintColor = surfaceTintColor,
            iconShape = taskbarSettings.iconShape
        )
    }
}

@Composable
internal fun VolumePanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val streams by appMenuViewModel.volumeStreams.collectAsState()
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    TaskBarTheme(themeMode = themeMode) {
        VolumePanel(
            streams = streams,
            onVolumeChange = { streamType, value ->
                appMenuViewModel.setStreamVolume(streamType, value)
            },
            panelOutlineEnabled = panelOutlineEnabled,
            translucentMode = translucentMode,
            translucentAlpha = translucentAlpha,
            grainAlpha = grainAlpha,
            surfaceTintColor = surfaceTintColor
        )
    }
}

@Composable
internal fun BrightnessPanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val brightnessLevel by appMenuViewModel.brightnessLevel.collectAsState()
    val quickControlsState by appMenuViewModel.quickControlsState.collectAsState()
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    TaskBarTheme(themeMode = themeMode) {
        BrightnessPanel(
            brightnessLevel = brightnessLevel,
            onBrightnessChange = { value -> appMenuViewModel.setBrightnessLevel(value) },
            autoBrightnessEnabled = quickControlsState.autoBrightness,
            onAutoBrightnessToggle = { appMenuViewModel.toggleAutoBrightness() },
            panelOutlineEnabled = panelOutlineEnabled,
            translucentMode = translucentMode,
            translucentAlpha = translucentAlpha,
            grainAlpha = grainAlpha,
            surfaceTintColor = surfaceTintColor
        )
    }
}

@Composable
internal fun ClipboardPanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel,
    clipboardViewModel: ClipboardViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()

    val expandedRows = if (isDockExpanded) 1 else 0
    val dockBottomPadding = dockAwarePanelBottomPadding(isTaskbarVisible, taskbarSettings.heightDp, expandedRows, taskbarSettings.dockPadding.bottomGapDp)

    TaskBarTheme(themeMode = themeMode) {
        ClipboardPanel(
            viewModel = clipboardViewModel,
            onDismiss = appMenuViewModel::dismissClipboardPanel,
            onOpenExternal = {
                appMenuViewModel.dismissClipboardPanelForExternalOpen()
                taskbarViewModel.hideTaskbar()
                appMenuViewModel.dismissMusicPanel()
            },
            panelOutlineEnabled = panelOutlineEnabled,
            translucentMode = translucentMode,
            translucentAlpha = translucentAlpha,
            grainAlpha = grainAlpha,
            surfaceTintColor = surfaceTintColor,
            dockBottomPadding = dockBottomPadding
        )
    }
}

@Composable
internal fun NotesPanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel,
    clipboardViewModel: ClipboardViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()

    val expandedRows = if (isDockExpanded) 1 else 0
    val dockBottomPadding = dockAwarePanelBottomPadding(isTaskbarVisible, taskbarSettings.heightDp, expandedRows, taskbarSettings.dockPadding.bottomGapDp)

    TaskBarTheme(themeMode = themeMode) {
        NotesPanel(
            viewModel = clipboardViewModel,
            onDismiss = appMenuViewModel::dismissNotesPanel,
            onOpenExternal = {
                appMenuViewModel.dismissNotesPanelForExternalOpen()
                taskbarViewModel.hideTaskbar()
                appMenuViewModel.dismissMusicPanel()
            },
            panelOutlineEnabled = panelOutlineEnabled,
            translucentMode = translucentMode,
            translucentAlpha = translucentAlpha,
            grainAlpha = grainAlpha,
            surfaceTintColor = surfaceTintColor,
            dockBottomPadding = dockBottomPadding
        )
    }
}

private fun notificationListenerEnabled(context: android.content.Context): Boolean {
    val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    val cn = android.content.ComponentName(context, MediaListenerService::class.java)
    return flat.split(":").any { android.content.ComponentName.unflattenFromString(it.trim()) == cn }
}

@Composable
internal fun QuickSettingsPanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()
    val quickSettingsPanelVisible by appMenuViewModel.quickSettingsPanelVisible.collectAsState()

    val expandedRows = if (isDockExpanded) 1 else 0
    val dockBottomPadding = dockAwarePanelBottomPadding(isTaskbarVisible, taskbarSettings.heightDp, expandedRows, taskbarSettings.dockPadding.bottomGapDp)
    val context = LocalContext.current
    val hasNotificationListenerPermission = remember(quickSettingsPanelVisible) {
        notificationListenerEnabled(context)
    }

    TaskBarTheme(themeMode = themeMode) {
        QuickSettingsPanel(
            taskbarViewModel = taskbarViewModel,
            hasNotificationListenerPermission = hasNotificationListenerPermission,
            onDismiss = appMenuViewModel::dismissQuickSettingsPanel,
            panelOutlineEnabled = panelOutlineEnabled,
            translucentMode = translucentMode,
            translucentAlpha = translucentAlpha,
            grainAlpha = grainAlpha,
            surfaceTintColor = surfaceTintColor,
            dockBottomPadding = dockBottomPadding
        )
    }
}

@Composable
internal fun NotificationHistoryPanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel,
    notificationHistoryViewModel: NotificationHistoryViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()
    val allApps by appMenuViewModel.allApps.collectAsState()

    val appsByPackage = remember(allApps) { allApps.associateBy { it.packageName } }

    val expandedRows = if (isDockExpanded) 1 else 0
    val dockBottomPadding = dockAwarePanelBottomPadding(isTaskbarVisible, taskbarSettings.heightDp, expandedRows, taskbarSettings.dockPadding.bottomGapDp)

    TaskBarTheme(themeMode = themeMode) {
        NotificationHistoryPanel(
            viewModel = notificationHistoryViewModel,
            iconForPackage = { pkg -> appsByPackage[pkg]?.icon },
            onDismiss = appMenuViewModel::dismissNotificationPanel,
            onOpenApp = { pkg ->
                appMenuViewModel.launchApp(pkg)
                appMenuViewModel.dismissNotificationPanel()
                taskbarViewModel.hideTaskbar()
            },
            panelOutlineEnabled = panelOutlineEnabled,
            translucentMode = translucentMode,
            translucentAlpha = translucentAlpha,
            grainAlpha = grainAlpha,
            surfaceTintColor = surfaceTintColor,
            dockBottomPadding = dockBottomPadding,
            iconShape = taskbarSettings.iconShape
        )
    }
}

@Composable
internal fun MusicPanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val mediaState by appMenuViewModel.mediaState.collectAsState()
    val musicPanelEnabled by taskbarViewModel.musicPanelEnabled.collectAsState()
    val musicPanelVisible by appMenuViewModel.musicPanelVisible.collectAsState()
    val alwaysShowMusicPanel by appMenuViewModel.alwaysShowMusicPanel.collectAsState()
    val isSearching by appMenuViewModel.isSearching.collectAsState()
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()

    val dockRevealProgress by taskbarViewModel.dockRevealProgress.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()

    val panelShouldShow = musicPanelEnabled && (mediaState.hasSession || alwaysShowMusicPanel) && musicPanelVisible && !isSearching && !menuVisible

    val revealAnim = remember { Animatable(0f) }
    var panelWidthPx by remember { mutableFloatStateOf(0f) }

    // Mirrors the dock's reveal spring in OverlayContent, so the panel can wait for the quick
    // controls button to rise into place before sliding out from behind it.
    val dockAnim = remember { Animatable(0f) }
    LaunchedEffect(isTaskbarVisible, dockRevealProgress) {
        when {
            isTaskbarVisible ->
                dockAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            dockRevealProgress == 0f ->
                dockAnim.animateTo(0f, tween(220))
            else ->
                dockAnim.snapTo(dockRevealProgress)
        }
    }

    LaunchedEffect(panelShouldShow, isTaskbarVisible) {
        if (panelShouldShow && isTaskbarVisible) {
            snapshotFlow { dockAnim.value }.first { it >= 0.98f }
            revealAnim.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
        } else if (!isTaskbarVisible) {
            // Its window can't follow the button down, so don't leave it floating there.
            revealAnim.snapTo(0f)
        } else {
            revealAnim.animateTo(0f, tween(220))
        }
    }

    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val pillIconCount by taskbarViewModel.pillIconCount.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()
    // Sits beside a short (1-2 icon) pill instead of covering it; OverlayService lowers
    // the window to the pill's row in the same state. A longer pill leaves no room, so it goes above.
    val pillVisible by taskbarViewModel.pillVisible.collectAsState()
    val besideButton = pillVisible && pillIconCount in 1..2 && !isDockExpanded
    val buttonBounds by taskbarViewModel.quickControlsButtonBounds.collectAsState()
    // Bounds clear while the dock is hidden; keep the last width so the inset doesn't grow from 0 on reveal.
    var lastButtonWidthPx by remember { mutableIntStateOf(0) }
    buttonBounds?.let { lastButtonWidthPx = it.width }
    val buttonWidth = with(LocalDensity.current) { lastButtonWidthPx.toDp() }
    val buttonInset by animateDpAsState(
        if (besideButton && buttonWidth > 0.dp) buttonWidth + QuickControlsButtonGap else 0.dp,
        label = "musicButtonInset"
    )
    val buttonOnLeft = taskbarSettings.appMenuButtonSide == AppMenuButtonSide.LEFT

    TaskBarTheme(themeMode = themeMode) {
        // Fixed height: OverlayService keeps the window touch-transparent while the panel is hidden.
        Box(modifier = Modifier.fillMaxWidth().height(taskbarSettings.heightDp.dp), contentAlignment = Alignment.Center) {
            if (revealAnim.value > 0.001f) {
                MusicPanel(
                    mediaState = mediaState,
                    onPlayPause = appMenuViewModel::playPause,
                    onNext = appMenuViewModel::nextTrack,
                    onPrev = appMenuViewModel::prevTrack,
                    panelOutlineEnabled = panelOutlineEnabled,
                    translucentMode = translucentMode,
                    translucentAlpha = translucentAlpha,
                    grainAlpha = grainAlpha,
                    heightDp = taskbarSettings.heightDp,
                    dockWidthFraction = 1f,
                    onArtworkClick = {
                        if (mediaState.packageName.isNotEmpty()) {
                            appMenuViewModel.launchApp(mediaState.packageName)
                            taskbarViewModel.hideTaskbar()
                            appMenuViewModel.dismissMusicPanel()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(taskbarSettings.dockPadding.widthFraction)
                        .padding(start = if (buttonOnLeft) buttonInset else 0.dp, end = if (buttonOnLeft) 0.dp else buttonInset)
                        .onGloballyPositioned { panelWidthPx = it.size.width.toFloat() }
                        .graphicsLayer {
                            // Slides from the quick controls button's side, like the quick controls panel.
                            translationX = panelWidthPx * (1f - revealAnim.value) * (if (buttonOnLeft) -1f else 1f)
                            alpha = revealAnim.value
                        }
                )
            }
        }
    }
}

@Composable
internal fun CalculatorPanelContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    TaskBarTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CalculatorPanel(
                panelOutlineEnabled = panelOutlineEnabled,
                translucentMode = translucentMode,
                translucentAlpha = translucentAlpha,
                grainAlpha = grainAlpha,
                surfaceTintColor = surfaceTintColor,
                cornerRadiusDp = taskbarSettings.cornerRadiusDp,
                dockWidthFraction = taskbarSettings.dockPadding.widthFraction
            )
        }
    }
}
