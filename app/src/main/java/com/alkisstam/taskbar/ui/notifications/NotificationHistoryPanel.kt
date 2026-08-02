package com.alkisstam.taskbar.ui.notifications

import android.app.Notification
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.data.IconShape
import com.alkisstam.taskbar.data.NotificationEntry
import com.alkisstam.taskbar.ui.common.AppIconImage
import com.alkisstam.taskbar.ui.common.toComposeShape
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen
import com.alkisstam.taskbar.ui.theme.grain
import com.alkisstam.taskbar.viewmodel.NotificationHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryPanel(
    viewModel: NotificationHistoryViewModel,
    iconForPackage: (String) -> Bitmap?,
    onDismiss: () -> Unit,
    onOpenApp: (String) -> Unit,
    panelOutlineEnabled: Boolean = false,
    translucentMode: Boolean = false,
    translucentAlpha: Float = 0.80f,
    grainAlpha: Float = 0.10f,
    surfaceTintColor: Long = 0L,
    dockBottomPadding: Dp = 0.dp,
    iconShape: IconShape = IconShape.DEFAULT
) {
    val notifications by viewModel.notifications.collectAsState()
    val grouped = remember(notifications) { notifications.groupBy { it.packageName } }
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    val panelShape = RoundedCornerShape(24.dp)
    val panelColor = if (surfaceTintColor != 0L) Color(surfaceTintColor) else MaterialTheme.colorScheme.surface
    val glassBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

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
                .then(if (translucentMode && !panelOutlineEnabled) Modifier.border(1.dp, glassBorderColor, panelShape) else Modifier)
                .clip(panelShape)
                .grain(enabled = translucentMode && grainAlpha > 0f, alpha = grainAlpha),
            shape = panelShape,
            color = if (translucentMode) panelColor.copy(alpha = translucentAlpha) else panelColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = if (translucentMode || surfaceTintColor != 0L) 0.dp else 2.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.notification_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAll() }) {
                            Text(stringResource(R.string.notification_history_clear_all))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.notification_history_close))
                    }
                }

                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.notification_history_empty_state),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        grouped.forEach { (packageName, entries) ->
                            if (entries.size == 1) {
                                val entry = entries[0]
                                item(key = entry.id) {
                                    SwipeableNotificationCard(
                                        onClick = { onOpenApp(packageName) },
                                        onDelete = { viewModel.remove(entry.id) }
                                    ) {
                                        NotificationEntryContent(
                                            entry = entry,
                                            icon = iconForPackage(packageName),
                                            showAppRow = true,
                                            iconShape = iconShape,
                                            actions = remember(entry.id) { viewModel.actionsFor(entry.id) },
                                            onAction = { viewModel.sendAction(it) }
                                        )
                                    }
                                }
                            } else {
                                val expanded = expandedMap[packageName] == true
                                item(key = "group_$packageName") {
                                    NotificationGroupHeader(
                                        appLabel = entries[0].appLabel,
                                        icon = iconForPackage(packageName),
                                        count = entries.size,
                                        expanded = expanded,
                                        iconShape = iconShape,
                                        onToggle = { expandedMap[packageName] = !expanded }
                                    )
                                }
                                if (expanded) {
                                    items(entries, key = { it.id }) { entry ->
                                        SwipeableNotificationCard(
                                            onClick = { onOpenApp(packageName) },
                                            onDelete = { viewModel.remove(entry.id) }
                                        ) {
                                            NotificationEntryContent(
                                                entry = entry,
                                                icon = null,
                                                showAppRow = false,
                                                iconShape = iconShape,
                                                actions = remember(entry.id) { viewModel.actionsFor(entry.id) },
                                                onAction = { viewModel.sendAction(it) }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationCard(
    onClick: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            true
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (direction != null) MaterialTheme.colorScheme.errorContainer else Color.Transparent
                    )
            )
        }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
        ) {
            content()
        }
    }
}

@Composable
private fun NotificationGroupHeader(
    appLabel: String,
    icon: Bitmap?,
    count: Int,
    expanded: Boolean,
    iconShape: IconShape = IconShape.DEFAULT,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                icon = icon,
                contentDescription = appLabel,
                shape = iconShape.toComposeShape(),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.notification_history_group_count, count),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) stringResource(R.string.notification_history_collapse) else stringResource(R.string.notification_history_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationEntryContent(
    entry: NotificationEntry,
    icon: Bitmap?,
    showAppRow: Boolean,
    iconShape: IconShape = IconShape.DEFAULT,
    actions: List<Notification.Action> = emptyList(),
    onAction: (Notification.Action) -> Unit = {}
) {
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAppRow) {
            AppIconImage(
                icon = icon,
                contentDescription = entry.appLabel,
                shape = iconShape.toComposeShape(),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showAppRow) {
                    Text(
                        text = entry.appLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.title.isNotBlank()) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.text.isNotBlank()) {
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (actions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    actions.forEach { action ->
                        TextButton(onClick = { onAction(action) }) {
                            Text(
                                text = action.title.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> stringResource(R.string.notification_history_time_just_now)
        minutes < 60 -> stringResource(R.string.notification_history_time_minutes_ago, minutes)
        hours < 24 -> stringResource(R.string.notification_history_time_hours_ago, hours)
        else -> stringResource(R.string.notification_history_time_days_ago, days)
    }
}
