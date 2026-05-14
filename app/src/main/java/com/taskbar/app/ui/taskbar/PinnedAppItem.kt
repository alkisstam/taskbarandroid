package com.taskbar.app.ui.taskbar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import com.taskbar.app.data.AppInfo
import com.taskbar.app.ui.common.AppIconImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedAppItem(
    app: AppInfo,
    onLaunch: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .combinedClickable(
                    onClick = onLaunch,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                ),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                AppIconImage(
                    icon = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp)
                )
            }
        }

        if (showMenu) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -160),
                onDismissRequest = { showMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(140.dp)
                ) {
                    TextButton(
                        onClick = { onUnpin(); showMenu = false },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text("Unpin from TaskBar", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
