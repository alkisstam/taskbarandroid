package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.ui.appmenu.QuickControlItem
import com.alkisstam.taskbar.ui.appmenu.toItems
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel

@Composable
fun QuickStripView(
    taskbarViewModel: TaskbarViewModel,
    appMenuViewModel: AppMenuViewModel,
    onHideTaskbar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val surfaceTintColor by taskbarViewModel.surfaceTintColor.collectAsState()
    val quickControls by appMenuViewModel.quickControlsState.collectAsState()
    val controlsOrder by taskbarViewModel.controlsOrder.collectAsState()
    val controlsDisabledIds by taskbarViewModel.controlsDisabledIds.collectAsState()
    val stripColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = screenWidthDp * 0.76f)
                .height(70.dp),
            shape = RoundedCornerShape(16.dp),
            color = stripColor,
            tonalElevation = if (surfaceTintColor != 0L) 0.dp else 3.dp,
            shadowElevation = 8.dp
        ) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickControls.toItems(controlsOrder, controlsDisabledIds)) { item ->
                    QuickControlItem(
                        item = item,
                        onToggle = {
                            appMenuViewModel.handleQuickControlAction(item.id)
                            if (item.id in listOf("qr", "power", "screenshot", "lockscreen")) onHideTaskbar()
                        }
                    )
                }
            }
        }
    }
}
