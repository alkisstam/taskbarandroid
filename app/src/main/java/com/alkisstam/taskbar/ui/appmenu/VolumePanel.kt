package com.alkisstam.taskbar.ui.appmenu

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.ui.theme.TaskbarOutlineGreen

data class VolumeStreamInfo(
    val streamType: Int,
    val label: String,
    val icon: ImageVector,
    val current: Int,
    val max: Int
)

@Composable
fun VolumePanel(
    streams: List<VolumeStreamInfo>,
    onVolumeChange: (streamType: Int, value: Int) -> Unit,
    panelOutlineEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.then(if (panelOutlineEnabled) Modifier.border(2.dp, TaskbarOutlineGreen, RoundedCornerShape(20.dp)) else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            streams.forEach { stream ->
                VolumeSliderColumn(
                    stream = stream,
                    onVolumeChange = { onVolumeChange(stream.streamType, it) }
                )
            }
        }
    }
}

@Composable
private fun VolumeSliderColumn(
    stream: VolumeStreamInfo,
    onVolumeChange: (Int) -> Unit
) {
    var trackHeightPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var localCurrent by remember(stream.streamType) { mutableIntStateOf(stream.current) }
    // Only sync from ViewModel when not dragging; syncing mid-drag resets localCurrent
    // if the AudioManager hasn't flushed the write yet (returns the old value).
    SideEffect { if (!isDragging) localCurrent = stream.current }
    val fraction = if (stream.max > 0) localCurrent.toFloat() / stream.max else 0f
    var dragAccumulator by remember(stream.streamType) { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        if (stream.max <= 0) return@rememberDraggableState
        val pxPerStep = trackHeightPx / stream.max
        dragAccumulator -= delta
        val steps = (dragAccumulator / pxPerStep).toInt()
        if (steps != 0) {
            dragAccumulator -= steps * pxPerStep
            val newVal = (localCurrent + steps).coerceIn(0, stream.max)
            if (newVal != localCurrent) {
                localCurrent = newVal
                onVolumeChange(newVal)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = stream.icon,
                contentDescription = stream.label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { trackHeightPx = it.height.toFloat() }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = { isDragging = true; dragAccumulator = 0f },
                    onDragStopped = { isDragging = false }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(
            text = stream.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BrightnessPanel(
    brightnessLevel: Int,
    onBrightnessChange: (Int) -> Unit,
    autoBrightnessEnabled: Boolean,
    onAutoBrightnessToggle: () -> Unit,
    panelOutlineEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val maxBrightness = 255
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var localCurrent by remember { mutableIntStateOf(brightnessLevel) }
    SideEffect { if (!isDragging) localCurrent = brightnessLevel }
    val fraction = localCurrent.toFloat() / maxBrightness
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        val pxPerStep = trackWidthPx / maxBrightness
        dragAccumulator += delta
        val steps = (dragAccumulator / pxPerStep).toInt()
        if (steps != 0) {
            dragAccumulator -= steps * pxPerStep
            val newVal = (localCurrent + steps).coerceIn(1, maxBrightness)
            if (newVal != localCurrent) {
                localCurrent = newVal
                onBrightnessChange(newVal)
            }
        }
    }

    Surface(
        modifier = modifier.then(if (panelOutlineEnabled) Modifier.border(2.dp, TaskbarOutlineGreen, RoundedCornerShape(20.dp)) else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (autoBrightnessEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAutoBrightnessToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (autoBrightnessEnabled) Icons.Filled.BrightnessAuto else Icons.Filled.BrightnessHigh,
                    contentDescription = if (autoBrightnessEnabled) "Disable auto brightness" else "Enable auto brightness",
                    modifier = Modifier.size(18.dp),
                    tint = if (autoBrightnessEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .alpha(if (autoBrightnessEnabled) 0.4f else 1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .onSizeChanged { trackWidthPx = it.width.toFloat() }
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = { isDragging = true; dragAccumulator = 0f },
                        onDragStopped = { isDragging = false }
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

fun buildVolumeStreams(audioManager: AudioManager): List<VolumeStreamInfo> {
    val streams = mutableListOf<VolumeStreamInfo>()
    streams += VolumeStreamInfo(
        streamType = AudioManager.STREAM_MUSIC,
        label = "Media",
        icon = Icons.Filled.MusicNote,
        current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
        max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    )
    streams += VolumeStreamInfo(
        streamType = AudioManager.STREAM_RING,
        label = "Ring",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        current = audioManager.getStreamVolume(AudioManager.STREAM_RING),
        max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
    )
    streams += VolumeStreamInfo(
        streamType = AudioManager.STREAM_NOTIFICATION,
        label = "Notif",
        icon = Icons.Filled.Notifications,
        current = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION),
        max = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
    )
    streams += VolumeStreamInfo(
        streamType = AudioManager.STREAM_ALARM,
        label = "Alarm",
        icon = Icons.Filled.Alarm,
        current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM),
        max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    )
    return streams
}
