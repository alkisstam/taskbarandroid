package com.taskbar.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taskbar.app.data.PillGesture
import com.taskbar.app.viewmodel.TaskbarViewModel

@Composable
fun PillSettingsScreen(
    viewModel: TaskbarViewModel,
    modifier: Modifier = Modifier
) {
    val pillSettings by viewModel.pillSettings.collectAsState()
    val taskbarSettings by viewModel.taskbarSettings.collectAsState()

    var gestureExpanded by remember { mutableStateOf(false) }
    var pillExpanded by remember { mutableStateOf(false) }
    var taskbarExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpandableSection(
            title = "Trigger Gesture",
            expanded = gestureExpanded,
            onToggle = { gestureExpanded = !gestureExpanded }
        ) {
            Text(
                text = "How to show the taskbar when it's hidden",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GestureOption(
                    label = "Swipe Up",
                    description = "Swipe upward on the pill",
                    icon = { Icon(Icons.Filled.SwipeUp, contentDescription = null) },
                    selected = pillSettings.gesture == PillGesture.SWIPE_UP,
                    onClick = { viewModel.savePillSettings(pillSettings.copy(gesture = PillGesture.SWIPE_UP)) }
                )
                GestureOption(
                    label = "Swipe Down",
                    description = "Swipe downward on the pill",
                    icon = { Icon(Icons.Filled.SwipeDown, contentDescription = null) },
                    selected = pillSettings.gesture == PillGesture.SWIPE_DOWN,
                    onClick = { viewModel.savePillSettings(pillSettings.copy(gesture = PillGesture.SWIPE_DOWN)) }
                )
                GestureOption(
                    label = "Swipe In",
                    description = "Swipe inward (right) on the pill",
                    icon = { Icon(Icons.Filled.SwipeRight, contentDescription = null) },
                    selected = pillSettings.gesture == PillGesture.SWIPE_IN,
                    onClick = { viewModel.savePillSettings(pillSettings.copy(gesture = PillGesture.SWIPE_IN)) }
                )
                GestureOption(
                    label = "Double Tap",
                    description = "Tap the pill twice quickly",
                    icon = { Icon(Icons.Filled.TouchApp, contentDescription = null) },
                    selected = pillSettings.gesture == PillGesture.DOUBLE_TAP,
                    onClick = { viewModel.savePillSettings(pillSettings.copy(gesture = PillGesture.DOUBLE_TAP)) }
                )
            }
        }

        ExpandableSection(
            title = "Pill Size & Appearance",
            expanded = pillExpanded,
            onToggle = { pillExpanded = !pillExpanded }
        ) {
            SettingsSlider(
                label = "Width",
                value = pillSettings.widthDp,
                valueRange = 2f..120f,
                unit = "dp",
                onValueChange = { viewModel.savePillSettings(pillSettings.copy(widthDp = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSlider(
                label = "Height",
                value = pillSettings.heightDp,
                valueRange = 2f..64f,
                unit = "dp",
                onValueChange = { viewModel.savePillSettings(pillSettings.copy(heightDp = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSlider(
                label = "Transparency",
                value = pillSettings.alpha,
                valueRange = 0f..1f,
                unit = "%",
                displayTransform = { "${(it * 100).toInt()}%" },
                onValueChange = { viewModel.savePillSettings(pillSettings.copy(alpha = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSlider(
                label = "Position (from bottom)",
                value = pillSettings.positionYDp,
                valueRange = 0f..400f,
                unit = "dp",
                onValueChange = { viewModel.savePillSettings(pillSettings.copy(positionYDp = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSlider(
                label = "Position (from left)",
                value = pillSettings.positionXDp,
                valueRange = 0f..400f,
                unit = "dp",
                onValueChange = { viewModel.savePillSettings(pillSettings.copy(positionXDp = it)) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier
                        .width(pillSettings.widthDp.coerceIn(2f, 120f).dp)
                        .height(pillSettings.heightDp.coerceIn(2f, 64f).dp),
                    shape = RoundedCornerShape(percent = 50),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = pillSettings.alpha)
                    )
                ) {}
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${pillSettings.widthDp.toInt()} × ${pillSettings.heightDp.toInt()} dp  •  ${(pillSettings.alpha * 100).toInt()}% opacity",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ExpandableSection(
            title = "Taskbar Size & Appearance",
            expanded = taskbarExpanded,
            onToggle = { taskbarExpanded = !taskbarExpanded }
        ) {
            SettingsSlider(
                label = "Position (from bottom)",
                value = taskbarSettings.positionYDp,
                valueRange = 0f..300f,
                unit = "dp",
                onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(positionYDp = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSlider(
                label = "Width",
                value = taskbarSettings.widthFraction,
                valueRange = 0.4f..1f,
                unit = "%",
                displayTransform = { "${(it * 100).toInt()}%" },
                onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(widthFraction = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSlider(
                label = "Height",
                value = taskbarSettings.heightDp,
                valueRange = 40f..120f,
                unit = "dp",
                onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(heightDp = it)) }
            )
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun GestureOption(
    label: String,
    description: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingIcon = icon,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    displayTransform: (Float) -> String = { "${it.toInt()} $unit" },
    onValueChange: (Float) -> Unit
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                displayTransform(localValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChange(localValue) },
            valueRange = valueRange
        )
    }
}

