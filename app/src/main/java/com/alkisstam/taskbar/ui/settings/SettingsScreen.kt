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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotationAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.res.stringResource
import com.alkisstam.taskbar.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.AppSortOrder
import com.alkisstam.taskbar.ui.common.AppIconImage
import com.alkisstam.taskbar.ui.common.LocalHapticEnabled
import com.alkisstam.taskbar.ui.common.toComposeShape
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskbarViewModel,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasWriteSettingsPermission: Boolean,
    hasNotificationPolicyPermission: Boolean,
    hasNotificationsPermission: Boolean,
    hasNotificationListenerPermission: Boolean,
    hasBatteryOptimizationExcluded: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestWriteSettingsPermission: () -> Unit,
    onRequestNotificationPolicyPermission: () -> Unit,
    onRequestNotificationsPermission: () -> Unit,
    onRequestNotificationListenerPermission: () -> Unit,
    onRequestBatteryOptimizationExclusion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        stringResource(R.string.settings_tab_general),
        stringResource(R.string.settings_tab_apps),
        stringResource(R.string.settings_tab_controls),
        stringResource(R.string.settings_tab_design)
    )
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
                title = { Text(stringResource(R.string.settings_top_bar_title)) },
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
                        hasWriteSettingsPermission = hasWriteSettingsPermission,
                        hasNotificationPolicyPermission = hasNotificationPolicyPermission,
                        hasNotificationsPermission = hasNotificationsPermission,
                        hasNotificationListenerPermission = hasNotificationListenerPermission,
                        hasBatteryOptimizationExcluded = hasBatteryOptimizationExcluded,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onRequestAccessibilityPermission = onRequestAccessibilityPermission,
                        onRequestWriteSettingsPermission = onRequestWriteSettingsPermission,
                        onRequestNotificationPolicyPermission = onRequestNotificationPolicyPermission,
                        onRequestNotificationsPermission = onRequestNotificationsPermission,
                        onRequestNotificationListenerPermission = onRequestNotificationListenerPermission,
                        onRequestBatteryOptimizationExclusion = onRequestBatteryOptimizationExclusion,
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
    hasWriteSettingsPermission: Boolean,
    hasNotificationPolicyPermission: Boolean,
    hasNotificationsPermission: Boolean,
    hasNotificationListenerPermission: Boolean,
    hasBatteryOptimizationExcluded: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestWriteSettingsPermission: () -> Unit,
    onRequestNotificationPolicyPermission: () -> Unit,
    onRequestNotificationsPermission: () -> Unit,
    onRequestNotificationListenerPermission: () -> Unit,
    onRequestBatteryOptimizationExclusion: () -> Unit,
    bottomPadding: Dp = 0.dp
) {
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()
    val context = LocalContext.current
    var batteryUsageExpanded by remember { mutableStateOf(false) }
    var permissionsExpanded by remember { mutableStateOf(false) }

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
        SettingsCard(title = stringResource(R.string.settings_overlay_status_title)) {
            if (!hasOverlayPermission) {
                Text(
                    text = stringResource(R.string.settings_overlay_permission_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRequestOverlayPermission) {
                    Text(stringResource(R.string.settings_grant_permission_button))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (overlayEnabled) stringResource(R.string.settings_overlay_status_running) else stringResource(R.string.settings_overlay_status_stopped),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (overlayEnabled) stringResource(R.string.settings_overlay_status_running_desc) else stringResource(R.string.settings_overlay_status_stopped_desc),
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
                            Text(stringResource(R.string.settings_stop_button))
                        }
                    } else {
                        Button(onClick = viewModel::startOverlay) {
                            Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text(stringResource(R.string.settings_start_button))
                        }
                    }
                }
            }
        }

        SettingsCard(title = stringResource(R.string.settings_language_card_title)) {
            val appLanguageTag by viewModel.appLanguageTag.collectAsState()
            GradientDropdownField(
                icon = Icons.Filled.Language,
                selectedLabel = LANGUAGE_OPTIONS.firstOrNull { it.second == appLanguageTag }?.first
                    ?: stringResource(R.string.settings_language_system_default),
                options = LANGUAGE_OPTIONS,
                isSelected = { it == appLanguageTag },
                onSelect = { viewModel.setAppLanguage(it) }
            )
        }

        BehaviourCard(viewModel = viewModel)

        MusicPanelSettingsCard(
            viewModel = viewModel,
            context = context,
            notificationAccessGranted = hasNotificationListenerPermission
        )

        SearchSettingsCard(viewModel = viewModel)

        SettingsCard(title = stringResource(R.string.settings_navbar_overlay_title)) {
            if (!hasAccessibilityPermission) {
                Text(
                    text = stringResource(R.string.settings_navbar_overlay_enable_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRequestAccessibilityPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_enable_accessibility_button))
                }
            } else {
                Text(
                    text = stringResource(R.string.settings_navbar_overlay_active_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        ExpandableSection(
            title = stringResource(R.string.settings_permissions_card_title),
            expanded = permissionsExpanded,
            onToggle = { permissionsExpanded = !permissionsExpanded }
        ) {
            PermissionStatusRow(
                name = stringResource(R.string.settings_permission_overlay_name),
                description = stringResource(R.string.settings_permission_overlay_desc),
                granted = hasOverlayPermission,
                onClick = onRequestOverlayPermission
            )
            PermissionStatusRow(
                name = stringResource(R.string.settings_permission_accessibility_name),
                description = stringResource(R.string.settings_permission_accessibility_desc),
                granted = hasAccessibilityPermission,
                onClick = onRequestAccessibilityPermission
            )
            PermissionStatusRow(
                name = stringResource(R.string.settings_permission_write_settings_name),
                description = stringResource(R.string.settings_permission_write_settings_desc),
                granted = hasWriteSettingsPermission,
                onClick = onRequestWriteSettingsPermission
            )
            PermissionStatusRow(
                name = stringResource(R.string.settings_permission_dnd_name),
                description = stringResource(R.string.settings_permission_dnd_desc),
                granted = hasNotificationPolicyPermission,
                onClick = onRequestNotificationPolicyPermission
            )
            PermissionStatusRow(
                name = stringResource(R.string.settings_permission_notifications_name),
                description = stringResource(R.string.settings_permission_notifications_desc),
                granted = hasNotificationsPermission,
                onClick = onRequestNotificationsPermission
            )
            PermissionStatusRow(
                name = stringResource(R.string.settings_permission_notification_listener_name),
                description = stringResource(R.string.settings_permission_notification_listener_desc),
                granted = hasNotificationListenerPermission,
                onClick = onRequestNotificationListenerPermission
            )
            PermissionStatusRow(
                name = stringResource(R.string.settings_permission_battery_name),
                description = stringResource(R.string.settings_permission_battery_desc),
                granted = hasBatteryOptimizationExcluded,
                onClick = onRequestBatteryOptimizationExclusion
            )
        }

        ExpandableSection(
            title = stringResource(R.string.settings_battery_usage_section_title),
            expanded = batteryUsageExpanded,
            onToggle = { batteryUsageExpanded = !batteryUsageExpanded }
        ) {
            Text(
                text = stringResource(R.string.settings_battery_usage_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            BatteryUsageDeviceSection(R.string.settings_battery_usage_samsung_title, R.string.settings_battery_usage_samsung_steps)
            BatteryUsageDeviceSection(R.string.settings_battery_usage_miui_title, R.string.settings_battery_usage_miui_steps)
            BatteryUsageDeviceSection(R.string.settings_battery_usage_oneplus_title, R.string.settings_battery_usage_oneplus_steps)
            BatteryUsageDeviceSection(R.string.settings_battery_usage_redmi_title, R.string.settings_battery_usage_redmi_steps)
            BatteryUsageDeviceSection(R.string.settings_battery_usage_oppo_title, R.string.settings_battery_usage_oppo_steps)
            BatteryUsageDeviceSection(R.string.settings_battery_usage_vivo_title, R.string.settings_battery_usage_vivo_steps)
            BatteryUsageDeviceSection(R.string.settings_battery_usage_realme_title, R.string.settings_battery_usage_realme_steps)
            BatteryUsageDeviceSection(R.string.settings_battery_usage_motorola_title, R.string.settings_battery_usage_motorola_steps)
            BatteryUsageDeviceSection(R.string.settings_battery_usage_pixel_title, R.string.settings_battery_usage_pixel_steps)
            BatteryUsageDeviceSection(R.string.settings_battery_usage_huawei_title, R.string.settings_battery_usage_huawei_steps, isLast = true)
        }

        SettingsCard(title = stringResource(R.string.settings_backup_restore_title)) {
            OutlinedButton(
                onClick = { backupLauncher.launch("taskbar_backup.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_backup_button))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_restore_button))
            }
        }

        var showResetDialog by remember { mutableStateOf(false) }
        SettingsCard(title = stringResource(R.string.settings_reset_card_title)) {
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_reset_button))
            }
        }
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
                text = { Text(stringResource(R.string.settings_reset_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetAllSettings()
                        showResetDialog = false
                    }) {
                        Text(stringResource(R.string.settings_reset_confirm_button), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.settings_reset_dialog_cancel_button))
                    }
                }
            )
        }

        SettingsCard(title = stringResource(R.string.settings_contact_feedback_card_title)) {
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
                                Toast.makeText(context, context.getString(R.string.settings_no_email_app_toast), Toast.LENGTH_SHORT).show()
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
                            stringResource(R.string.settings_contact_feedback_label),
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
                                Toast.makeText(context, context.getString(R.string.settings_no_link_app_toast), Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_telegram),
                        contentDescription = stringResource(R.string.settings_telegram_content_description),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun BehaviourCard(viewModel: TaskbarViewModel) {
    val autoHideInFullscreen by viewModel.autoHideInFullscreen.collectAsState()
    val autoHideInLandscape by viewModel.autoHideInLandscape.collectAsState()
    val disableOnLockscreen by viewModel.disableOnLockscreen.collectAsState()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()

    SettingsCard(title = stringResource(R.string.settings_behaviour_card_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_autohide_fullscreen_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_autohide_fullscreen_desc),
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
                Text(stringResource(R.string.settings_autohide_landscape_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_autohide_landscape_desc),
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
                Text(stringResource(R.string.settings_disable_lockscreen_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_disable_lockscreen_desc),
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
                Text(stringResource(R.string.settings_vibrate_feedback_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_vibrate_feedback_desc),
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
}

@Composable
internal fun MusicPanelSettingsCard(
    viewModel: TaskbarViewModel,
    context: android.content.Context,
    notificationAccessGranted: Boolean
) {
    val musicPanelEnabled by viewModel.musicPanelEnabled.collectAsState()
    val alwaysShowMusicPanel by viewModel.alwaysShowMusicPanel.collectAsState()

    SettingsCard(title = stringResource(R.string.settings_music_panel_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_show_music_panel_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_show_music_panel_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = musicPanelEnabled,
                onCheckedChange = { viewModel.setMusicPanelEnabled(it) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_always_show_panel_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_always_show_panel_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = alwaysShowMusicPanel,
                onCheckedChange = { viewModel.setAlwaysShowMusicPanel(it) }
            )
        }
        if (!notificationAccessGranted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_music_panel_notification_required_desc),
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
                        Toast.makeText(context, context.getString(R.string.settings_couldnt_open_settings_toast), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_grant_notification_access_button))
            }
        }
    }
}

@Composable
internal fun SearchSettingsCard(viewModel: TaskbarViewModel) {
    val fuzzySearchEnabled by viewModel.fuzzySearchEnabled.collectAsState()
    val showRecentApps by viewModel.showRecentApps.collectAsState()
    SettingsCard(title = stringResource(R.string.settings_search_card_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_fuzzy_search_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_fuzzy_search_desc),
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
                Text(stringResource(R.string.settings_show_recent_apps_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_show_recent_apps_desc),
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
    val taskbarSettings by viewModel.taskbarSettings.collectAsState()
    val iconShape = taskbarSettings.iconShape.toComposeShape()
    val appGridColumns by viewModel.appGridColumns.collectAsState()
    val appGridRows by viewModel.appGridRows.collectAsState()
    val showRecentAppsRow by viewModel.showRecentAppsRow.collectAsState()
    var showHideAppPicker by remember { mutableStateOf(false) }

    if (showHideAppPicker) {
        HideAppPickerDialog(
            apps = allApps,
            iconShape = iconShape,
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
                text = stringResource(R.string.settings_pinned_apps_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            PinnedAppsReorderCard(viewModel = viewModel, pinnedApps = pinnedApps, iconShape = iconShape)
        }
        item {
            SettingsCard(title = stringResource(R.string.settings_hidden_apps_title, hiddenApps.size)) {
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
                                    contentDescription = stringResource(R.string.settings_add_app_content_description),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = stringResource(R.string.settings_add_label),
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
                                    shape = iconShape,
                                    modifier = Modifier.size(48.dp)
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
                                        contentDescription = stringResource(R.string.settings_unhide_content_description),
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
            SettingsCard(title = stringResource(R.string.settings_all_apps_title, allApps.size)) {
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
                            iconShape = iconShape,
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
            SettingsCard(title = stringResource(R.string.settings_recent_apps_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_show_recent_apps_panel_title), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.settings_show_recent_apps_panel_desc),
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
            AppOrderCard(viewModel = viewModel)
        }
        item {
            AppGridCard(viewModel = viewModel, appGridColumns = appGridColumns, appGridRows = appGridRows)
        }
    }
}

@Composable
internal fun PinnedAppsReorderCard(viewModel: TaskbarViewModel, pinnedApps: List<AppInfo>, iconShape: Shape) {
    SettingsCard(title = stringResource(R.string.settings_pinned_apps_title, pinnedApps.size)) {
        if (pinnedApps.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_pinned_apps_empty_desc),
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
                                    shape = iconShape,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .then(
                                            if (isDragging) Modifier.background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                iconShape
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
                                        contentDescription = stringResource(R.string.settings_pinned_app_unpin_content_description),
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

@Composable
internal fun AppOrderCard(viewModel: TaskbarViewModel) {
    SettingsCard(title = stringResource(R.string.settings_app_order_title)) {
        Text(
            text = stringResource(R.string.settings_app_order_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val appSortOrder by viewModel.appSortOrder.collectAsState()
        val options = listOf(
            stringResource(R.string.app_sort_order_name) to AppSortOrder.NAME,
            stringResource(R.string.app_sort_order_install_time) to AppSortOrder.INSTALL_TIME,
            stringResource(R.string.app_sort_order_usage) to AppSortOrder.USAGE
        )
        GradientDropdownField(
            icon = Icons.AutoMirrored.Filled.Sort,
            selectedLabel = options.first { it.second == appSortOrder }.first,
            options = options,
            isSelected = { it == appSortOrder },
            onSelect = { viewModel.setAppSortOrder(it) }
        )
    }
}

@Composable
internal fun AppGridCard(viewModel: TaskbarViewModel, appGridColumns: Int, appGridRows: Int) {
    SettingsCard(title = stringResource(R.string.settings_app_grid_title)) {
        Column {
            SettingsSlider(
                label = stringResource(R.string.settings_app_grid_columns_label),
                value = appGridColumns.toFloat(),
                valueRange = 3f..6f,
                unit = "",
                step = 1f,
                sliderSteps = 2,
                displayTransform = { "${it.toInt()}" },
                onValueChange = { viewModel.setAppGridColumns(it.roundToInt()) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SettingsSlider(
                label = stringResource(R.string.settings_app_grid_rows_label),
                value = appGridRows.toFloat(),
                valueRange = 3f..6f,
                unit = "",
                step = 1f,
                sliderSteps = 2,
                displayTransform = { "${it.toInt()}" },
                onValueChange = { viewModel.setAppGridRows(it.roundToInt()) }
            )
        }
    }
}

@Composable
internal fun AllAppGridItem(
    app: AppInfo,
    isPinned: Boolean,
    iconShape: Shape = RectangleShape,
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
                shape = iconShape,
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
                    contentDescription = if (isPinned) stringResource(R.string.settings_all_apps_unpin_content_description) else stringResource(R.string.settings_all_apps_pin_content_description),
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
    iconShape: Shape = RectangleShape,
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
        title = { Text(stringResource(R.string.settings_hide_apps_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.settings_hide_apps_search_placeholder)) },
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
                                shape = iconShape,
                                modifier = Modifier.size(52.dp)
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_hide_apps_done_button)) } }
    )
}

@Composable
internal fun QuickControlsToggleCard(viewModel: TaskbarViewModel) {
    val quickControlsEnabled by viewModel.quickControlsEnabled.collectAsState()
    val taskbarSettings by viewModel.taskbarSettings.collectAsState()
    val quickSettingsPanelEnabled by viewModel.quickSettingsPanelEnabled.collectAsState()

    SettingsCard(title = stringResource(R.string.settings_quick_controls_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(stringResource(R.string.settings_enable_quick_controls_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_enable_quick_controls_desc),
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
            Text(stringResource(R.string.settings_show_control_labels_title), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = taskbarSettings.showControlLabels,
                onCheckedChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(showControlLabels = it)) },
                enabled = quickControlsEnabled
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
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(stringResource(R.string.settings_keep_controls_expanded_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_keep_controls_expanded_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = taskbarSettings.keepDockExpanded,
                onCheckedChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(keepDockExpanded = it)) },
                enabled = quickControlsEnabled
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
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(stringResource(R.string.settings_enable_quick_settings_panel_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_enable_quick_settings_panel_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = quickSettingsPanelEnabled,
                onCheckedChange = { viewModel.setQuickSettingsPanelEnabled(it) },
                enabled = quickControlsEnabled
            )
        }
    }
}

@Composable
private fun ControlsTab(viewModel: TaskbarViewModel, bottomPadding: Dp = 0.dp) {
    val quickControlsEnabled by viewModel.quickControlsEnabled.collectAsState()
    val taskbarSettings by viewModel.taskbarSettings.collectAsState()
    val controlsOrder by viewModel.controlsOrder.collectAsState()
    val controlsDisabledIds by viewModel.controlsDisabledIds.collectAsState()

    val torchLabel = stringResource(R.string.settings_control_torch)
    val ringerLabel = stringResource(R.string.settings_control_ringer)
    val rotateLabel = stringResource(R.string.settings_control_rotate)
    val brightnessLabel = stringResource(R.string.settings_control_brightness)
    val dndLabel = stringResource(R.string.settings_control_dnd)
    val qrLabel = stringResource(R.string.settings_control_qr)
    val powerLabel = stringResource(R.string.settings_control_power)
    val volumeLabel = stringResource(R.string.settings_control_volume)
    val screenshotLabel = stringResource(R.string.settings_control_screenshot)
    val lockLabel = stringResource(R.string.settings_control_lock)
    val caffeineLabel = stringResource(R.string.settings_control_caffeine)
    val clipboardLabel = stringResource(R.string.settings_control_clipboard)
    val notesLabel = stringResource(R.string.settings_control_notes)
    val calculatorLabel = stringResource(R.string.settings_control_calculator)
    val wifiLabel = stringResource(R.string.settings_control_wifi)
    val bluetoothLabel = stringResource(R.string.settings_control_bluetooth)
    val shareLabel = stringResource(R.string.settings_control_share)
    val notifHistoryLabel = stringResource(R.string.settings_control_notifications)
    val controlMeta = remember(
        torchLabel, ringerLabel, rotateLabel, brightnessLabel, dndLabel, qrLabel, powerLabel,
        volumeLabel, screenshotLabel, lockLabel, caffeineLabel, clipboardLabel, notesLabel,
        calculatorLabel, wifiLabel, bluetoothLabel, shareLabel, notifHistoryLabel
    ) {
        listOf(
            Triple("torch",             torchLabel,      Icons.Filled.FlashlightOn),
            Triple("ringer",            ringerLabel,     Icons.AutoMirrored.Filled.VolumeUp),
            Triple("rotate",            rotateLabel,     Icons.Filled.ScreenRotationAlt),
            Triple("brightness_slider", brightnessLabel, Icons.Filled.BrightnessMedium),
            Triple("dnd",               dndLabel,        Icons.Filled.DoNotDisturbOff),
            Triple("qr",                qrLabel,         Icons.Filled.QrCodeScanner),
            Triple("power",             powerLabel,      Icons.Filled.PowerSettingsNew),
            Triple("volume",            volumeLabel,     Icons.Filled.Tune),
            Triple("screenshot",        screenshotLabel, Icons.Filled.PhotoCamera),
            Triple("lockscreen",        lockLabel,       Icons.Filled.Lock),
            Triple("caffeine",          caffeineLabel,   Icons.Filled.FreeBreakfast),
            Triple("clipboard",         clipboardLabel,  Icons.Filled.ContentPaste),
            Triple("notes",             notesLabel,      Icons.Filled.EditNote),
            Triple("calculator",        calculatorLabel, Icons.Filled.Calculate),
            Triple("wifi",              wifiLabel,       Icons.Filled.Wifi),
            Triple("bluetooth",         bluetoothLabel,  Icons.Filled.Bluetooth),
            Triple("share",             shareLabel,      Icons.Filled.Share),
            Triple("notif_history",     notifHistoryLabel, Icons.Filled.Notifications)
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
            QuickControlsToggleCard(viewModel = viewModel)
        }

        item {
            SettingsCard(
                title = stringResource(R.string.settings_active_controls_title),
                modifier = Modifier.alpha(if (quickControlsEnabled) 1f else 0.38f)
            ) {
                if (activeIds.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_no_controls_enabled_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(R.string.settings_active_controls_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                title = stringResource(R.string.settings_all_controls_title),
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
private fun PermissionStatusRow(
    name: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (granted) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = if (granted) stringResource(R.string.settings_permission_granted_content_description) else stringResource(R.string.settings_permission_not_granted_content_description),
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun BatteryUsageDeviceSection(titleRes: Int, stepsRes: Int, isLast: Boolean = false) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = stringResource(stepsRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (!isLast) {
        Spacer(modifier = Modifier.height(12.dp))
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

