package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.ui.common.AppIconImage

@Composable
fun PinnedAppItem(
    app: AppInfo,
    iconSize: Dp = 48.dp,
    showLabel: Boolean = false,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.then(dragModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIconImage(
            icon = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .clickable(onClick = onLaunch)
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
    }
}
