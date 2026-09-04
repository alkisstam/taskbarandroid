package com.alkisstam.taskbar.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.ui.common.toComposeShape
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.ui.theme.glassSheen
import com.alkisstam.taskbar.ui.theme.grain
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import kotlinx.coroutines.launch

@Composable
fun QuickSettingsPanel(
    taskbarViewModel: TaskbarViewModel,
    hasNotificationListenerPermission: Boolean,
    onDismiss: () -> Unit,
    panelOutlineEnabled: Boolean = false,
    translucentMode: Boolean = false,
    translucentAlpha: Float = 0.80f,
    grainAlpha: Float = 0.10f,
    surfaceTintColor: Long = 0L,
    dockBottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current

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

    val panelShape = RoundedCornerShape(24.dp)
    val panelColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 4.dp, start = 2.dp, end = 2.dp)
                .padding(bottom = dockBottomPadding)
                .then(if (panelOutlineEnabled) Modifier.border(1.dp, TaskbarOutlineGreen, panelShape) else Modifier)
                .clip(panelShape)
                .grain(enabled = translucentMode && grainAlpha > 0f, alpha = grainAlpha)
                .glassSheen(enabled = translucentMode && !panelOutlineEnabled, shape = panelShape),
            shape = panelShape,
            color = if (translucentMode) panelColor.copy(alpha = translucentAlpha) else panelColor,
            tonalElevation = if (translucentMode || surfaceTintColor != 0L) 0.dp else 2.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (page) {
                            0 -> Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                BehaviourCard(viewModel = taskbarViewModel)
                                MusicPanelSettingsCard(
                                    viewModel = taskbarViewModel,
                                    context = context,
                                    notificationAccessGranted = hasNotificationListenerPermission
                                )
                                SearchSettingsCard(viewModel = taskbarViewModel)
                            }
                            1 -> {
                                val pinnedApps by taskbarViewModel.pinnedApps.collectAsState()
                                val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
                                val appGridColumns by taskbarViewModel.appGridColumns.collectAsState()
                                val appGridRows by taskbarViewModel.appGridRows.collectAsState()
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    PinnedAppsReorderCard(
                                        viewModel = taskbarViewModel,
                                        pinnedApps = pinnedApps,
                                        iconShape = taskbarSettings.iconShape.toComposeShape()
                                    )
                                    AppOrderCard(viewModel = taskbarViewModel)
                                    AppGridCard(viewModel = taskbarViewModel, appGridColumns = appGridColumns, appGridRows = appGridRows)
                                }
                            }
                            2 -> Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                QuickControlsToggleCard(viewModel = taskbarViewModel)
                            }
                            3 -> PillSettingsScreen(viewModel = taskbarViewModel)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(40.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Surface(
                                        modifier = Modifier.clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
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
                                                tabIcons[index],
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1
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
                                            tabIcons[index],
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
    }
}
