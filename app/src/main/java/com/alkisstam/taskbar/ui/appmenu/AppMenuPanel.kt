package com.alkisstam.taskbar.ui.appmenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.IconShape
import com.alkisstam.taskbar.ui.common.AppIconImage
import com.alkisstam.taskbar.ui.common.toComposeShape
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.ui.theme.grain
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel

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
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val appGridColumns by taskbarViewModel.appGridColumns.collectAsState()
    val appGridRows by taskbarViewModel.appGridRows.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val showRecentAppsRow by viewModel.showRecentAppsRow.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    val panelColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface
    val gridHeight = (appGridRows * 84).dp

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
            val glassBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .then(if (panelOutlineEnabled) Modifier.border(1.dp, TaskbarOutlineGreen, RoundedCornerShape(20.dp)) else Modifier)
                    .then(if (translucentMode && !panelOutlineEnabled) Modifier.border(1.dp, glassBorderColor, RoundedCornerShape(20.dp)) else Modifier)
                    .clip(RoundedCornerShape(20.dp))
                    .grain(enabled = translucentMode && grainAlpha > 0f, alpha = grainAlpha),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                color = if (translucentMode) panelColor.copy(alpha = translucentAlpha) else panelColor,
                tonalElevation = if (translucentMode || surfaceTintColor != 0L) 0.dp else 4.dp,
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
                    color = if (translucentMode) MaterialTheme.colorScheme.surface.copy(alpha = 0.70f) else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (translucentMode) 0.dp else 2.dp,
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

                if (showRecentAppsRow && recentApps.isNotEmpty()) {
                    // Same contentPadding + horizontalArrangement as AppGrid's LazyVerticalGrid
                    // below, so each icon's column center lines up with the grid's columns.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(appGridColumns) { index ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                recentApps.getOrNull(index)?.let { app ->
                                    RecentAppIcon(
                                        app = app,
                                        iconShape = taskbarSettings.iconShape,
                                        onLaunch = { viewModel.launchApp(app.packageName); onHideTaskbar() }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                }

                AppGrid(
                    apps = apps,
                    pinnedPackages = pinnedPackages,
                    onLaunchApp = { pkg -> viewModel.launchApp(pkg); onHideTaskbar() },
                    onPinApp = viewModel::pinApp,
                    onUnpinApp = viewModel::unpinApp,
                    onHideApp = viewModel::hideApp,
                    columns = appGridColumns,
                    iconShape = taskbarSettings.iconShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                )
            }
            }   // Surface
            }   // inner Box (margin wrapper)
    }           // AnimatedVisibility
}

@Composable
private fun RecentAppIcon(app: AppInfo, iconShape: IconShape, onLaunch: () -> Unit) {
    AppIconImage(
        icon = app.icon,
        contentDescription = app.label,
        shape = iconShape.toComposeShape(),
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onLaunch)
    )
}
