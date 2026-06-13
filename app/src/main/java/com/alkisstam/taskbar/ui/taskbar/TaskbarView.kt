package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.ui.appmenu.QuickControlItem
import com.alkisstam.taskbar.ui.appmenu.toItems
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.QuickControlItemData
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun TaskbarView(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel,
    modifier: Modifier = Modifier
) {
    val pinnedApps by taskbarViewModel.pinnedApps.collectAsState()
    val recentApps by taskbarViewModel.recentApps.collectAsState()
    val recentAppsEnabled by taskbarViewModel.recentAppsEnabled.collectAsState()
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val quickControlsEnabled by taskbarViewModel.quickControlsEnabled.collectAsState()
    val quickControlsStripEnabled by taskbarViewModel.quickControlsStripEnabled.collectAsState()
    val controlsOrder by taskbarViewModel.controlsOrder.collectAsState()
    val controlsDisabledIds by taskbarViewModel.controlsDisabledIds.collectAsState()
    val musicPanelEnabled by taskbarViewModel.musicPanelEnabled.collectAsState()
    val musicPanelVisible by appMenuViewModel.musicPanelVisible.collectAsState()
    val quickControls by appMenuViewModel.quickControlsState.collectAsState()
    val controlsShowLabels by taskbarViewModel.controlsShowLabels.collectAsState()
    val haptic = LocalHapticFeedback.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    val surfaceColor = if (surfaceTintColor != 0L)
        Color(surfaceTintColor)
    else
        MaterialTheme.colorScheme.surface

    val pinnedListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(pinnedListState) { from, to ->
        val pkgs = pinnedApps.map { it.packageName }.toMutableList()
        pkgs.add(to.index, pkgs.removeAt(from.index))
        taskbarViewModel.reorderPinnedApps(pkgs)
    }

    val recentListState = rememberLazyListState()
    val showControlsInDock = quickControlsEnabled && quickControlsStripEnabled
    val iconSize = (taskbarSettings.heightDp - 16f).coerceIn(24f, 48f).dp

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .widthIn(max = (screenWidthDp * 0.98f).dp)
                .border(2.dp, TaskbarOutlineGreen, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Column {
                if (showControlsInDock) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(taskbarSettings.heightDp.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (musicPanelEnabled) {
                            item {
                                QuickControlItem(
                                    item = QuickControlItemData(
                                        id = "music",
                                        label = "Music",
                                        active = musicPanelVisible,
                                        icon = Icons.Filled.MusicNote
                                    ),
                                    onToggle = { appMenuViewModel.toggleMusicPanel() },
                                    showLabel = controlsShowLabels
                                )
                            }
                        }
                        items(quickControls.toItems(controlsOrder, controlsDisabledIds)) { item ->
                            QuickControlItem(
                                item = item,
                                onToggle = {
                                    appMenuViewModel.handleQuickControlAction(item.id)
                                    if (item.id in listOf("qr", "power", "screenshot", "lockscreen")) taskbarViewModel.hideTaskbar()
                                },
                                showLabel = controlsShowLabels
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                }

                Row(
                    modifier = Modifier
                        .then(if (recentAppsEnabled || showControlsInDock) Modifier.fillMaxWidth() else Modifier)
                        .height(taskbarSettings.heightDp.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    AppMenuButton(
                        menuOpen = menuVisible,
                        onClick = { appMenuViewModel.toggleMenu() }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    val hasRecents = recentAppsEnabled && recentApps.isNotEmpty()
                    val hasPinned = pinnedApps.isNotEmpty()

                    if (hasPinned || !hasRecents) {
                        val pinnedBoxModifier = when {
                            hasRecents -> Modifier.weight(0.6f)
                            recentAppsEnabled || showControlsInDock -> Modifier.weight(1f)
                            else -> Modifier.widthIn(max = 308.dp)
                        }
                        Box(modifier = pinnedBoxModifier) {
                            LazyRow(
                                state = pinnedListState,
                                modifier = Modifier.wrapContentWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(pinnedApps, key = { it.packageName }) { app ->
                                    ReorderableItem(reorderableState, key = app.packageName) { isDragging ->
                                        PinnedAppItem(
                                            app = app,
                                            iconSize = iconSize,
                                            showLabel = taskbarSettings.showLabels,
                                            isDragging = isDragging,
                                            dragModifier = Modifier.longPressDraggableHandle(
                                                onDragStarted = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            ),
                                            onLaunch = {
                                                taskbarViewModel.launchApp(app.packageName)
                                                taskbarViewModel.hideTaskbar()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (hasRecents) {
                        if (hasPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(32.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        LazyRow(
                            state = recentListState,
                            modifier = if (hasPinned) Modifier.weight(0.4f) else Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(recentApps, key = { it.packageName }) { app ->
                                PinnedAppItem(
                                    app = app,
                                    iconSize = iconSize,
                                    showLabel = taskbarSettings.showLabels,
                                    isDragging = false,
                                    dragModifier = Modifier,
                                    onLaunch = {
                                        taskbarViewModel.launchApp(app.packageName)
                                        taskbarViewModel.hideTaskbar()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
