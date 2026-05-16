package com.taskbar.app.ui.taskbar

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.taskbar.app.data.AppInfo
import com.taskbar.app.ui.common.AppIconImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedAppItem(
    app: AppInfo,
    showLabel: Boolean = false,
    isDragging: Boolean = false,
    extraPopupBottomOffsetPx: Int = 0,
    dragModifier: Modifier = Modifier,
    onLaunch: () -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var itemHeightPx by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val shortcuts = remember(app.packageName) { getStaticShortcuts(context, app.packageName) }

    Column(
        modifier = modifier
            .then(dragModifier)
            .onSizeChanged { itemHeightPx = it.height },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .combinedClickable(
                    onClick = onLaunch,
                    onLongClick = { showMenu = true }
                ),
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

        if (showMenu) {
            val yOffsetPx = -(itemHeightPx + extraPopupBottomOffsetPx)
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, yOffsetPx),
                onDismissRequest = { showMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.widthIn(min = 160.dp, max = 220.dp)
                ) {
                    Column {
                        shortcuts.forEach { shortcut ->
                            TextButton(
                                onClick = {
                                    launchShortcut(context, shortcut)
                                    showMenu = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    shortcut.shortLabel?.toString() ?: shortcut.id,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        if (shortcuts.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(horizontal = 8.dp))
                        }
                        TextButton(
                            onClick = { onUnpin(); showMenu = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                "Unpin from TaskBar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getStaticShortcuts(context: Context, packageName: String): List<ShortcutInfo> {
    return try {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val query = LauncherApps.ShortcutQuery().apply {
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
            )
            setPackage(packageName)
        }
        launcherApps.getShortcuts(query, Process.myUserHandle())
            ?.take(4)
            ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun launchShortcut(context: Context, shortcut: ShortcutInfo) {
    try {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        launcherApps.startShortcut(shortcut, null, null)
    } catch (e: Exception) {
        // shortcut may no longer be available
    }
}
