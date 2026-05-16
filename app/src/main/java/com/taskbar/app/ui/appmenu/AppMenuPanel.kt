package com.taskbar.app.ui.appmenu

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.taskbar.app.viewmodel.AppMenuViewModel
import com.taskbar.app.viewmodel.TaskbarViewModel

@Composable
fun AppMenuPanel(
    viewModel: AppMenuViewModel,
    taskbarViewModel: TaskbarViewModel,
    onHideTaskbar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val menuVisible by viewModel.menuVisible.collectAsState()
    val apps by viewModel.filteredApps.collectAsState()
    val pinnedPackages by viewModel.pinnedPackages.collectAsState()
    val quickControls by viewModel.quickControlsState.collectAsState()
    val stripEnabled by taskbarViewModel.quickControlsStripEnabled.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val panelColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface
    val context = LocalContext.current

    AnimatedVisibility(
        visible = menuVisible,
        enter = slideInVertically(
            animationSpec = spring(),
            initialOffsetY = { it / 4 }
        ) + fadeIn(),
        exit = slideOutVertically(
            animationSpec = spring(),
            targetOffsetY = { it / 4 }
        ) + fadeOut(),
        modifier = modifier.clipToBounds()
    ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                color = panelColor,
                tonalElevation = if (surfaceTintColor != 0L) 0.dp else 4.dp,
                shadowElevation = 8.dp
            ) {
            Column(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    onClick = { viewModel.openSearch() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Search apps…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    AppGrid(
                        apps = apps,
                        pinnedPackages = pinnedPackages,
                        onLaunchApp = { pkg -> viewModel.launchApp(pkg); onHideTaskbar() },
                        onPinApp = viewModel::pinApp,
                        onUnpinApp = viewModel::unpinApp,
                        modifier = if (stripEnabled) Modifier.weight(1f) else Modifier.weight(0.8f)
                    )

                    if (!stripEnabled) {
                        QuickControls(
                            state = quickControls,
                            onToggleTorch = viewModel::toggleTorch,
                            onCycleRingerMode = viewModel::cycleRingerMode,
                            onToggleAutoRotate = viewModel::toggleAutoRotate,
                            onToggleAutoBrightness = viewModel::toggleAutoBrightness,
                            onRequestWriteSettings = {
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            onToggleDnd = viewModel::toggleDnd,
                            onRequestDndPermission = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            onOpenQrScanner = { viewModel.openQrScanner(); onHideTaskbar() },
                            onShowPowerMenu = { viewModel.showPowerMenu(); onHideTaskbar() },
                            modifier = Modifier.weight(0.2f)
                        )
                    }
                }
            }
            }   // Surface
            }   // inner Box (margin wrapper)
    }           // AnimatedVisibility
}
