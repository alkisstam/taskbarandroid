package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.IconShape
import com.alkisstam.taskbar.ui.common.AppIconImage
import com.alkisstam.taskbar.ui.common.LocalHapticEnabled
import com.alkisstam.taskbar.ui.common.toComposeShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedAppItem(
    app: AppInfo,
    iconSize: Dp = 48.dp,
    showLabel: Boolean = false,
    iconShape: IconShape = IconShape.DEFAULT,
    onLaunch: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticEnabled.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIconImage(
            icon = app.icon,
            contentDescription = app.label,
            shape = iconShape.toComposeShape(),
            modifier = Modifier
                .size(iconSize)
                .combinedClickable(
                    onClick = onLaunch,
                    onLongClick = {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
        )

        if (showLabel) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(52.dp)
            )
        }

        if (showMenu) {
            val density = LocalDensity.current
            val yOffset = with(density) { (iconSize + 8.dp).roundToPx() }
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -yOffset),
                onDismissRequest = { showMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(180.dp)
                ) {
                    Column {
                        AppMenuAction("Open") { onLaunch(); showMenu = false }
                        AppMenuAction("Unpin from Dock") { onUnpin(); showMenu = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppMenuAction(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.width(180.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(148.dp)
        )
    }
}
