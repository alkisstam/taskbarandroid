package com.alkisstam.taskbar.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoNotDisturbOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ScreenRotationAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.ThemeMode
import com.alkisstam.taskbar.ui.common.AppIconImage
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskbarViewModel,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("General", "Apps", "Controls", "Design")
    val tabIcons = listOf(
        Icons.Filled.Tune,
        Icons.Filled.PushPin,
        Icons.Filled.Dashboard,
        Icons.Filled.Palette
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBottomPadding = 88.dp + navBarPadding

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Dock Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> GeneralTab(
                        viewModel = viewModel,
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                        bottomPadding = navBottomPadding
                    )
                    1 -> PinnedAppsTab(viewModel = viewModel, bottomPadding = navBottomPadding)
                    2 -> ControlsTab(viewModel = viewModel, bottomPadding = navBottomPadding)
                    3 -> PillSettingsScreen(viewModel = viewModel, bottomPadding = navBottomPadding)
                }
            }

            androidx.compose.material3.Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(40.dp),
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = pagerState.currentPage == index
                        if (selected) {
                            androidx.compose.material3.Surface(
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcons[index],
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = title,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralTab(
    viewModel: TaskbarViewModel,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val surfaceTintColor by viewModel.surfaceTintColor.collectAsState()
    val autoHideInFullscreen by viewModel.autoHideInFullscreen.collectAsState()
    val autoHideInLandscape by viewModel.autoHideInLandscape.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsCard(title = "Overlay Status") {
            if (!hasOverlayPermission) {
                Text(
                    text = "Floating Dock requires the \"Draw over other apps\" permission to show the dock above other apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRequestOverlayPermission) {
                    Text("Grant Permission")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (overlayEnabled) "Dock is running" else "Dock is stopped",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (overlayEnabled) "Visible above all apps" else "Tap start to enable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (overlayEnabled) {
                        OutlinedButton(
                            onClick = viewModel::stopOverlay,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text("Stop")
                        }
                    } else {
                        Button(onClick = viewModel::startOverlay) {
                            Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text("Start")
                        }
                    }
                }
            }
        }

        SettingsCard(title = "Theme") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    label = { Text("Light") },
                    leadingIcon = { Icon(Icons.Filled.LightMode, contentDescription = null) }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                    label = { Text("Dark") },
                    leadingIcon = { Icon(Icons.Filled.DarkMode, contentDescription = null) }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    label = { Text("System") },
                    leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SurfaceTintColorPicker(
                currentColor = surfaceTintColor,
                onColorSelected = { viewModel.setSurfaceTintColor(it) }
            )
        }

        SettingsCard(title = "Behaviour") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-hide in Fullscreen", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Hide the dock when an app goes fullscreen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoHideInFullscreen,
                    onCheckedChange = { viewModel.setAutoHideInFullscreen(it) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-hide in Landscape", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Hide the dock when the device is in landscape",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoHideInLandscape,
                    onCheckedChange = { viewModel.setAutoHideInLandscape(it) }
                )
            }
        }

        MusicPanelSettingsCard(viewModel = viewModel, context = context)

        SettingsCard(title = "Navigation Bar Overlay") {
            if (!hasAccessibilityPermission) {
                Text(
                    text = "Enable the Floating Dock Accessibility Service to allow the overlay to draw above the system navigation bar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRequestAccessibilityPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Accessibility Service")
                }
            } else {
                Text(
                    text = "Accessibility Service is active. The overlay will appear above the navigation bar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        SettingsCard(title = "Permissions") {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Write Settings (Auto-rotate / Brightness)")
            }
        }
    }
}

@Composable
private fun MusicPanelSettingsCard(viewModel: TaskbarViewModel, context: android.content.Context) {
    val musicPanelEnabled by viewModel.musicPanelEnabled.collectAsState()
    val notificationAccessGranted = remember {
        val enabled = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: ""
        val component = "${context.packageName}/com.alkisstam.taskbar.service.MediaListenerService"
        enabled.split(":").any { it.trim() == component }
    }

    SettingsCard(title = "Music Panel") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Show Music Panel", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Floats above the dock when media is playing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = musicPanelEnabled,
                onCheckedChange = { viewModel.setMusicPanelEnabled(it) }
            )
        }
        if (!notificationAccessGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Requires Notification Access to read track info and control playback.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Notification Access")
            }
        }
    }
}

@Composable
private fun PinnedAppsTab(viewModel: TaskbarViewModel, bottomPadding: Dp = 0.dp) {
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val pinnedPackages = remember(pinnedApps) { pinnedApps.map { it.packageName }.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsCard(title = "Pinned Apps (${pinnedApps.size})") {
                if (pinnedApps.isEmpty()) {
                    Text(
                        text = "No apps pinned yet. Tap + on any app below to pin it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val lazyListState = rememberLazyListState()
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                        val pkgs = pinnedApps.map { it.packageName }.toMutableList()
                        pkgs.add(to.index, pkgs.removeAt(from.index))
                        viewModel.reorderPinnedApps(pkgs)
                    }
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(pinnedApps, key = { it.packageName }) { app ->
                            ReorderableItem(reorderableState, key = app.packageName) { isDragging ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .longPressDraggableHandle(
                                            onDragStarted = {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            }
                                        )
                                ) {
                                    Box {
                                        AppIconImage(
                                            icon = app.icon,
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .then(
                                                    if (isDragging) Modifier.background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        RoundedCornerShape(12.dp)
                                                    ) else Modifier
                                                )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(16.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.error,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.unpinApp(app.packageName) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Unpin",
                                                tint = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.width(48.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SettingsCard(title = "All Apps (${allApps.size})") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(400.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allApps, key = { it.packageName }) { app ->
                        AllAppGridItem(
                            app = app,
                            isPinned = app.packageName in pinnedPackages,
                            onTogglePin = {
                                if (app.packageName in pinnedPackages)
                                    viewModel.unpinApp(app.packageName)
                                else
                                    viewModel.pinApp(app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllAppGridItem(
    app: AppInfo,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            AppIconImage(
                icon = app.icon,
                contentDescription = app.label,
                modifier = Modifier.size(52.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .background(
                        color = if (isPinned) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable(onClick = onTogglePin),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = if (isPinned) "Unpin" else "Pin",
                    tint = if (isPinned) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ControlsTab(viewModel: TaskbarViewModel, bottomPadding: Dp = 0.dp) {
    val quickControlsEnabled by viewModel.quickControlsEnabled.collectAsState()
    val taskbarSettings by viewModel.taskbarSettings.collectAsState()
    val controlsOrder by viewModel.controlsOrder.collectAsState()
    val controlsDisabledIds by viewModel.controlsDisabledIds.collectAsState()

    val controlMeta = remember {
        listOf(
            Triple("torch",             "Torch",      Icons.Filled.FlashlightOn),
            Triple("ringer",            "Ringer",     Icons.AutoMirrored.Filled.VolumeUp),
            Triple("rotate",            "Rotate",     Icons.Filled.ScreenRotationAlt),
            Triple("brightness_slider", "Brightness", Icons.Filled.BrightnessMedium),
            Triple("dnd",               "DND",        Icons.Filled.DoNotDisturbOff),
            Triple("qr",                "QR",         Icons.Filled.QrCodeScanner),
            Triple("power",             "Power",      Icons.Filled.PowerSettingsNew),
            Triple("volume",            "Volume",     Icons.Filled.Tune),
            Triple("screenshot",        "Screenshot", Icons.Filled.PhotoCamera),
            Triple("lockscreen",        "Lock",       Icons.Filled.Lock),
            Triple("caffeine",          "Caffeine",   Icons.Filled.FreeBreakfast)
        )
    }
    val metaMap = remember(controlMeta) { controlMeta.associateBy { it.first } }

    val activeIds = remember(controlsOrder, controlsDisabledIds) {
        controlsOrder.filter { it !in controlsDisabledIds }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsCard(title = "Quick Controls") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable Quick Controls", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Show quick controls in the dock or apps panel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = quickControlsEnabled,
                        onCheckedChange = { viewModel.setQuickControlsEnabled(it) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (quickControlsEnabled) 1f else 0.38f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show control labels", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = taskbarSettings.showControlLabels,
                        onCheckedChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(showControlLabels = it)) },
                        enabled = quickControlsEnabled
                    )
                }
            }
        }

        item {
            SettingsCard(
                title = "Active Controls",
                modifier = Modifier.alpha(if (quickControlsEnabled) 1f else 0.38f)
            ) {
                if (activeIds.isEmpty()) {
                    Text(
                        "No controls enabled. Enable some below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val lazyListState = rememberLazyListState()
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                        val newOrder = controlsOrder.toMutableList()
                        val fromId = activeIds[from.index]
                        val toId = activeIds[to.index]
                        val fromGlobal = newOrder.indexOf(fromId)
                        val toGlobal = newOrder.indexOf(toId)
                        if (fromGlobal >= 0 && toGlobal >= 0) {
                            newOrder.add(toGlobal, newOrder.removeAt(fromGlobal))
                            viewModel.saveControlsOrder(newOrder)
                        }
                    }
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(activeIds, key = { it }) { id ->
                            ReorderableItem(reorderableState, key = id) { isDragging ->
                                val meta = metaMap[id] ?: return@ReorderableItem
                                ActiveControlItem(
                                    label = meta.second,
                                    icon = meta.third,
                                    isDragging = isDragging,
                                    modifier = Modifier.longPressDraggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(
                                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                            )
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = "All Controls",
                modifier = Modifier.alpha(if (quickControlsEnabled) 1f else 0.38f)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(260.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(controlMeta, key = { it.first }) { meta ->
                        val (id, label, icon) = meta
                        ControlGridItem(
                            label = label,
                            icon = icon,
                            isEnabled = id !in controlsDisabledIds,
                            isInteractive = quickControlsEnabled,
                            onToggle = {
                                val newDisabled = controlsDisabledIds.toMutableSet()
                                if (id !in controlsDisabledIds) {
                                    newDisabled.add(id)
                                } else {
                                    newDisabled.remove(id)
                                    if (id !in controlsOrder) {
                                        viewModel.saveControlsOrder(controlsOrder + id)
                                    }
                                }
                                viewModel.saveControlsDisabledIds(newDisabled)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveControlItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
private fun ControlGridItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    isInteractive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .then(if (isInteractive) Modifier.clickable(onClick = onToggle) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .background(
                        color = if (isEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEnabled) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

private val SURFACE_TINT_PRESETS: List<Pair<String, Long>> = listOf(
    "Default" to 0L,
    "Black" to 0xFF1A1A2E,
    "Navy" to 0xFF16213E,
    "Deep Purple" to 0xFF2D1B69,
    "Slate" to 0xFF2D3748,
    "Charcoal" to 0xFF36454F,
    "Cream" to 0xFFFFF8E7,
    "Sand" to 0xFFFFF3E0,
    "Blush" to 0xFFFFE4E1,
    "Sky" to 0xFFE3F2FD,
    "Mint" to 0xFFE8F5E9
)

@Composable
private fun SurfaceTintColorPicker(
    currentColor: Long,
    onColorSelected: (Long) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Surface Tint Color", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Applies to the dock, app menu, and quick controls strip",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            items(SURFACE_TINT_PRESETS) { (label, colorValue) ->
                val isSelected = currentColor == colorValue
                val displayColor = if (colorValue == 0L)
                    MaterialTheme.colorScheme.surface
                else
                    Color(colorValue)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(displayColor, CircleShape)
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable { onColorSelected(colorValue) }
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                val isCustomSelected = SURFACE_TINT_PRESETS.none { it.second == currentColor }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isCustomSelected) Color(currentColor) else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .then(
                                if (isCustomSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable { showCustomPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isCustomSelected) {
                            Text("+", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        "Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCustomSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = if (SURFACE_TINT_PRESETS.none { it.second == currentColor } && currentColor != 0L)
                currentColor else 0xFF808080L,
            onDismiss = { showCustomPicker = false },
            onConfirm = { color ->
                onColorSelected(color)
                showCustomPicker = false
            }
        )
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var r by remember { mutableFloatStateOf(((initialColor shr 16) and 0xFF).toFloat()) }
    var g by remember { mutableFloatStateOf(((initialColor shr 8) and 0xFF).toFloat()) }
    var b by remember { mutableFloatStateOf((initialColor and 0xFF).toFloat()) }
    val previewColor = Color(
        red = r / 255f,
        green = g / 255f,
        blue = b / 255f
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(previewColor, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("R: ${r.toInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = r,
                        onValueChange = { r = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("G: ${g.toInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = g,
                        onValueChange = { g = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("B: ${b.toInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = b,
                        onValueChange = { b = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val color = (0xFF000000L) or
                    (r.toLong().coerceIn(0, 255) shl 16) or
                    (g.toLong().coerceIn(0, 255) shl 8) or
                    b.toLong().coerceIn(0, 255)
                onConfirm(color)
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
