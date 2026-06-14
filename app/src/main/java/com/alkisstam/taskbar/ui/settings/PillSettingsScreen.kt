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
import androidx.compose.ui.text.style.TextAlign
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pill Size & Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                            val (w, h) = if (pos == PillEdgePosition.BOTTOM) 130f to 8f else 4f to 60f
                            val (swipeUp, swipeDown, doubleTap) = if (pos == PillEdgePosition.BOTTOM)
                                Triple(GestureAction.DISABLED, GestureAction.DISABLED, GestureAction.SHOW_DOCK)
                            else
                                Triple(GestureAction.SHOW_DOCK, GestureAction.DISABLED, GestureAction.DISABLED)
                            viewModel.savePillSettings(pillSettings.copy(
                                edgePosition = pos, widthDp = w, heightDp = h,
                                swipeUpAction = swipeUp, swipeDownAction = swipeDown, doubleTapAction = doubleTap
                            ))
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
            Spacer(modifier = Modifier.height(8.dp))
            if (pillSettings.edgePosition == PillEdgePosition.BOTTOM) {
                Text(
                    text = "Double Tap Home Button/Pill to Activate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Swipe Up in Trigger Area to Activate the Dock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Dock Size & Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSlider(
                    label = "Height",
                    value = taskbarSettings.heightDp,
                    valueRange = 40f..120f,
                    unit = "dp",
                    onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(heightDp = it)) }
                )
                SettingsSlider(
                    label = "Pinned App Icon Size",
                    value = taskbarSettings.pinnedIconSizeDp,
                    valueRange = 32f..50f,
                    unit = "dp",
                    onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(pinnedIconSizeDp = it)) }
                )
                SettingsSlider(
                    label = "Quick Controls Size",
                    value = taskbarSettings.quickControlSizeDp,
                    valueRange = 32f..50f,
                    unit = "dp",
                    onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(quickControlSizeDp = it)) }
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
    val triggerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val frameColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val frameW = 60.dp
    val frameH = 100.dp
    val pillH = heightDp.coerceIn(2f, 50f).dp
    val sideOffsetFraction = (sidePositionPct - 50f) / 100f

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
                    // Trigger area: full-width 18dp strip at bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(triggerColor, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    )
                    // Pill indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(28.dp)
                            .height(4.dp)
                            .offset(y = (-3).dp)
                            .background(pillColor, RoundedCornerShape(percent = 50))
                    )
                }
                PillEdgePosition.LEFT, PillEdgePosition.BOTH -> {
                    // Trigger area: 6dp strip on left
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(6.dp)
                            .height(frameH)
                            .background(triggerColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    )
                    // Pill indicator
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
                                .width(6.dp)
                                .height(frameH)
                                .background(triggerColor, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        )
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
                    // Trigger area: 6dp strip on right
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(6.dp)
                            .height(frameH)
                            .background(triggerColor, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    )
                    // Pill indicator
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


