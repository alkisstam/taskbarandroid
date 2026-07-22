package com.alkisstam.taskbar.service

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.bottomGapDp
import com.alkisstam.taskbar.data.widthFraction
import com.alkisstam.taskbar.data.GestureAction
import com.alkisstam.taskbar.ui.appmenu.AppMenuPanel
import com.alkisstam.taskbar.ui.appmenu.BrightnessPanel
import com.alkisstam.taskbar.ui.appmenu.CalculatorPanel
import com.alkisstam.taskbar.ui.appmenu.FloatingSearchBar
import com.alkisstam.taskbar.ui.appmenu.MusicPanel
import com.alkisstam.taskbar.ui.appmenu.VolumePanel
import com.alkisstam.taskbar.ui.clipboard.ClipboardPanel
import com.alkisstam.taskbar.ui.clipboard.NotesPanel
import com.alkisstam.taskbar.ui.notifications.NotificationHistoryPanel
import com.alkisstam.taskbar.ui.taskbar.TaskbarView
import com.alkisstam.taskbar.ui.taskbar.TriggerPillView
import com.alkisstam.taskbar.ui.theme.TaskBarTheme
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.ClipboardViewModel
import com.alkisstam.taskbar.viewmodel.NotificationHistoryViewModel
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel

private fun dockAwarePanelBottomPadding(isTaskbarVisible: Boolean, heightDp: Float, expandedRows: Int, bottomGapDp: Float) =
    if (isTaskbarVisible) (bottomGapDp + heightDp * (1 + expandedRows) + 16f + 28f).dp else 0.dp

@Composable
internal fun OverlayContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val quickControlsEnabled by taskbarViewModel.quickControlsEnabled.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()

    val expandedRows = if (isDockExpanded && quickControlsEnabled) 1 else 0
    val navBarInsetDp by taskbarViewModel.navBarInsetDp.collectAsState()
    val panelBottomPadding = dockAwarePanelBottomPadding(isTaskbarVisible, taskbarSettings.heightDp, expandedRows, maxOf(taskbarSettings.dockPadding.bottomGapDp, navBarInsetDp))

    TaskBarTheme(themeMode = themeMode) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
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
            Column(modifier = Modifier.wrapContentHeight().padding(bottom = panelBottomPadding)) {
                AppMenuPanel(
                    viewModel = appMenuViewModel,
                    taskbarViewModel = taskbarViewModel,
                    onHideTaskbar = taskbarViewModel::hideTaskbar,
                    modifier = Modifier
                )
            }
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
    val dockBottomGap = maxOf(taskbarSettings.dockPadding.bottomGapDp, navBarInsetDp).dp

    val revealAnim = remember { Animatable(0f) }
    var taskbarHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isTaskbarVisible, dockRevealProgress) {
        when {
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
internal fun TriggerPillContent(taskbarViewModel: TaskbarViewModel) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val pillSettings by taskbarViewModel.pillSettings.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val dockRevealMaxDragPx = remember(taskbarSettings.heightDp, density) {
        with(density) { taskbarSettings.heightDp.dp.toPx() }
    }

    TaskBarTheme(themeMode = themeMode) {
        TriggerPillView(
            isCollapsed = !isTaskbarVisible,
            pillSettings = pillSettings,
            dockRevealMaxDragPx = dockRevealMaxDragPx,
            onRevealProgress = { taskbarViewModel.setRevealProgress(it) },
            onRevealCommit = { taskbarViewModel.showTaskbar() },
            onRevealCancel = { taskbarViewModel.cancelReveal() },
            onAction = { action ->
                when (action) {
                    GestureAction.SHOW_DOCK -> taskbarViewModel.showTaskbar()
                    GestureAction.SHOW_NOTIFICATIONS -> {
                        val svc = TaskBarAccessibilityService.instance
                        if (svc != null) svc.expandNotifications()
                        else Toast.makeText(context, "Requires accessibility service", Toast.LENGTH_SHORT).show()
                    }
                    GestureAction.SHOW_QUICK_SETTINGS -> {
                        val svc = TaskBarAccessibilityService.instance
                        if (svc != null) svc.expandQuickSettings()
                        else Toast.makeText(context, "Requires accessibility service", Toast.LENGTH_SHORT).show()
                    }
                    GestureAction.DISABLED -> {}
                }
            }
        )
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
    val quickControlsEnabled by taskbarViewModel.quickControlsEnabled.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()

    val expandedRows = if (isDockExpanded && quickControlsEnabled) 1 else 0
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
    val quickControlsEnabled by taskbarViewModel.quickControlsEnabled.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()

    val expandedRows = if (isDockExpanded && quickControlsEnabled) 1 else 0
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
    val quickControlsEnabled by taskbarViewModel.quickControlsEnabled.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()
    val allApps by appMenuViewModel.allApps.collectAsState()

    val appsByPackage = remember(allApps) { allApps.associateBy { it.packageName } }

    val expandedRows = if (isDockExpanded && quickControlsEnabled) 1 else 0
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

    val panelShouldShow = musicPanelEnabled && (mediaState.hasSession || alwaysShowMusicPanel) && musicPanelVisible && !isSearching && !menuVisible

    val revealAnim = remember { Animatable(0f) }
    var panelHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(panelShouldShow, dockRevealProgress) {
        when {
            panelShouldShow && dockRevealProgress == 1f ->
                revealAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            !panelShouldShow || dockRevealProgress == 0f ->
                revealAnim.animateTo(0f, tween(220))
            panelShouldShow ->
                revealAnim.snapTo(dockRevealProgress)
        }
    }

    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()

    TaskBarTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                    cornerRadiusDp = taskbarSettings.cornerRadiusDp,
                    dockWidthFraction = taskbarSettings.dockPadding.widthFraction,
                    onArtworkClick = {
                        if (mediaState.packageName.isNotEmpty()) {
                            appMenuViewModel.launchApp(mediaState.packageName)
                            taskbarViewModel.hideTaskbar()
                            appMenuViewModel.dismissMusicPanel()
                        }
                    },
                    modifier = Modifier
                        .onGloballyPositioned { panelHeightPx = it.size.height.toFloat() }
                        .graphicsLayer {
                            translationY = panelHeightPx * (1f - revealAnim.value)
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
