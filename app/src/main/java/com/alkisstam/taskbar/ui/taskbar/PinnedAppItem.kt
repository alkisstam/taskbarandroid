package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.ui.common.AppIconImage

@Composable
fun PinnedAppItem(
    app: AppInfo,
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
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onLaunch),
            shape = RoundedCornerShape(14.dp),
            color = if (isDragging)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp,
            shadowElevation = if (isDragging) 8.dp else 0.dp
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
