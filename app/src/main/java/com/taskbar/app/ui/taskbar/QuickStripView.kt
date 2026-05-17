package com.taskbar.app.ui.taskbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.taskbar.app.ui.appmenu.QuickControlItem
import com.taskbar.app.ui.appmenu.VolumePanel
import com.taskbar.app.ui.appmenu.toItems
import com.taskbar.app.viewmodel.AppMenuViewModel
import com.taskbar.app.viewmodel.TaskbarViewModel

@Composable
fun QuickStripView(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel,
    onHideTaskbar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val taskbarSettings by taskbarViewModel.taskbarSettings.collectAsState()
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val quickControls by appMenuViewModel.quickControlsState.collectAsState()
    val volumePanelVisible by appMenuViewModel.volumePanelVisible.collectAsState()
    val stripColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
    var stripHeightPx by remember { mutableStateOf(0) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .height(taskbarSettings.heightDp.dp)
                .onSizeChanged { stripHeightPx = it.height },
            shape = RoundedCornerShape(16.dp),
            color = stripColor,
            tonalElevation = if (surfaceTintColor != 0L) 0.dp else 3.dp,
            shadowElevation = 8.dp
        ) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickControls.toItems()) { item ->
                    QuickControlItem(
                        item = item,
                        onToggle = {
                            appMenuViewModel.handleQuickControlAction(item.id)
                            if (item.id == "qr" || item.id == "power") onHideTaskbar()
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = volumePanelVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset {
                    IntOffset(0, -(stripHeightPx + with(density) { 8.dp.roundToPx() }))
                }
        ) {
            VolumePanel(
                streams = appMenuViewModel.getVolumeStreams(),
                onVolumeChange = { streamType, value ->
                    appMenuViewModel.setStreamVolume(streamType, value)
                }
            )
        }

        if (volumePanelVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(taskbarSettings.heightDp.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { appMenuViewModel.dismissVolumePanel() }
            )
        }
    }
}
