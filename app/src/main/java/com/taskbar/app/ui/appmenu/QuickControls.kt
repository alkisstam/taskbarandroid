package com.taskbar.app.ui.appmenu

import android.media.AudioManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.DoNotDisturbOff
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenRotationAlt
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskbar.app.viewmodel.QuickControlItemData
import com.taskbar.app.viewmodel.QuickControlsState

@Composable
fun QuickControls(
    state: QuickControlsState,
    onAction: (String) -> Unit,
    order: List<String> = com.taskbar.app.data.PreferencesRepository.ALL_CONTROL_IDS,
    disabledIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val items = state.toItems(order, disabledIds)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            QuickControlTile(
                icon = item.icon,
                label = item.label,
                active = item.active,
                onClick = { onAction(item.id) }
            )
        }
    }
}

fun QuickControlsState.toItems(
    order: List<String> = com.taskbar.app.data.PreferencesRepository.ALL_CONTROL_IDS,
    disabledIds: Set<String> = emptySet()
): List<QuickControlItemData> {
    val ringerIcon = when (ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> Icons.AutoMirrored.Filled.VolumeUp
        AudioManager.RINGER_MODE_VIBRATE -> Icons.Filled.Vibration
        else -> Icons.AutoMirrored.Filled.VolumeOff
    }
    val ringerLabel = when (ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> "Ring"
        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
        else -> "Silent"
    }
    val all = mapOf(
        "torch" to QuickControlItemData(id = "torch", label = "Torch", active = torchOn,
            icon = if (torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff),
        "ringer" to QuickControlItemData(id = "ringer", label = ringerLabel,
            active = ringerMode == AudioManager.RINGER_MODE_NORMAL, icon = ringerIcon),
        "rotate" to QuickControlItemData(id = "rotate", label = "Rotate",
            active = canWriteSettings && autoRotate,
            icon = if (autoRotate) Icons.Filled.ScreenRotation else Icons.Filled.ScreenRotationAlt),
        "brightness" to QuickControlItemData(id = "brightness", label = "Auto-Bright",
            active = canWriteSettings && autoBrightness,
            icon = if (autoBrightness) Icons.Filled.BrightnessAuto else Icons.Filled.BrightnessHigh),
        "brightness_slider" to QuickControlItemData(id = "brightness_slider", label = "Bright",
            active = false, icon = Icons.Filled.BrightnessMedium),
        "dnd" to QuickControlItemData(id = "dnd", label = "DND", active = dndEnabled,
            icon = if (dndEnabled) Icons.Filled.DoNotDisturb else Icons.Filled.DoNotDisturbOff),
        "qr" to QuickControlItemData(id = "qr", label = "QR", active = false, icon = Icons.Filled.QrCodeScanner),
        "power" to QuickControlItemData(id = "power", label = "Power", active = false, icon = Icons.Filled.PowerSettingsNew),
        "volume" to QuickControlItemData(id = "volume", label = "Volume", active = false, icon = Icons.Filled.Tune)
    )
    val effectiveOrder = order.ifEmpty { com.taskbar.app.data.PreferencesRepository.ALL_CONTROL_IDS }
    return effectiveOrder
        .filter { id -> id !in disabledIds }
        .filter { id -> id != "power" || canShowPowerMenu }
        .mapNotNull { id -> all[id] }
}

@Composable
fun QuickControlItem(
    item: QuickControlItemData,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuickControlTile(
        icon = item.icon,
        label = item.label,
        active = item.active,
        onClick = onToggle,
        modifier = modifier
    )
}

@Composable
private fun QuickControlTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (active)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (active)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            color = containerColor,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                    tint = contentColor
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
