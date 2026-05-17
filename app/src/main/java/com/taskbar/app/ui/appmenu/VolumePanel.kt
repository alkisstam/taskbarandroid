package com.taskbar.app.ui.appmenu

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
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
    var localValue by remember(stream.current) { mutableFloatStateOf(stream.current.toFloat()) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
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
            Slider(
                value = localValue,
                onValueChange = { v ->
                    localValue = v
                    onVolumeChange(v.toInt())
                },
                valueRange = 0f..stream.max.toFloat(),
                modifier = Modifier
                    .requiredWidth(160.dp)
                    .rotate(-90f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        Text(
            text = stream.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun buildVolumeStreams(audioManager: AudioManager, ringerMode: Int): List<VolumeStreamInfo> {
    val streams = mutableListOf<VolumeStreamInfo>()
    streams += VolumeStreamInfo(
        streamType = AudioManager.STREAM_MUSIC,
        label = "Media",
        icon = Icons.Filled.MusicNote,
        current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
        max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    )
    if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
        streams += VolumeStreamInfo(
            streamType = AudioManager.STREAM_RING,
            label = "Ring",
            icon = Icons.Filled.VolumeUp,
            current = audioManager.getStreamVolume(AudioManager.STREAM_RING),
            max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        )
    }
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
