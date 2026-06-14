package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
    val dockExpandProgress by taskbarViewModel.dockExpandProgress.collectAsState()
    val batteryLevel by taskbarViewModel.batteryLevel.collectAsState()
    val isCharging by taskbarViewModel.isCharging.collectAsState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

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
                .pointerInput(isDockExpanded, taskbarSettings.heightDp) {
                    val maxDragPx = with(density) { taskbarSettings.heightDp.dp.toPx() }
                    var totalDragY = 0f
                    detectDragGestures(
                        onDragStart = { totalDragY = 0f },
                        onDragEnd = {
                            val upward = -totalDragY
                            val downward = totalDragY
                            when {
                                !isDockExpanded && quickControlsEnabled && upward > 0 -> {
                                    if ((upward / maxDragPx).coerceIn(0f, 1f) >= 0.4f)
                                        taskbarViewModel.toggleDockExpanded()
                                    else
                                        taskbarViewModel.cancelExpandReveal()
                                }
                                isDockExpanded && downward > 0 -> {
                                    if ((downward / maxDragPx).coerceIn(0f, 1f) >= 0.4f)
                                        taskbarViewModel.toggleDockExpanded()
                                    else
                                        taskbarViewModel.cancelCollapseReveal()
                                }
                            }
                            totalDragY = 0f
                        },
                        onDragCancel = {
                            if (!isDockExpanded) taskbarViewModel.cancelExpandReveal()
                            else taskbarViewModel.cancelCollapseReveal()
                            totalDragY = 0f
                        }
                    ) { _, drag ->
                        totalDragY += drag.y
                        if (abs(drag.y) > abs(drag.x)) {
                            val upward = -totalDragY
                            val downward = totalDragY
                            when {
                                !isDockExpanded && quickControlsEnabled && upward > 0 ->
                                    taskbarViewModel.setExpandProgress((upward / maxDragPx).coerceIn(0f, 1f))
                                isDockExpanded && downward > 0 ->
                                    taskbarViewModel.setExpandProgress(1f - (downward / maxDragPx).coerceIn(0f, 1f))
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
                    val expandAnim = remember { Animatable(if (isDockExpanded) 1f else 0f) }
                    val maxControlsHeight = taskbarSettings.heightDp.dp + 1.dp

                    LaunchedEffect(isDockExpanded, dockExpandProgress) {
                        when {
                            isDockExpanded && dockExpandProgress >= 1f ->
                                expandAnim.animateTo(1f, tween(220))
                            !isDockExpanded && dockExpandProgress == 0f ->
                                expandAnim.animateTo(0f, tween(180))
                            else ->
                                expandAnim.snapTo(dockExpandProgress)
                        }
                    }

                    if (expandAnim.value > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(maxControlsHeight * expandAnim.value)
                                .clipToBounds()
                        ) {
                            Column {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(taskbarSettings.heightDp.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
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
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 4.dp)
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

                StatusBarRow(batteryLevel = batteryLevel, isCharging = isCharging)
            }
        }
    }
}

@Composable
private fun StatusBarRow(batteryLevel: Int, isCharging: Boolean) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
            currentTime = LocalTime.now()
        }
    }
    val timeStr = remember(currentTime) {
        currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    val dateStr = remember(currentTime) {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
    val batteryIcon = when {
        isCharging        -> Icons.Filled.BatteryChargingFull
        batteryLevel > 80 -> Icons.Filled.BatteryFull
        batteryLevel > 60 -> Icons.Filled.Battery5Bar
        batteryLevel > 40 -> Icons.Filled.Battery3Bar
        batteryLevel > 20 -> Icons.Filled.Battery2Bar
        else              -> Icons.Filled.Battery0Bar
    }
    val contentAlpha = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = contentAlpha)
            Text("·", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = contentAlpha)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(batteryIcon, contentDescription = null,
                modifier = Modifier.width(14.dp).height(14.dp),
                tint = contentAlpha)
            Text("$batteryLevel%", style = MaterialTheme.typography.labelSmall, color = contentAlpha)
        }
    }
}
