package com.alkisstam.taskbar.service

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.GestureAction
import com.alkisstam.taskbar.ui.appmenu.AppMenuPanel
import com.alkisstam.taskbar.ui.appmenu.BrightnessPanel
import com.alkisstam.taskbar.ui.appmenu.FloatingSearchBar
import com.alkisstam.taskbar.ui.appmenu.MusicPanel
import com.alkisstam.taskbar.ui.appmenu.VolumePanel
import com.alkisstam.taskbar.ui.taskbar.TaskbarView
import com.alkisstam.taskbar.ui.taskbar.TriggerPillView
import com.alkisstam.taskbar.ui.theme.TaskBarTheme
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel

@Composable
internal fun OverlayContent(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel
) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()
    val isSettingsOpen by taskbarViewModel.isSettingsOpen.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val quickControlsEnabled by taskbarViewModel.quickControlsEnabled.collectAsState()
    val quickControlsStripEnabled by taskbarViewModel.quickControlsStripEnabled.collectAsState()

    val controlsInDock = quickControlsEnabled && quickControlsStripEnabled
    val panelBottomPadding = if (isTaskbarVisible) {
        (taskbarSettings.positionYDp + taskbarSettings.heightDp * (if (controlsInDock) 2 else 1) + 8f).dp
    } else 0.dp

    TaskBarTheme(themeMode = themeMode) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (menuVisible && !isSettingsOpen) {
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

    TaskBarTheme(themeMode = themeMode) {
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

@Composable
internal fun TriggerPillContent(taskbarViewModel: TaskbarViewModel) {
    val themeMode by taskbarViewModel.themeMode.collectAsState()
    val isTaskbarVisible by taskbarViewModel.isTaskbarVisible.collectAsState()
    val pillSettings by taskbarViewModel.pillSettings.collectAsState()
    val context = LocalContext.current

    TaskBarTheme(themeMode = themeMode) {
        TriggerPillView(
            isCollapsed = !isTaskbarVisible,
            pillSettings = pillSettings,
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
internal fun SearchOverlayContent(appMenuViewModel: AppMenuViewModel, onHideTaskbar: () -> Unit) {
    TaskBarTheme {
        FloatingSearchBar(viewModel = appMenuViewModel, onHideTaskbar = onHideTaskbar)
    }
}

@Composable
internal fun VolumePanelContent(
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
internal fun BrightnessPanelContent(
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
internal fun MusicPanelContent(
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
