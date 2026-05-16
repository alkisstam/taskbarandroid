package com.taskbar.app.ui.settings

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.taskbar.app.data.AppInfo
import com.taskbar.app.data.ThemeMode
import com.taskbar.app.ui.common.AppIconImage
import com.taskbar.app.viewmodel.TaskbarViewModel
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Pinned Apps", "Design")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TaskBar Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> GeneralTab(
                    viewModel = viewModel,
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestAccessibilityPermission = onRequestAccessibilityPermission
                )
                1 -> PinnedAppsTab(viewModel = viewModel)
                2 -> PillSettingsScreen(viewModel = viewModel)
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
    onRequestAccessibilityPermission: () -> Unit
) {
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val surfaceTintColor by viewModel.surfaceTintColor.collectAsState()
    val autoHideInFullscreen by viewModel.autoHideInFullscreen.collectAsState()
    val autoHideInLandscape by viewModel.autoHideInLandscape.collectAsState()
    val quickControlsStripEnabled by viewModel.quickControlsStripEnabled.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsCard(title = "Overlay Status") {
            if (!hasOverlayPermission) {
                Text(
                    text = "TaskBar requires the \"Draw over other apps\" permission to show the taskbar above other apps.",
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
                            text = if (overlayEnabled) "TaskBar is running" else "TaskBar is stopped",
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
                        "Hide the taskbar when an app goes fullscreen",
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
                        "Hide the taskbar when the device is in landscape",
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
                Column {
                    Text("Quick Controls Strip", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Show a quick controls bar above the taskbar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = quickControlsStripEnabled,
                    onCheckedChange = { viewModel.setQuickControlsStripEnabled(it) }
                )
            }
        }

        SettingsCard(title = "Navigation Bar Overlay") {
            if (!hasAccessibilityPermission) {
                Text(
                    text = "Enable the TaskBar Accessibility Service to allow the overlay to draw above the system navigation bar.",
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
private fun PinnedAppsTab(viewModel: TaskbarViewModel) {
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val pinnedPackages = remember(pinnedApps) { pinnedApps.map { it.packageName }.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
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
internal fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
    "Forest" to 0xFF1B4332,
    "Slate" to 0xFF2D3748,
    "Charcoal" to 0xFF36454F,
    "Rose" to 0xFF4A1528,
    "Midnight" to 0xFF0D0D0D
)

@Composable
private fun SurfaceTintColorPicker(
    currentColor: Long,
    onColorSelected: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Surface Tint Color", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Applies to the taskbar, app menu, and quick controls strip",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }
    }
}
