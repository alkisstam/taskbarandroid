package com.alkisstam.taskbar.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.GestureAction
import com.alkisstam.taskbar.data.PillEdgePosition
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel

@Composable
fun PillSettingsScreen(
    viewModel: TaskbarViewModel,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val pillSettings by viewModel.pillSettings.collectAsState()
    val taskbarSettings by viewModel.taskbarSettings.collectAsState()
    val configuration = LocalConfiguration.current
    val widthMax = configuration.screenWidthDp.toFloat()
    val heightMax = (configuration.screenHeightDp / 2).toFloat()

    var gestureExpanded by remember { mutableStateOf(false) }
    var pillExpanded by remember { mutableStateOf(false) }
    var taskbarExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpandableSection(
            title = "Trigger Gesture",
            expanded = gestureExpanded,
            onToggle = { gestureExpanded = !gestureExpanded }
        ) {
            Text(
                text = "Configure what each gesture does",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GestureActionRow(
                    label = "Swipe Up",
                    icon = { Icon(Icons.Filled.SwipeUp, contentDescription = null) },
                    selected = pillSettings.swipeUpAction,
                    onSelect = { viewModel.savePillSettings(pillSettings.copy(swipeUpAction = it)) }
                )
                GestureActionRow(
                    label = "Swipe Down",
                    icon = { Icon(Icons.Filled.SwipeDown, contentDescription = null) },
                    selected = pillSettings.swipeDownAction,
                    onSelect = { viewModel.savePillSettings(pillSettings.copy(swipeDownAction = it)) }
                )
                GestureActionRow(
                    label = "Double Tap",
                    icon = { Icon(Icons.Filled.TouchApp, contentDescription = null) },
                    selected = pillSettings.doubleTapAction,
                    onSelect = { viewModel.savePillSettings(pillSettings.copy(doubleTapAction = it)) }
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
                value = pillSettings.widthDp.coerceAtMost(widthMax),
                valueRange = 2f..widthMax,
                unit = "dp",
                onValueChange = { viewModel.savePillSettings(pillSettings.copy(widthDp = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSlider(
                label = "Height",
                value = pillSettings.heightDp.coerceAtMost(heightMax),
                valueRange = 2f..heightMax,
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
            Text("Position", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PillEdgePosition.entries.forEach { pos ->
                    FilterChip(
                        selected = pillSettings.edgePosition == pos,
                        onClick = {
                            val (w, h) = if (pos == PillEdgePosition.BOTTOM) 180f to 20f else 12f to 120f
                            viewModel.savePillSettings(pillSettings.copy(edgePosition = pos, widthDp = w, heightDp = h))
                        },
                        label = {
                            Text(
                                when (pos) {
                                    PillEdgePosition.BOTTOM -> "Bottom"
                                    PillEdgePosition.LEFT   -> "Left"
                                    PillEdgePosition.RIGHT  -> "Right"
                                    PillEdgePosition.BOTH   -> "Both"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
            if (pillSettings.edgePosition != PillEdgePosition.BOTTOM) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSlider(
                    label = "Position along edge",
                    value = pillSettings.sidePositionPct,
                    valueRange = 0f..100f,
                    unit = "%",
                    displayTransform = { "${it.toInt()}%" },
                    onValueChange = { viewModel.savePillSettings(pillSettings.copy(sidePositionPct = it)) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            PillPositionPreview(pillSettings.edgePosition, pillSettings.widthDp, pillSettings.heightDp, pillSettings.alpha, pillSettings.sidePositionPct)
        }

        ExpandableSection(
            title = "Dock Size & Appearance",
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
                label = "Height",
                value = taskbarSettings.heightDp,
                valueRange = 40f..120f,
                unit = "dp",
                onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(heightDp = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Show App Labels", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Show app name below each icon in the dock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.material3.Switch(
                    checked = taskbarSettings.showLabels,
                    onCheckedChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(showLabels = it)) }
                )
            }
        }
    }
}

@Composable
private fun PillPositionPreview(
    edgePosition: PillEdgePosition,
    widthDp: Float,
    heightDp: Float,
    alpha: Float,
    sidePositionPct: Float = 50f
) {
    val pillColor = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    val frameColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val frameW = 60.dp
    val frameH = 100.dp
    val pillW = widthDp.coerceIn(2f, 30f).dp
    val pillH = heightDp.coerceIn(2f, 50f).dp
    // y offset of the pill centre inside the frame (clamped so the pill stays within bounds)
    val sideOffsetFraction = (sidePositionPct - 50f) / 100f  // -0.5 .. +0.5

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(frameH + 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(frameW, frameH)
                .background(frameColor, RoundedCornerShape(8.dp))
        ) {
            when (edgePosition) {
                PillEdgePosition.BOTTOM -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(pillW.coerceAtMost(frameW - 4.dp))
                            .height(4.dp)
                            .background(pillColor, RoundedCornerShape(percent = 50))
                    )
                }
                PillEdgePosition.LEFT, PillEdgePosition.BOTH -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(4.dp)
                            .height(pillH.coerceAtMost(frameH - 4.dp))
                            .offset(y = (sideOffsetFraction * frameH.value).dp)
                            .background(pillColor, RoundedCornerShape(percent = 50))
                    )
                    if (edgePosition == PillEdgePosition.BOTH) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(4.dp)
                                .height(pillH.coerceAtMost(frameH - 4.dp))
                                .offset(y = (sideOffsetFraction * frameH.value).dp)
                                .background(pillColor, RoundedCornerShape(percent = 50))
                        )
                    }
                }
                PillEdgePosition.RIGHT -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(4.dp)
                            .height(pillH.coerceAtMost(frameH - 4.dp))
                            .offset(y = (sideOffsetFraction * frameH.value).dp)
                            .background(pillColor, RoundedCornerShape(percent = 50))
                    )
                }
            }
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
private fun GestureActionRow(
    label: String,
    icon: @Composable () -> Unit,
    selected: GestureAction,
    onSelect: (GestureAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = selected == GestureAction.SHOW_DOCK,
                onClick = { onSelect(GestureAction.SHOW_DOCK) },
                label = { Text("Show Dock", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(Icons.Filled.ViewDay, contentDescription = null) }
            )
            FilterChip(
                selected = selected == GestureAction.SHOW_NOTIFICATIONS,
                onClick = { onSelect(GestureAction.SHOW_NOTIFICATIONS) },
                label = { Text("Notifications", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(Icons.Filled.Notifications, contentDescription = null) }
            )
            FilterChip(
                selected = selected == GestureAction.SHOW_QUICK_SETTINGS,
                onClick = { onSelect(GestureAction.SHOW_QUICK_SETTINGS) },
                label = { Text("Quick Settings", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) }
            )
            FilterChip(
                selected = selected == GestureAction.DISABLED,
                onClick = { onSelect(GestureAction.DISABLED) },
                label = { Text("Disable", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) }
            )
        }
    }
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


