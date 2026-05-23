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
import androidx.compose.foundation.background
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
    val recentApps by taskbarViewModel.recentApps.collectAsState()
    val recentAppsEnabled by taskbarViewModel.recentAppsEnabled.collectAsState()
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
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

                // Pinned section — max 3 icons visible before scrolling
                Box(modifier = Modifier.widthIn(max = 160.dp)) {
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

                // Recent apps section
                if (recentAppsEnabled && recentApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    LazyRow(
                        state = recentListState,
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(recentApps, key = { it.packageName }) { app ->
                            PinnedAppItem(
                                app = app,
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
