package com.alkisstam.taskbar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.alkisstam.taskbar.service.OverlayService
import com.alkisstam.taskbar.ui.onboarding.OnboardingScreen
import com.alkisstam.taskbar.ui.settings.SettingsScreen
import com.alkisstam.taskbar.ui.theme.TaskBarTheme
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val taskbarViewModel: TaskbarViewModel by viewModels()

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasWriteSettingsPermission by mutableStateOf(false)
    private var hasNotificationPolicyPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasWriteSettingsPermission = Settings.System.canWrite(this)
        hasNotificationPolicyPermission = getNotificationPolicyAccess()

        setContent {
            val themeMode by taskbarViewModel.themeMode.collectAsState()
            val hasAccessibilityPermission by taskbarViewModel.isAccessibilityEnabled.collectAsState()
            val onboardingComplete by taskbarViewModel.onboardingComplete.collectAsState()
            TaskBarTheme(themeMode = themeMode) {
                when (onboardingComplete) {
                    null -> Box(modifier = Modifier.fillMaxSize())
                    false -> OnboardingScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        hasWriteSettingsPermission = hasWriteSettingsPermission,
                        hasNotificationPolicyPermission = hasNotificationPolicyPermission,
                        onRequestOverlayPermission = ::requestOverlayPermission,
                        onRequestAccessibilityPermission = ::requestAccessibilityPermission,
                        onRequestWriteSettingsPermission = ::requestWriteSettingsPermission,
                        onRequestNotificationPolicyPermission = ::requestNotificationPolicyPermission,
                        onComplete = taskbarViewModel::completeOnboarding
                    )
                    true -> SettingsScreen(
                        viewModel = taskbarViewModel,
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        onRequestOverlayPermission = ::requestOverlayPermission,
                        onRequestAccessibilityPermission = ::requestAccessibilityPermission
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sendBroadcast(Intent(OverlayService.ACTION_SETTINGS_OPEN))
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasWriteSettingsPermission = Settings.System.canWrite(this)
        hasNotificationPolicyPermission = getNotificationPolicyAccess()
    }

    private fun getNotificationPolicyAccess(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    override fun onStop() {
        super.onStop()
        sendBroadcast(Intent(OverlayService.ACTION_SETTINGS_CLOSE))
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        permissionLauncher.launch(intent)
    }

    private fun requestAccessibilityPermission() {
        permissionLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestWriteSettingsPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:$packageName")
        )
        permissionLauncher.launch(intent)
    }

    private fun requestNotificationPolicyPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        permissionLauncher.launch(intent)
    }
}
