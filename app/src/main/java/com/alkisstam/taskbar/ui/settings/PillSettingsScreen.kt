@file:OptIn(ExperimentalMaterial3Api::class)

package com.alkisstam.taskbar.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.GestureAction
import com.alkisstam.taskbar.data.PillEdgePosition
import com.alkisstam.taskbar.data.ThemeMode
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel

@Composable
fun PillSettingsScreen(
    viewModel: TaskbarViewModel,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val pillSettings by viewModel.pillSettings.collectAsState()
    val taskbarSettings by viewModel.taskbarSettings.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val surfaceTintColor by viewModel.surfaceTintColor.collectAsState()
    val panelOutlineEnabled by viewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by viewModel.translucentMode.collectAsState()
    val translucentAlpha by viewModel.translucentAlpha.collectAsState()
    val configuration = LocalConfiguration.current
    val widthMax = configuration.screenWidthDp.toFloat()
    val heightMax = (configuration.screenHeightDp / 2).toFloat()

    var pillExpanded by remember { mutableStateOf(false) }
    var dockExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExpandableSection(
            title = "Theme",
            expanded = themeExpanded,
            onToggle = { themeExpanded = !themeExpanded }
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    label = { Text("Light") },
                    leadingIcon = { Icon(Icons.Filled.LightMode, contentDescription = null) }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                    label = { Text("Dark") },
                    leadingIcon = { Icon(Icons.Filled.DarkMode, contentDescription = null) }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    label = { Text("System") },
                    leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SurfaceTintColorPicker(
                currentColor = surfaceTintColor,
                onColorSelected = { viewModel.setSurfaceTintColor(it) }
            )
            if (!translucentMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Panel Outline", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = panelOutlineEnabled,
                        onCheckedChange = { viewModel.setPanelOutlineEnabled(it) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Translucent panels", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Semi-transparent dock and panels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = translucentMode,
                    onCheckedChange = { viewModel.setTranslucentMode(it) }
                )
            }
            if (translucentMode) {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSlider(
                    label = "Transparency",
                    value = translucentAlpha,
                    valueRange = 0.3f..1f,
                    unit = "%",
                    displayTransform = { "${(it * 100).toInt()}%" },
                    onValueChange = { viewModel.setTranslucentAlpha(it) }
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
            SettingsSlider(
                label = "Trigger Area",
                value = pillSettings.triggerAreaDp,
                valueRange = 8f..40f,
                unit = "dp",
                onValueChange = { viewModel.savePillSettings(pillSettings.copy(triggerAreaDp = it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Restrict Trigger to Pill", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Only the pill area responds to gestures, not the whole edge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = pillSettings.restrictTriggerToPill,
                    onCheckedChange = { viewModel.savePillSettings(pillSettings.copy(restrictTriggerToPill = it)) }
                )
            }
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
                            val (w, h) = if (pos == PillEdgePosition.BOTTOM) 220f to 20f else 4f to 60f
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
            if (pillSettings.edgePosition == PillEdgePosition.BOTTOM) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select Pill Gesture", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = pillSettings.doubleTapAction == GestureAction.SHOW_DOCK,
                        onClick = { viewModel.savePillSettings(pillSettings.copy(
                            doubleTapAction = GestureAction.SHOW_DOCK,
                            swipeUpAction = GestureAction.DISABLED,
                            swipeDownAction = GestureAction.DISABLED
                        )) },
                        label = { Text("Double Tap", style = MaterialTheme.typography.labelMedium) }
                    )
                    FilterChip(
                        selected = pillSettings.swipeUpAction == GestureAction.SHOW_DOCK,
                        onClick = { viewModel.savePillSettings(pillSettings.copy(
                            doubleTapAction = GestureAction.DISABLED,
                            swipeUpAction = GestureAction.SHOW_DOCK,
                            swipeDownAction = GestureAction.DISABLED
                        )) },
                        label = { Text("Swipe Up", style = MaterialTheme.typography.labelMedium) }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Swipe Up works better on 3-button navigation devices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSlider(
                    label = "Position from bottom",
                    value = pillSettings.positionYDp,
                    valueRange = 0f..heightMax,
                    unit = "dp",
                    onValueChange = { viewModel.savePillSettings(pillSettings.copy(positionYDp = it)) }
                )
            }
            if (pillSettings.edgePosition != PillEdgePosition.BOTTOM) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notification Panel", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Swipe down for Notification Panel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = pillSettings.swipeDownAction == GestureAction.SHOW_NOTIFICATIONS,
                        onCheckedChange = { enabled ->
                            viewModel.savePillSettings(pillSettings.copy(
                                swipeDownAction = if (enabled) GestureAction.SHOW_NOTIFICATIONS else GestureAction.DISABLED
                            ))
                        }
                    )
                }
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
            PillPositionPreview(pillSettings.edgePosition, pillSettings.widthDp, pillSettings.heightDp, pillSettings.alpha, pillSettings.sidePositionPct, pillSettings.restrictTriggerToPill)
            Spacer(modifier = Modifier.height(8.dp))
            if (pillSettings.edgePosition != PillEdgePosition.BOTTOM) {
                Text(
                    text = "Swipe Up in Trigger Area to Activate the Dock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        ExpandableSection(
            title = "Dock Size & Appearance",
            expanded = dockExpanded,
            onToggle = { dockExpanded = !dockExpanded }
        ) {
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
                valueRange = 32f..60f,
                unit = "dp",
                onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(pinnedIconSizeDp = it)) }
            )
            SettingsSlider(
                label = "Quick Controls Size",
                value = taskbarSettings.quickControlSizeDp,
                valueRange = 32f..60f,
                unit = "dp",
                onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(quickControlSizeDp = it)) }
            )
        }
    }
}

@Composable
private fun PillPositionPreview(
    edgePosition: PillEdgePosition,
    widthDp: Float,
    heightDp: Float,
    alpha: Float,
    sidePositionPct: Float = 50f,
    restrictTriggerToPill: Boolean = false
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
                    // Trigger area: full-width 18dp strip at bottom, or pill-only when restricted
                    Box(
                        modifier = if (restrictTriggerToPill) {
                            Modifier
                                .align(Alignment.BottomCenter)
                                .width(28.dp)
                                .height(10.dp)
                                .background(triggerColor, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(triggerColor, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        }
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
                    val triggerH = if (restrictTriggerToPill) pillH.coerceAtMost(frameH - 4.dp) else frameH
                    // Trigger area: 6dp strip on left, or pill-only height when restricted
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(6.dp)
                            .height(triggerH)
                            .offset(y = if (restrictTriggerToPill) (sideOffsetFraction * frameH.value).dp else 0.dp)
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
                                .height(triggerH)
                                .offset(y = if (restrictTriggerToPill) (sideOffsetFraction * frameH.value).dp else 0.dp)
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
                    val triggerH = if (restrictTriggerToPill) pillH.coerceAtMost(frameH - 4.dp) else frameH
                    // Trigger area: 6dp strip on right, or pill-only height when restricted
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(6.dp)
                            .height(triggerH)
                            .offset(y = if (restrictTriggerToPill) (sideOffsetFraction * frameH.value).dp else 0.dp)
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
fun FilledPillSliderTrack(sliderState: SliderState) {
    val range = sliderState.valueRange
    val fraction = ((sliderState.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(MaterialTheme.colorScheme.primary)
        )
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
            valueRange = valueRange,
            track = { FilledPillSliderTrack(it) },
            thumb = {}
        )
    }
}

private val SURFACE_TINT_PRESETS: List<Pair<String, Long>> = listOf(
    "Default" to 0L,
    "Black" to 0xFF1A1A2E,
    "Navy" to 0xFF16213E,
    "Deep Purple" to 0xFF2D1B69,
    "Slate" to 0xFF2D3748,
    "Charcoal" to 0xFF36454F,
    "Cream" to 0xFFFFF8E7,
    "Sand" to 0xFFFFF3E0,
    "Blush" to 0xFFFFE4E1,
    "Sky" to 0xFFE3F2FD,
    "Mint" to 0xFFE8F5E9
)

@Composable
private fun SurfaceTintColorPicker(
    currentColor: Long,
    onColorSelected: (Long) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Surface Tint Color", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Applies to the dock and app menu",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            items(SURFACE_TINT_PRESETS) { (label, colorValue) ->
                val isSelected = currentColor == colorValue
                val displayColor = if (colorValue == 0L)
                    MaterialTheme.colorScheme.surface
                else
                    Color(colorValue)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(displayColor, CircleShape)
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable { onColorSelected(colorValue) }
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                val isCustomSelected = SURFACE_TINT_PRESETS.none { it.second == currentColor }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isCustomSelected) Color(currentColor) else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .then(
                                if (isCustomSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable { showCustomPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isCustomSelected) {
                            Text("+", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        "Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCustomSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = if (SURFACE_TINT_PRESETS.none { it.second == currentColor } && currentColor != 0L)
                currentColor else 0xFF808080L,
            onDismiss = { showCustomPicker = false },
            onConfirm = { color ->
                onColorSelected(color)
                showCustomPicker = false
            }
        )
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var r by remember { mutableFloatStateOf(((initialColor shr 16) and 0xFF).toFloat()) }
    var g by remember { mutableFloatStateOf(((initialColor shr 8) and 0xFF).toFloat()) }
    var b by remember { mutableFloatStateOf((initialColor and 0xFF).toFloat()) }
    val previewColor = Color(
        red = r / 255f,
        green = g / 255f,
        blue = b / 255f
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(previewColor, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("R: ${r.toInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = r,
                        onValueChange = { r = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth(),
                        track = { FilledPillSliderTrack(it) },
                        thumb = {}
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("G: ${g.toInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = g,
                        onValueChange = { g = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth(),
                        track = { FilledPillSliderTrack(it) },
                        thumb = {}
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("B: ${b.toInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = b,
                        onValueChange = { b = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth(),
                        track = { FilledPillSliderTrack(it) },
                        thumb = {}
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val color = (0xFF000000L) or
                    (r.toLong().coerceIn(0, 255) shl 16) or
                    (g.toLong().coerceIn(0, 255) shl 8) or
                    b.toLong().coerceIn(0, 255)
                onConfirm(color)
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
