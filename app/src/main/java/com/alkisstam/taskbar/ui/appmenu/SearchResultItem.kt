package com.alkisstam.taskbar.ui.appmenu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.R
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.IconShape
import com.alkisstam.taskbar.ui.common.AppIconImage
import com.alkisstam.taskbar.ui.common.LocalHapticEnabled
import com.alkisstam.taskbar.ui.common.toComposeShape
import com.alkisstam.taskbar.ui.theme.GlassBackdrop
import com.alkisstam.taskbar.ui.theme.LocalGlassSurface
import androidx.compose.foundation.layout.Column
import com.alkisstam.taskbar.ui.theme.GlassBlur
import androidx.compose.material3.MenuDefaults
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(
    app: AppInfo,
    isPinned: Boolean,
    isHighlighted: Boolean = false,
    iconShape: IconShape = IconShape.DEFAULT,
    onLaunch: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticEnabled.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isHighlighted) Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                else Modifier
            )
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconImage(
            icon = app.icon,
            contentDescription = app.label,
            shape = iconShape.toComposeShape(),
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        val glassMenu = GlassBlur.activeFor(LocalGlassSurface.current)
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = if (glassMenu) Color.Transparent else MenuDefaults.containerColor,
            tonalElevation = if (glassMenu) 0.dp else MenuDefaults.TonalElevation,
            shadowElevation = if (glassMenu) 0.dp else MenuDefaults.ShadowElevation
        ) {
            // The menu is its own popup window; frost it like the panel it opens from. Its Surface
            // goes fully transparent on glass: the blur can't reach the 8dp padding it adds.
            GlassBackdrop(
                enabled = LocalGlassSurface.current,
                cornerRadius = 12.dp,
                tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
            ) {
                Column {
                    DropdownMenuItem(
                        text = { Text(if (isPinned) stringResource(R.string.app_action_unpin_from_dock) else stringResource(R.string.app_action_pin_to_dock)) },
                        onClick = { onPin(); showMenu = false }
                    )
                }
            }
        }
    }
}
