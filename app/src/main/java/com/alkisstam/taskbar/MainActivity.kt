package com.alkisstam.taskbar

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.content.pm.PackageInfoCompat
import com.alkisstam.taskbar.data.GestureAction
import com.alkisstam.taskbar.data.PillEdgePosition
import com.alkisstam.taskbar.service.OverlayService
import com.alkisstam.taskbar.ui.onboarding.OnboardingScreen
import com.alkisstam.taskbar.ui.settings.SettingsScreen
import com.alkisstam.taskbar.ui.common.LocalHapticEnabled
import com.alkisstam.taskbar.ui.theme.TaskBarTheme
import com.alkisstam.taskbar.ui.whatsnew.WhatsNewDialog
import com.alkisstam.taskbar.ui.whatsnew.whatsNewReleases
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val taskbarViewModel: TaskbarViewModel by viewModels()

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasWriteSettingsPermission by mutableStateOf(false)
    private var hasNotificationPolicyPermission by mutableStateOf(false)
    private var hasNotificationsPermission by mutableStateOf(false)
    private var hasNotificationListenerPermission by mutableStateOf(false)
    private var hasBatteryOptimizationExcluded by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(this)
    }

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationsPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasWriteSettingsPermission = Settings.System.canWrite(this)
        hasNotificationPolicyPermission = getNotificationPolicyAccess()
        hasNotificationsPermission = getNotificationsPermission()
        hasNotificationListenerPermission = getNotificationListenerPermission()
        hasBatteryOptimizationExcluded = getBatteryOptimizationExcluded()

        setContent {
            val themeMode by taskbarViewModel.themeMode.collectAsState()
            val hasAccessibilityPermission by taskbarViewModel.isAccessibilityEnabled.collectAsState()
            val onboardingComplete by taskbarViewModel.onboardingComplete.collectAsState()
            val pillSettings by taskbarViewModel.pillSettings.collectAsState()
            val hapticEnabled by taskbarViewModel.hapticFeedbackEnabled.collectAsState()
            val lastSeenVersionCode by taskbarViewModel.lastSeenVersionCode.collectAsState()
            TaskBarTheme(themeMode = themeMode) {
              CompositionLocalProvider(LocalHapticEnabled provides hapticEnabled) {
                when (onboardingComplete) {
                    null -> Box(modifier = Modifier.fillMaxSize())
                    false -> OnboardingScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        hasWriteSettingsPermission = hasWriteSettingsPermission,
                        hasNotificationPolicyPermission = hasNotificationPolicyPermission,
                        hasNotificationsPermission = hasNotificationsPermission,
                        hasNotificationListenerPermission = hasNotificationListenerPermission,
                        hasBatteryOptimizationExcluded = hasBatteryOptimizationExcluded,
                        selectedPosition = pillSettings.edgePosition,
                        onPositionSelected = { pos ->
                            val (w, h) = if (pos == PillEdgePosition.BOTTOM) 220f to 20f else 8f to 40f
                            val (swipeUp, swipeDown, doubleTap) = if (pos == PillEdgePosition.BOTTOM)
                                Triple(GestureAction.DISABLED, GestureAction.DISABLED, GestureAction.SHOW_DOCK)
                            else
                                Triple(GestureAction.SHOW_DOCK, GestureAction.DISABLED, GestureAction.DISABLED)
                            taskbarViewModel.savePillSettings(pillSettings.copy(
                                edgePosition = pos, widthDp = w, heightDp = h,
                                swipeUpAction = swipeUp, swipeDownAction = swipeDown, doubleTapAction = doubleTap
                            ))
                        },
                        onRequestOverlayPermission = ::requestOverlayPermission,
                        onRequestAccessibilityPermission = ::requestAccessibilityPermission,
                        onRequestWriteSettingsPermission = ::requestWriteSettingsPermission,
                        onRequestNotificationPolicyPermission = ::requestNotificationPolicyPermission,
                        onRequestNotificationsPermission = ::requestNotificationsPermission,
                        onRequestNotificationListenerPermission = ::requestNotificationListenerPermission,
                        onRequestBatteryOptimizationExclusion = ::requestBatteryOptimizationExclusion,
                        onComplete = taskbarViewModel::completeOnboarding
                    )
                    true -> {
                        val currentVersionCode = remember { getAppVersionCode() }
                        var showWhatsNew by remember { mutableStateOf(false) }
                        var seenAtDialogOpen by remember { mutableStateOf(0) }
                        LaunchedEffect(lastSeenVersionCode) {
                            val seen = lastSeenVersionCode ?: return@LaunchedEffect
                            when {
                                seen == 0 -> taskbarViewModel.markVersionSeen(currentVersionCode)
                                seen < currentVersionCode -> {
                                    seenAtDialogOpen = seen
                                    showWhatsNew = true
                                }
                            }
                        }
                        SettingsScreen(
                            viewModel = taskbarViewModel,
                            hasOverlayPermission = hasOverlayPermission,
                            hasAccessibilityPermission = hasAccessibilityPermission,
                            hasNotificationListenerPermission = hasNotificationListenerPermission,
                            onRequestOverlayPermission = ::requestOverlayPermission,
                            onRequestAccessibilityPermission = ::requestAccessibilityPermission
                        )
                        if (showWhatsNew) {
                            val releases = whatsNewReleases.filter {
                                it.versionCode in (seenAtDialogOpen + 1)..currentVersionCode
                            }
                            if (releases.isNotEmpty()) {
                                WhatsNewDialog(
                                    releases = releases,
                                    onDismiss = {
                                        showWhatsNew = false
                                        taskbarViewModel.markVersionSeen(currentVersionCode)
                                    }
                                )
                            } else {
                                // No changelog entry for this version range — don't wedge
                                // lastSeenVersionCode behind currentVersionCode forever.
                                taskbarViewModel.markVersionSeen(currentVersionCode)
                            }
                        }
                    }
                }
              }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sendBroadcast(Intent(OverlayService.ACTION_SETTINGS_OPEN).setPackage(packageName))
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasWriteSettingsPermission = Settings.System.canWrite(this)
        hasNotificationPolicyPermission = getNotificationPolicyAccess()
        hasNotificationsPermission = getNotificationsPermission()
        hasNotificationListenerPermission = getNotificationListenerPermission()
        hasBatteryOptimizationExcluded = getBatteryOptimizationExcluded()
    }

    private fun getNotificationPolicyAccess(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun getNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStop() {
        super.onStop()
        sendBroadcast(Intent(OverlayService.ACTION_SETTINGS_CLOSE).setPackage(packageName))
    }

    private fun launchPermissionIntent(intent: Intent) {
        try {
            permissionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestOverlayPermission() {
        launchPermissionIntent(Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ))
    }

    private fun requestAccessibilityPermission() {
        launchPermissionIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestWriteSettingsPermission() {
        launchPermissionIntent(Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:$packageName")
        ))
    }

    private fun requestNotificationPolicyPermission() {
        launchPermissionIntent(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun getNotificationListenerPermission(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val cn = ComponentName(this, com.alkisstam.taskbar.service.MediaListenerService::class.java)
        return flat.split(":").any { ComponentName.unflattenFromString(it.trim()) == cn }
    }

    private fun requestNotificationListenerPermission() {
        launchPermissionIntent(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun getAppVersionCode(): Int {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun getBatteryOptimizationExcluded(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestBatteryOptimizationExclusion() {
        // The list screen instead of the direct-request dialog: the dialog needs
        // REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, which Play policy restricts.
        launchPermissionIntent(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}
