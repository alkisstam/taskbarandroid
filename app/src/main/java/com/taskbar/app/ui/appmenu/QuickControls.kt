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
import com.taskbar.app.viewmodel.QuickControlsState

@Composable
fun QuickControls(
    state: QuickControlsState,
    onToggleTorch: () -> Unit,
    onCycleRingerMode: () -> Unit,
    onToggleAutoRotate: () -> Unit,
    onToggleAutoBrightness: () -> Unit,
    onRequestWriteSettings: () -> Unit,
    onToggleDnd: () -> Unit,
    onRequestDndPermission: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onShowPowerMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (state.hasTorch) {
            QuickControlTile(
                icon = if (state.torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                label = "Torch",
                active = state.torchOn,
                onClick = onToggleTorch
            )
        }

        val ringerIcon = when (state.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> Icons.AutoMirrored.Filled.VolumeUp
            AudioManager.RINGER_MODE_VIBRATE -> Icons.Filled.Vibration
            else -> Icons.AutoMirrored.Filled.VolumeOff
        }
        val ringerLabel = when (state.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "Ring"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> "Silent"
        }
        QuickControlTile(
            icon = ringerIcon,
            label = ringerLabel,
            active = state.ringerMode == AudioManager.RINGER_MODE_NORMAL,
            onClick = onCycleRingerMode
        )

        if (!state.canWriteSettings) {
            QuickControlTile(
                icon = Icons.Filled.ScreenRotation,
                label = "Rotate",
                active = false,
                onClick = onRequestWriteSettings
            )
            QuickControlTile(
                icon = Icons.Filled.BrightnessAuto,
                label = "Bright",
                active = false,
                onClick = onRequestWriteSettings
            )
        } else {
            QuickControlTile(
                icon = if (state.autoRotate) Icons.Filled.ScreenRotation else Icons.Filled.ScreenRotationAlt,
                label = "Rotate",
                active = state.autoRotate,
                onClick = onToggleAutoRotate
            )
            QuickControlTile(
                icon = if (state.autoBrightness) Icons.Filled.BrightnessAuto else Icons.Filled.BrightnessHigh,
                label = "Bright",
                active = state.autoBrightness,
                onClick = onToggleAutoBrightness
            )
        }

        QuickControlTile(
            icon = if (state.dndEnabled) Icons.Filled.DoNotDisturb else Icons.Filled.DoNotDisturbOff,
            label = "DND",
            active = state.dndEnabled,
            onClick = if (state.dndPermissionGranted) onToggleDnd else onRequestDndPermission
        )

        QuickControlTile(
            icon = Icons.Filled.QrCodeScanner,
            label = "QR",
            active = false,
            onClick = onOpenQrScanner
        )

        if (state.canShowPowerMenu) {
            QuickControlTile(
                icon = Icons.Filled.PowerSettingsNew,
                label = "Power",
                active = false,
                onClick = onShowPowerMenu
            )
        }
    }
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
