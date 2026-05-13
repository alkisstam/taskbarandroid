package com.taskbar.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.taskbar.app.ui.settings.SettingsScreen
import com.taskbar.app.ui.theme.TaskBarTheme
import com.taskbar.app.viewmodel.TaskbarViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val taskbarViewModel: TaskbarViewModel by viewModels()

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasAccessibilityPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasAccessibilityPermission = taskbarViewModel.isAccessibilityServiceEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasAccessibilityPermission = taskbarViewModel.isAccessibilityServiceEnabled()

        setContent {
            val themeMode by taskbarViewModel.themeMode.collectAsState()
            TaskBarTheme(themeMode = themeMode) {
                SettingsScreen(
                    viewModel = taskbarViewModel,
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onRequestOverlayPermission = ::requestOverlayPermission,
                    onRequestAccessibilityPermission = ::requestAccessibilityPermission
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermission = Settings.canDrawOverlays(this)
        hasAccessibilityPermission = taskbarViewModel.isAccessibilityServiceEnabled()
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
}
