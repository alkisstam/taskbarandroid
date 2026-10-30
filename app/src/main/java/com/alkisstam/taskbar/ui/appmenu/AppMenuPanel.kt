package com.alkisstam.taskbar.ui.appmenu

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.IconShape
import com.alkisstam.taskbar.ui.common.AppIconImage
import com.alkisstam.taskbar.ui.common.toComposeShape
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.ui.theme.glassSheen
import com.alkisstam.taskbar.ui.theme.GlassBackdrop
import com.alkisstam.taskbar.ui.theme.GlassBlur
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
    val floatingWindowsEnabled by viewModel.floatingWindowsEnabled.collectAsState()
    val floatingWindowsOnTap by viewModel.floatingWindowsOnTap.collectAsState()
    val altLaunchLabel = when {
        !floatingWindowsEnabled -> null
        floatingWindowsOnTap -> stringResource(R.string.app_action_open_fullscreen)
        else -> stringResource(R.string.app_action_open_floating)
    }
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val panelOutlineEnabled by taskbarViewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by taskbarViewModel.translucentMode.collectAsState()
    val translucentAlpha by taskbarViewModel.translucentAlpha.collectAsState()
    val grainAlpha by taskbarViewModel.grainAlpha.collectAsState()
    val appGridColumns by taskbarViewModel.appGridColumns.collectAsState()
    val appGridIconSize by taskbarViewModel.appGridIconSize.collectAsState()
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val showRecentAppsRow by viewModel.showRecentAppsRow.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    val appSortOrder by viewModel.appSortOrder.collectAsState()
    val panelColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface

    val panelShape = RoundedCornerShape(32.dp)
    // Phone landscape leaves too little height for the grid once the recent row takes its share.
    val configuration = LocalConfiguration.current
    val shortLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenHeightDp < 600

    AnimatedVisibility(
        visible = menuVisible,
        enter = slideInVertically(
            animationSpec = spring(),
            initialOffsetY = { it / 8 }
        ) + fadeIn(),
        exit = fadeOut(tween(150)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(if (panelOutlineEnabled) Modifier.border(1.dp, TaskbarOutlineGreen, panelShape) else Modifier)
                .clip(panelShape)
                .grain(enabled = translucentMode && grainAlpha > 0f, alpha = grainAlpha)
                .glassSheen(enabled = translucentMode && !panelOutlineEnabled, shape = panelShape),
            shape = panelShape,
            // Without the blur behind it the surface tint shows as a white flash while closing.
            color = when {
                !translucentMode -> panelColor
                !menuVisible -> Color.Transparent
                else -> panelColor.copy(alpha = GlassBlur.surfaceAlpha(translucentAlpha))
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = if (translucentMode || surfaceTintColor != 0L) 0.dp else 4.dp,
            shadowElevation = if (translucentMode) 0.dp else 8.dp
        ) {
            // Samsung's blur ignores Compose alpha, so drop it as soon as the fade-out starts.
            GlassBackdrop(enabled = translucentMode && menuVisible, cornerRadius = 32.dp, tint = panelColor.copy(alpha = translucentAlpha), windowBlur = true) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 8.dp, end = 8.dp)) {
                if (showRecentAppsRow && recentApps.isNotEmpty() && !shortLandscape) {
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
                                        iconSize = appGridIconSize.dp,
                                        onLaunch = { viewModel.launchAppTap(app.packageName); onHideTaskbar() }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                }

                AppGrid(
                    apps = apps,
                    pinnedPackages = pinnedPackages,
                    onLaunchApp = { pkg -> viewModel.launchAppTap(pkg); onHideTaskbar() },
                    altLaunchLabel = altLaunchLabel,
                    onAltLaunchApp = { pkg -> viewModel.launchAppAlternate(pkg); onHideTaskbar() },
                    onPinApp = viewModel::pinApp,
                    onUnpinApp = viewModel::unpinApp,
                    onHideApp = viewModel::hideApp,
                    columns = appGridColumns,
                    iconShape = taskbarSettings.iconShape,
                    iconSize = appGridIconSize.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                val pillColor = if (translucentMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = CircleShape,
                        color = pillColor,
                        tonalElevation = if (translucentMode) 0.dp else 2.dp,
                        onClick = { viewModel.openSearch() }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.search_apps),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = pillColor,
                        tonalElevation = if (translucentMode) 0.dp else 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AppSortMenuButton(
                                currentOrder = appSortOrder,
                                onSelect = viewModel::setAppSortOrder
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
private fun RecentAppIcon(app: AppInfo, iconShape: IconShape, iconSize: Dp, onLaunch: () -> Unit) {
    AppIconImage(
        icon = app.icon,
        contentDescription = app.label,
        shape = iconShape.toComposeShape(),
        modifier = Modifier
            .widthIn(max = iconSize)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onLaunch)
    )
}
