package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.ui.appmenu.QuickControlItem
import com.alkisstam.taskbar.ui.appmenu.toItems
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.util.Constants
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.QuickControlItemData
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.abs

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
    val quickControlsEnabled by taskbarViewModel.quickControlsEnabled.collectAsState()
    val controlsOrder by taskbarViewModel.controlsOrder.collectAsState()
    val controlsDisabledIds by taskbarViewModel.controlsDisabledIds.collectAsState()
    val musicPanelEnabled by taskbarViewModel.musicPanelEnabled.collectAsState()
    val musicPanelVisible by appMenuViewModel.musicPanelVisible.collectAsState()
    val quickControls by appMenuViewModel.quickControlsState.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()
    val haptic = LocalHapticFeedback.current
    var swipeFired by remember { mutableStateOf(false) }

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

    val iconSize = (taskbarSettings.heightDp - 16f).coerceIn(24f, 48f).dp

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .wrapContentHeight()
                .border(2.dp, TaskbarOutlineGreen, RoundedCornerShape(16.dp))
                .pointerInput(isDockExpanded) {
                    detectDragGestures(
                        onDragStart = { swipeFired = false },
                        onDragEnd = { swipeFired = false },
                        onDragCancel = { swipeFired = false }
                    ) { _, drag ->
                        if (!swipeFired) {
                            val threshold = Constants.SWIPE_TRIGGER_THRESHOLD_PX
                            if (drag.y < -threshold && abs(drag.y) > abs(drag.x) && !isDockExpanded && quickControlsEnabled) {
                                swipeFired = true
                                taskbarViewModel.toggleDockExpanded()
                            } else if (drag.y > threshold && abs(drag.y) > abs(drag.x) && isDockExpanded) {
                                swipeFired = true
                                taskbarViewModel.toggleDockExpanded()
                            }
                        }
                    }
                },
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (quickControlsEnabled) {
                    AnimatedVisibility(
                        visible = isDockExpanded,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(taskbarSettings.heightDp.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                                            showLabel = taskbarSettings.showControlLabels
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
                                        showLabel = taskbarSettings.showControlLabels
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(taskbarSettings.heightDp.dp)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    AppMenuButton(
                        menuOpen = menuVisible,
                        onClick = { appMenuViewModel.toggleMenu() },
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    LazyRow(
                        state = pinnedListState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                            items(pinnedApps, key = { it.packageName }) { app ->
                                ReorderableItem(reorderableState, key = app.packageName) { isDragging ->
                                    PinnedAppItem(
                                        app = app,
                                        iconSize = iconSize,
                                        showLabel = false,
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
}
