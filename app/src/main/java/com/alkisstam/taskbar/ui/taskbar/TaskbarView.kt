package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
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
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val haptic = LocalHapticFeedback.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    val surfaceColor = if (surfaceTintColor != 0L)
        Color(surfaceTintColor)
    else
        MaterialTheme.colorScheme.surface

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val pkgs = pinnedApps.map { it.packageName }.toMutableList()
        pkgs.add(to.index, pkgs.removeAt(from.index))
        taskbarViewModel.reorderPinnedApps(pkgs)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = taskbarSettings.positionYDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = (screenWidthDp * 0.95f).dp)
                .height(taskbarSettings.heightDp.dp),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                AppMenuButton(
                    menuOpen = menuVisible,
                    onClick = { appMenuViewModel.toggleMenu() }
                )

                Spacer(modifier = Modifier.width(4.dp))

                LazyRow(
                    state = lazyListState,
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(pinnedApps, key = { it.packageName }) { app ->
                        ReorderableItem(reorderableState, key = app.packageName) { isDragging ->
                            PinnedAppItem(
                                app = app,
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
    }
}
