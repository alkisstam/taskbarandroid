package com.alkisstam.taskbar.ui.taskbar

import android.widget.Toast
import android.view.View
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.data.AppGroup
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.AppMenuButtonSide
import com.alkisstam.taskbar.data.bottomGapDp
import com.alkisstam.taskbar.data.widthFraction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.alkisstam.taskbar.ui.appmenu.QuickControlItem
import com.alkisstam.taskbar.ui.appmenu.toItems
import com.alkisstam.taskbar.ui.common.appGroupIcon
import com.alkisstam.taskbar.ui.theme.GlassBackdrop
import com.alkisstam.taskbar.ui.theme.GlassBlur
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.ui.theme.glassSheen
import com.alkisstam.taskbar.ui.theme.grain
import com.alkisstam.taskbar.util.Constants
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.QuickControlItemData
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel


internal val QuickControlsButtonGap = 4.dp

@Composable
fun TaskbarView(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel,
    modifier: Modifier = Modifier
) {
    val pinnedApps by taskbarViewModel.pinnedApps.collectAsState()
    val menuVisible by appMenuViewModel.menuVisible.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val floatingWindowsEnabled by taskbarViewModel.floatingWindowsEnabled.collectAsState()
    val floatingWindowsOnTap by taskbarViewModel.floatingWindowsOnTap.collectAsState()
    val altLaunchLabel = when {
        !floatingWindowsEnabled -> null
        floatingWindowsOnTap -> stringResource(R.string.app_action_open_fullscreen)
        else -> stringResource(R.string.app_action_open_floating)
    }
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val quickControlsEnabled by taskbarViewModel.quickControlsEnabled.collectAsState()
    val isDockExpanded by taskbarViewModel.isDockExpanded.collectAsState()
    val expandedGroupId by taskbarViewModel.expandedGroupId.collectAsState()
    val appGroups = taskbarViewModel.appGroupsResolved.collectAsState().value.filter { it.first.enabled }
    val pinDockControlEnabled by taskbarViewModel.pinDockControlEnabled.collectAsState()
    val isDockPinned by taskbarViewModel.isDockPinned.collectAsState()
    val pillIconCount by taskbarViewModel.pillIconCount.collectAsState()
    val pillVisible by taskbarViewModel.pillVisible.collectAsState()
    val iconsPillAutoHide by taskbarViewModel.iconsPillAutoHide.collectAsState()
    val density = LocalDensity.current
    val musicPanelEnabled by taskbarViewModel.musicPanelEnabled.collectAsState()
    val musicPanelVisible by appMenuViewModel.musicPanelVisible.collectAsState()
    val batteryLevel by taskbarViewModel.batteryLevel.collectAsState()
    val isCharging by taskbarViewModel.isCharging.collectAsState()

    val noMediaMessage by appMenuViewModel.noMediaMessage.collectAsState()
    val context = LocalContext.current
    // setGravity is a no-op for text toasts on API 30+, so positioning above the dock
    // never worked on modern devices; default placement everywhere is at least consistent.
    LaunchedEffect(noMediaMessage) {
        noMediaMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            appMenuViewModel.clearNoMediaMessage()
        }
    }

    val surfaceColor = if (surfaceTintColor != 0L)
        Color(surfaceTintColor)
    else
        MaterialTheme.colorScheme.surface

    val pinnedListState = rememberLazyListState()

    val iconSize = taskbarSettings.pinnedIconSizeDp.dp
    val dockCornerShape = RoundedCornerShape(taskbarSettings.cornerRadiusDp.dp)
    val dockWidthFraction = taskbarSettings.dockPadding.widthFraction

    val hostView = LocalView.current
    val buttonOnLeft = taskbarSettings.appMenuButtonSide == AppMenuButtonSide.LEFT
    // Slides in from the button's side and back out towards it.
    val slideFrom: (Int) -> Int = { width -> if (buttonOnLeft) -width else width }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pillIconCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(dockWidthFraction)
                    .padding(bottom = QuickControlsButtonGap)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isDockExpanded && expandedGroupId == null && quickControlsEnabled,
                    enter = slideInHorizontally(initialOffsetX = slideFrom) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = slideFrom) + fadeOut()
                ) {
                    QuickControlsPanel(
                        taskbarViewModel = taskbarViewModel,
                        appMenuViewModel = appMenuViewModel,
                        surfaceColor = surfaceColor
                    )
                }
                appGroups.forEach { (group, apps) ->
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isDockExpanded && expandedGroupId == group.id,
                        enter = slideInHorizontally(initialOffsetX = slideFrom) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = slideFrom) + fadeOut()
                    ) {
                        AppGroupPanel(
                            taskbarViewModel = taskbarViewModel,
                            group = group,
                            apps = apps,
                            surfaceColor = surfaceColor,
                            altLaunchLabel = altLaunchLabel
                        )
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = pillVisible && !isDockExpanded && !menuVisible,
                    modifier = Modifier.align(if (buttonOnLeft) Alignment.BottomStart else Alignment.BottomEnd),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    DisposableEffect(Unit) {
                        onDispose { taskbarViewModel.setQuickControlsButtonBounds(null) }
                    }
                    val pillHeight = taskbarSettings.heightDp.dp
                    val tileSize = taskbarSettings.quickControlSizeDp.dp
                    val controlsTile = QuickControlItemData(
                        id = "quick_controls", label = stringResource(R.string.quick_controls),
                        active = false, icon = Icons.Filled.Tune
                    ) to { taskbarViewModel.toggleDockExpanded() }
                    val musicTile = QuickControlItemData(
                        id = "music", label = stringResource(R.string.taskbar_music_control_label),
                        active = musicPanelVisible, icon = Icons.Filled.MusicNote
                    ) to { appMenuViewModel.toggleMusicPanel() }
                    val tiles = buildList {
                        if (quickControlsEnabled) add(controlsTile)
                        appGroups.forEach { (group, _) ->
                            add(QuickControlItemData(
                                id = "group_${group.id}", label = group.name,
                                active = false, icon = appGroupIcon(group.iconKey)
                            ) to { taskbarViewModel.toggleGroupPanel(group.id) })
                        }
                        if (musicPanelEnabled) add(musicTile)
                        if (pinDockControlEnabled) add(QuickControlItemData(
                            id = "pin_dock", label = stringResource(R.string.quick_control_pin_dock_label),
                            active = isDockPinned, icon = Icons.Filled.PushPin
                        ) to { taskbarViewModel.toggleDockPinned() })
                    }.let { if (buttonOnLeft) it else it.reversed() }
                    DockSurface(
                        taskbarViewModel = taskbarViewModel,
                        surfaceColor = surfaceColor,
                        shape = CircleShape,
                        cornerRadius = pillHeight / 2,
                        modifier = Modifier
                            .height(pillHeight)
                            .onGloballyPositioned { coords ->
                                taskbarViewModel.setQuickControlsButtonBounds(coords.boundsOnScreen(hostView))
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .height(pillHeight)
                                .padding(horizontal = ((pillHeight - tileSize) / 2).coerceAtLeast(8.dp)),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tiles.forEach { (data, onToggle) ->
                                QuickControlItem(item = data, onToggle = onToggle, showLabel = false, tileSize = tileSize)
                            }
                        }
                    }
                }
            }
        }

        DockSurface(
            taskbarViewModel = taskbarViewModel,
            surfaceColor = surfaceColor,
            shape = dockCornerShape,
            cornerRadius = taskbarSettings.cornerRadiusDp.dp,
            modifier = Modifier
                .fillMaxWidth(dockWidthFraction)
                .wrapContentHeight()
                .then(
                    if (iconsPillAutoHide) Modifier.pointerInput(Unit) {
                        val thresholdPx = with(density) { 24.dp.toPx() }
                        var dragY = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragY = 0f },
                            onDragEnd = {
                                if (dragY < -thresholdPx) taskbarViewModel.setPillRevealed(true)
                                else if (dragY > thresholdPx) taskbarViewModel.setPillRevealed(false)
                            }
                        ) { _, dy -> dragY += dy }
                    } else Modifier
                )
                .onGloballyPositioned { coords ->
                    // boundsInWindow includes the parent's reveal translation, so the blur
                    // window sees the dock moving and waits for it to settle.
                    taskbarViewModel.setDockBounds(coords.boundsOnScreen(hostView))
                    taskbarViewModel.setDockHeightPx(coords.size.height)
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(taskbarSettings.heightDp.dp)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (buttonOnLeft) {
                        AppMenuButton(
                            menuOpen = menuVisible,
                            onClick = { appMenuViewModel.toggleMenu() },
                            size = iconSize,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    LazyRow(
                        state = pinnedListState,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(taskbarSettings.pinnedIconPaddingDp.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                            items(pinnedApps, key = { it.packageName }) { app ->
                                PinnedAppItem(
                                    app = app,
                                    iconSize = iconSize,
                                    showLabel = false,
                                    iconShape = taskbarSettings.iconShape,
                                    onLaunch = {
                                        taskbarViewModel.launchAppTap(app.packageName)
                                        taskbarViewModel.hideTaskbar()
                                    },
                                    altLaunchLabel = altLaunchLabel,
                                    onAltLaunch = {
                                        taskbarViewModel.launchAppAlternate(app.packageName)
                                        taskbarViewModel.hideTaskbar()
                                    },
                                    onUnpin = { taskbarViewModel.unpinApp(app.packageName) }
                                )
                            }
                        }

                    if (!buttonOnLeft) {
                        AppMenuButton(
                            menuOpen = menuVisible,
                            onClick = { appMenuViewModel.toggleMenu() },
                            size = iconSize,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                StatusBarRow(batteryLevel = batteryLevel, isCharging = isCharging)
            }
        }
    }
}

private fun LayoutCoordinates.boundsOnScreen(hostView: View): IntRect? {
    val b = boundsInWindow()
    if (b.isEmpty) return null
    val origin = IntArray(2).also { hostView.getLocationOnScreen(it) }
    return IntRect(
        origin[0] + b.left.toInt(), origin[1] + b.top.toInt(),
        origin[0] + b.right.toInt(), origin[1] + b.bottom.toInt()
    )
}

@Composable
private fun DockSurface(
    taskbarViewModel: TaskbarViewModel,
    surfaceColor: Color,
    shape: Shape,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()

    Surface(
        modifier = modifier
            .then(if (panelOutlineEnabled) Modifier.border(1.dp, TaskbarOutlineGreen, shape) else Modifier)
            .clip(shape)
            .grain(enabled = translucentMode && grainAlpha > 0f, alpha = grainAlpha)
            .glassSheen(enabled = translucentMode && !panelOutlineEnabled, shape = shape),
        shape = shape,
        color = if (translucentMode) surfaceColor.copy(alpha = GlassBlur.surfaceAlpha(translucentAlpha)) else surfaceColor,
        tonalElevation = if (translucentMode) 0.dp else 3.dp,
        shadowElevation = if (translucentMode) 0.dp else 8.dp
    ) {
        // In-window, so it tracks the surface every frame (reveal, panel open) instead of
        // waiting for a separate blur window to catch up.
        GlassBackdrop(
            enabled = translucentMode,
            cornerRadius = cornerRadius,
            tint = surfaceColor.copy(alpha = translucentAlpha),
            content = content
        )
    }
}

@Composable
private fun QuickControlsPanel(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel,
    surfaceColor: Color
) {
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val controlsOrder by taskbarViewModel.controlsOrder.collectAsState()
    val controlsDisabledIds by taskbarViewModel.controlsDisabledIds.collectAsState()
    val quickSettingsPanelEnabled by taskbarViewModel.quickSettingsPanelEnabled.collectAsState()
    val quickSettingsPanelVisible by appMenuViewModel.quickSettingsPanelVisible.collectAsState()
    val quickControls by appMenuViewModel.quickControlsState.collectAsState()

    val quickControlItems = quickControls.toItems(controlsOrder, controlsDisabledIds)
    val quickSettingsLabel = stringResource(R.string.quick_control_settings_label)
    val extraTiles = buildList<Pair<QuickControlItemData, () -> Unit>> {
        if (quickSettingsPanelEnabled) add(QuickControlItemData(id = "quick_settings", label = quickSettingsLabel, active = quickSettingsPanelVisible, icon = Icons.Filled.Settings) to { appMenuViewModel.toggleQuickSettingsPanel() })
    }

    ExpandedPanelSurface(taskbarViewModel = taskbarViewModel, surfaceColor = surfaceColor) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(taskbarSettings.heightDp.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(extraTiles) { (data, onToggle) ->
                QuickControlItem(
                    item = data,
                    onToggle = onToggle,
                    showLabel = taskbarSettings.showControlLabels,
                    tileSize = taskbarSettings.quickControlSizeDp.dp
                )
            }
            items(quickControlItems) { item ->
                QuickControlItem(
                    item = item,
                    onToggle = {
                        appMenuViewModel.handleQuickControlAction(item.id)
                        if (item.id in listOf("qr", "power", "screenshot", "lockscreen", "wifi", "bluetooth", "share"))
                            taskbarViewModel.hideTaskbar()
                        if (item.id in listOf("wifi", "bluetooth", "share")) {
                            appMenuViewModel.dismissMusicPanel()
                            appMenuViewModel.dismissCalculatorPanel()
                        }
                    },
                    showLabel = taskbarSettings.showControlLabels,
                    tileSize = taskbarSettings.quickControlSizeDp.dp
                )
            }
        }
    }
}

// Panel above the dock that swipes down to close.
@Composable
private fun ExpandedPanelSurface(
    taskbarViewModel: TaskbarViewModel,
    surfaceColor: Color,
    content: @Composable () -> Unit
) {
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val density = LocalDensity.current
    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    DockSurface(
        taskbarViewModel = taskbarViewModel,
        surfaceColor = surfaceColor,
        shape = RoundedCornerShape(taskbarSettings.cornerRadiusDp.dp),
        cornerRadius = taskbarSettings.cornerRadiusDp.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffset.value }
            .pointerInput(taskbarSettings.heightDp) {
                val closeThresholdPx = with(density) { taskbarSettings.heightDp.dp.toPx() } * 0.4f
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset.value >= closeThresholdPx) taskbarViewModel.collapseDock()
                        scope.launch { dragOffset.animateTo(0f, tween(180)) }
                    },
                    onDragCancel = { scope.launch { dragOffset.animateTo(0f, tween(180)) } }
                ) { _, dy ->
                    scope.launch { dragOffset.snapTo((dragOffset.value + dy).coerceAtLeast(0f)) }
                }
            },
        content = content
    )
}

@Composable
private fun AppGroupPanel(
    taskbarViewModel: TaskbarViewModel,
    group: AppGroup,
    apps: List<AppInfo>,
    surfaceColor: Color,
    altLaunchLabel: String?
) {
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val removeLabel = stringResource(R.string.app_group_remove_app)

    ExpandedPanelSurface(taskbarViewModel = taskbarViewModel, surfaceColor = surfaceColor) {
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(taskbarSettings.heightDp.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.app_group_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(taskbarSettings.heightDp.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(taskbarSettings.pinnedIconPaddingDp.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(apps, key = { it.packageName }) { app ->
                    PinnedAppItem(
                        app = app,
                        iconSize = taskbarSettings.pinnedIconSizeDp.dp,
                        showLabel = false,
                        iconShape = taskbarSettings.iconShape,
                        onLaunch = {
                            taskbarViewModel.launchAppTap(app.packageName)
                            taskbarViewModel.hideTaskbar()
                        },
                        altLaunchLabel = altLaunchLabel,
                        onAltLaunch = {
                            taskbarViewModel.launchAppAlternate(app.packageName)
                            taskbarViewModel.hideTaskbar()
                        },
                        onUnpin = { taskbarViewModel.removeAppGroupPackage(group.id, app.packageName) },
                        unpinLabel = removeLabel
                    )
                }
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
            Text(stringResource(R.string.taskbar_battery_percent, batteryLevel), style = MaterialTheme.typography.labelSmall, color = contentAlpha)
        }
    }
}
