package com.alkisstam.taskbar.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotationAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.ui.res.painterResource
import com.alkisstam.taskbar.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.ui.common.AppIconImage
import com.alkisstam.taskbar.ui.common.LocalHapticEnabled
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskbarViewModel,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasNotificationListenerPermission: Boolean,
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
                        hasNotificationListenerPermission = hasNotificationListenerPermission,
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
    hasNotificationListenerPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()
    val autoHideInFullscreen by viewModel.autoHideInFullscreen.collectAsState()
    val autoHideInLandscape by viewModel.autoHideInLandscape.collectAsState()
    val disableOnLockscreen by viewModel.disableOnLockscreen.collectAsState()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    val backupError by viewModel.backupError.collectAsState()
    LaunchedEffect(backupError) {
        backupError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearBackupError()
        }
    }

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
                    Column(modifier = Modifier.weight(1f)) {
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

        SettingsCard(title = "Behaviour") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                Column(modifier = Modifier.weight(1f)) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Disable on Lock Screen", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Prevent the trigger pill from opening the dock on the lock screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = disableOnLockscreen,
                    onCheckedChange = { viewModel.setDisableOnLockscreen(it) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibrate Feedback", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Haptic feedback on long press and drag",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = { viewModel.setHapticFeedbackEnabled(it) }
                )
            }
        }

        MusicPanelSettingsCard(
            viewModel = viewModel,
            context = context,
            notificationAccessGranted = hasNotificationListenerPermission
        )

        SearchSettingsCard(viewModel = viewModel)

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
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Couldn't open settings", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Write Settings (Auto-rotate / Brightness)")
            }
        }

        SettingsCard(title = "Backup & Restore") {
            OutlinedButton(
                onClick = { backupLauncher.launch("taskbar_backup.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup Settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Settings")
            }
        }

        var showResetDialog by remember { mutableStateOf(false) }
        SettingsCard(title = "Reset") {
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Settings to Default")
            }
        }
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Settings") },
                text = { Text("Reset all settings to default? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetAllSettings()
                        showResetDialog = false
                    }) {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SettingsCard(title = "Contact & Feedback") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            )
                        )
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:alkisstam@icloud.com"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Contact & Feedback",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF26A5E4))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/floatingdock"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app found to open link", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_telegram),
                        contentDescription = "Telegram",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicPanelSettingsCard(
    viewModel: TaskbarViewModel,
    context: android.content.Context,
    notificationAccessGranted: Boolean
) {
    val musicPanelEnabled by viewModel.musicPanelEnabled.collectAsState()

    SettingsCard(title = "Music Panel") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                    try {
                        context.startActivity(
                            android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Couldn't open settings", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Notification Access")
            }
        }
    }
}

@Composable
private fun SearchSettingsCard(viewModel: TaskbarViewModel) {
    val fuzzySearchEnabled by viewModel.fuzzySearchEnabled.collectAsState()
    val showRecentApps by viewModel.showRecentApps.collectAsState()
    SettingsCard(title = "Search") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Fuzzy Search", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Match apps even with typos or partial names",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = fuzzySearchEnabled,
                onCheckedChange = { viewModel.setFuzzySearchEnabled(it) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show Recent Apps", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Show your 5 most recently opened apps when you tap the search bar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = showRecentApps,
                onCheckedChange = { viewModel.setShowRecentApps(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinnedAppsTab(viewModel: TaskbarViewModel, bottomPadding: Dp = 0.dp) {
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val hiddenApps by viewModel.hiddenAppsInfo.collectAsState()
    val pinnedPackages = remember(pinnedApps) { pinnedApps.map { it.packageName }.toSet() }
    val appGridColumns by viewModel.appGridColumns.collectAsState()
    val appGridRows by viewModel.appGridRows.collectAsState()
    val showRecentAppsRow by viewModel.showRecentAppsRow.collectAsState()
    var showHideAppPicker by remember { mutableStateOf(false) }

    if (showHideAppPicker) {
        HideAppPickerDialog(
            apps = allApps,
            onHide = { viewModel.hideApp(it) },
            onDismiss = { showHideAppPicker = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tap on an app icon to add it to Pinned Apps. Touch and drag an icon in Pinned Apps to re-order it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
                    val hapticEnabled = LocalHapticEnabled.current
                    // Local working copy during a drag: persisting per-swap races the async
                    // DataStore save and can store a stale intermediate order.
                    var localPinned by remember(pinnedApps) { mutableStateOf(pinnedApps) }
                    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                        localPinned = localPinned.toMutableList().apply {
                            if (from.index in indices && to.index in indices) add(to.index, removeAt(from.index))
                        }
                    }
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(localPinned, key = { it.packageName }) { app ->
                            ReorderableItem(reorderableState, key = app.packageName) { isDragging ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .longPressDraggableHandle(
                                            onDragStarted = {
                                                if (hapticEnabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            },
                                            onDragStopped = {
                                                viewModel.reorderPinnedApps(localPinned.map { it.packageName })
                                            }
                                        )
                                ) {
                                    Box {
                                        AppIconImage(
                                            icon = app.icon,
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .then(
                                                    if (isDragging) Modifier.background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        CircleShape
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
            SettingsCard(title = "Hidden Apps (${hiddenApps.size})") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { showHideAppPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add App",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.width(48.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    items(hiddenApps, key = { it.packageName }) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box {
                                AppIconImage(
                                    icon = app.icon,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(48.dp).clip(CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.unhideApp(app.packageName) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Unhide",
                                        tint = MaterialTheme.colorScheme.onSecondary,
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
        item {
            SettingsCard(title = "All Apps (${allApps.size})") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(340.dp), // ~4 rows: (80dp row * 4) + (4dp gap * 3) + (4dp padding * 2)
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
        item {
            SettingsCard(title = "Recent Apps") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Recent Apps in All Apps Panel", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Adds a row of recently opened apps under the search bar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showRecentAppsRow,
                        onCheckedChange = { viewModel.setShowRecentAppsRow(it) }
                    )
                }
            }
        }
        item {
            SettingsCard(title = "App Grid") {
                var localColumns by remember(appGridColumns) { androidx.compose.runtime.mutableFloatStateOf(appGridColumns.toFloat()) }
                var localRows by remember(appGridRows) { androidx.compose.runtime.mutableFloatStateOf(appGridRows.toFloat()) }
                val haptic = LocalHapticFeedback.current
                val hapticEnabled = LocalHapticEnabled.current
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Columns", style = MaterialTheme.typography.bodyMedium)
                        Text("${localColumns.toInt()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = localColumns,
                        onValueChange = { newValue ->
                            if (hapticEnabled && newValue != localColumns) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            localColumns = newValue
                        },
                        onValueChangeFinished = { viewModel.setAppGridColumns(localColumns.toInt()) },
                        valueRange = 3f..6f,
                        steps = 2,
                        track = { FilledPillSliderTrack(it) },
                        thumb = {}
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rows", style = MaterialTheme.typography.bodyMedium)
                        Text("${localRows.toInt()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = localRows,
                        onValueChange = { newValue ->
                            if (hapticEnabled && newValue != localRows) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            localRows = newValue
                        },
                        onValueChangeFinished = { viewModel.setAppGridRows(localRows.toInt()) },
                        valueRange = 3f..6f,
                        steps = 2,
                        track = { FilledPillSliderTrack(it) },
                        thumb = {}
                    )
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
                modifier = Modifier.size(52.dp).clip(CircleShape)
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
private fun HideAppPickerDialog(
    apps: List<AppInfo>,
    onHide: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hide Apps") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps") },
                    singleLine = true
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(340.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onHide(app.packageName) }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AppIconImage(
                                icon = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier.size(52.dp).clip(CircleShape)
                            )
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
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
            Triple("caffeine",          "Caffeine",   Icons.Filled.FreeBreakfast),
            Triple("clipboard",         "Clipboard",  Icons.Filled.ContentPaste),
            Triple("notes",             "Notes",      Icons.Filled.EditNote),
            Triple("calculator",        "Calculator", Icons.Filled.Calculate),
            Triple("wifi",              "Wifi",       Icons.Filled.Wifi),
            Triple("bluetooth",         "Bluetooth",  Icons.Filled.Bluetooth),
            Triple("mobile_data",       "Mobile Data", Icons.Filled.SignalCellular4Bar),
            Triple("share",             "Share",      Icons.Filled.Share),
            Triple("notif_history",     "Notifications", Icons.Filled.Notifications)
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Enable Quick Controls", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Show quick controls in the Dock. Swipe up to show the quick controls. Swipe down to hide them.",
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
                    val hapticEnabled = LocalHapticEnabled.current
                    var localActiveIds by remember(activeIds) { mutableStateOf(activeIds) }
                    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                        localActiveIds = localActiveIds.toMutableList().apply {
                            if (from.index in indices && to.index in indices) add(to.index, removeAt(from.index))
                        }
                    }
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(localActiveIds, key = { it }) { id ->
                            ReorderableItem(reorderableState, key = id) { isDragging ->
                                val meta = metaMap[id] ?: return@ReorderableItem
                                ActiveControlItem(
                                    label = meta.second,
                                    icon = meta.third,
                                    isDragging = isDragging,
                                    modifier = Modifier.longPressDraggableHandle(
                                        onDragStarted = {
                                            if (hapticEnabled) haptic.performHapticFeedback(
                                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                            )
                                        },
                                        onDragStopped = {
                                            viewModel.saveControlsOrder(
                                                localActiveIds + controlsOrder.filter { it in controlsDisabledIds }
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
                    CircleShape
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
                        shape = CircleShape
                    )
                    .clip(CircleShape)
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

