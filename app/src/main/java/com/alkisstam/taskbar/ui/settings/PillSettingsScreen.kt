@file:OptIn(ExperimentalMaterial3Api::class)

package com.alkisstam.taskbar.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.AppMenuButtonSide
import com.alkisstam.taskbar.data.DARK_TINT_PRESETS
import com.alkisstam.taskbar.data.DockPadding
import com.alkisstam.taskbar.data.IconShape
import com.alkisstam.taskbar.data.LIGHT_TINT_PRESETS
import com.alkisstam.taskbar.data.GestureAction
import com.alkisstam.taskbar.data.PillHoldDragAction
import com.alkisstam.taskbar.data.PillEdgePosition
import com.alkisstam.taskbar.data.ThemeMode
import com.alkisstam.taskbar.ui.common.LocalHapticEnabled
import com.alkisstam.taskbar.ui.taskbar.isGestureNavigation
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel
import com.alkisstam.taskbar.R
import kotlin.math.roundToInt

@Composable
fun PillSettingsScreen(
    viewModel: TaskbarViewModel,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
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
            title = stringResource(R.string.pill_theme_section_title),
            expanded = themeExpanded,
            onToggle = { themeExpanded = !themeExpanded }
        ) {
            ThemeSectionContent(viewModel)
        }

        ExpandableSection(
            title = stringResource(R.string.pill_size_appearance_section_title),
            expanded = pillExpanded,
            onToggle = { pillExpanded = !pillExpanded }
        ) {
            PillSectionContent(viewModel)
        }

        ExpandableSection(
            title = stringResource(R.string.pill_dock_size_appearance_section_title),
            expanded = dockExpanded,
            onToggle = { dockExpanded = !dockExpanded }
        ) {
            DockSectionContent(viewModel, dockExpanded)
        }
    }
}

@Composable
private fun ThemeSectionContent(viewModel: TaskbarViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val surfaceTintColor by viewModel.surfaceTintColor.collectAsState()
    val panelOutlineEnabled by viewModel.panelOutlineEnabled.collectAsState()
    val translucentMode by viewModel.translucentMode.collectAsState()
    val translucentAlpha by viewModel.translucentAlpha.collectAsState()
    val grainAlpha by viewModel.grainAlpha.collectAsState()
    val blurRadiusDp by viewModel.blurRadiusDp.collectAsState()
    val blurTint by viewModel.blurTint.collectAsState()
    val blurTintAlpha by viewModel.blurTintAlpha.collectAsState()

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val themeEntries = listOf(
            Triple(ThemeMode.LIGHT, stringResource(R.string.pill_theme_light), Icons.Filled.LightMode),
            Triple(ThemeMode.DARK, stringResource(R.string.pill_theme_dark), Icons.Filled.DarkMode),
            Triple(ThemeMode.SYSTEM, stringResource(R.string.pill_theme_system), Icons.Filled.PhoneAndroid)
        )
        themeEntries.forEachIndexed { index, (mode, label, icon) ->
            SegmentedButton(
                selected = themeMode == mode,
                onClick = {
                    viewModel.setThemeMode(mode)
                    val incompatiblePresets = when (mode) {
                        ThemeMode.LIGHT -> DARK_TINT_PRESETS
                        ThemeMode.DARK -> LIGHT_TINT_PRESETS
                        ThemeMode.SYSTEM -> LIGHT_TINT_PRESETS + DARK_TINT_PRESETS
                    }
                    if (surfaceTintColor != 0L && incompatiblePresets.any { it.second == surfaceTintColor }) {
                        viewModel.setSurfaceTintColor(0L)
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = themeEntries.size),
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text(label) }
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    SurfaceTintColorPicker(
        currentColor = surfaceTintColor,
        themeMode = themeMode,
        onColorSelected = { viewModel.setSurfaceTintColor(it) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(stringResource(R.string.pill_theme_style_label), style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))
    val solidLabel = stringResource(R.string.pill_theme_style_solid)
    val transparentLabel = stringResource(R.string.pill_theme_style_transparent)
    GradientDropdownField(
        icon = Icons.Filled.Palette,
        selectedLabel = if (translucentMode) transparentLabel else solidLabel,
        options = listOf(solidLabel to false, transparentLabel to true),
        isSelected = { it == translucentMode },
        onSelect = { viewModel.setTranslucentMode(it) }
    )
    if (!translucentMode) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.pill_panel_outline_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = panelOutlineEnabled,
                onCheckedChange = { viewModel.setPanelOutlineEnabled(it) }
            )
        }
    } else {
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSlider(
            label = stringResource(R.string.pill_theme_transparency_label),
            value = translucentAlpha,
            valueRange = 0.3f..1f,
            unit = "%",
            step = 0.01f,
            displayTransform = { "${(it * 100).toInt()}%" },
            editValueTransform = { (it * 100).roundToInt().toString() },
            parseEditValue = { it.toFloatOrNull()?.div(100f) },
            onValueChange = { viewModel.setTranslucentAlpha(it) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        SettingsSlider(
            label = stringResource(R.string.pill_grain_label),
            value = grainAlpha,
            valueRange = 0f..0.3f,
            unit = "%",
            step = 0.01f,
            displayTransform = { "${(it * 100).toInt()}%" },
            editValueTransform = { (it * 100).roundToInt().toString() },
            parseEditValue = { it.toFloatOrNull()?.div(100f) },
            onValueChange = { viewModel.setGrainAlpha(it) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        SettingsSlider(
            label = stringResource(R.string.pill_blur_label),
            value = blurRadiusDp,
            valueRange = 0f..48f,
            unit = "dp",
            step = 1f,
            onValueChange = { viewModel.setBlurRadiusDp(it) }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.pill_blur_tint_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = blurTint,
                onCheckedChange = { viewModel.setBlurTint(it) }
            )
        }
        if (blurTint) {
            SettingsSlider(
                label = stringResource(R.string.pill_blur_tint_alpha_label),
                value = blurTintAlpha,
                valueRange = 0f..1f,
                unit = "%",
                step = 0.01f,
                displayTransform = { "${(it * 100).toInt()}%" },
                editValueTransform = { (it * 100).roundToInt().toString() },
                parseEditValue = { it.toFloatOrNull()?.div(100f) },
                onValueChange = { viewModel.setBlurTintAlpha(it) }
            )
        }
    }
}

@Composable
private fun PillSectionContent(viewModel: TaskbarViewModel) {
    val pillSettings by viewModel.pillSettings.collectAsState()
    val configuration = LocalConfiguration.current
    val widthMax = configuration.screenWidthDp.toFloat()
    val heightMax = (configuration.screenHeightDp / 2).toFloat()
    // Android honours at most 200dp of back-gesture exclusion per edge.
    val pillHeightMax = if (pillSettings.edgePosition == PillEdgePosition.BOTTOM) heightMax else minOf(heightMax, 200f)

    SettingsSlider(
        label = stringResource(R.string.pill_width_label),
        value = pillSettings.widthDp.coerceAtMost(widthMax),
        valueRange = 2f..widthMax,
        unit = "dp",
        step = 1f,
        onValueChange = { viewModel.savePillSettings(pillSettings.copy(widthDp = it)) }
    )
    Spacer(modifier = Modifier.height(10.dp))
    SettingsSlider(
        label = stringResource(R.string.pill_height_label),
        value = pillSettings.heightDp.coerceAtMost(pillHeightMax),
        valueRange = 2f..pillHeightMax,
        unit = "dp",
        step = 1f,
        onValueChange = { viewModel.savePillSettings(pillSettings.copy(heightDp = it)) }
    )
    Spacer(modifier = Modifier.height(10.dp))
    SettingsSlider(
        label = stringResource(R.string.pill_alpha_transparency_label),
        value = pillSettings.alpha,
        valueRange = 0f..1f,
        unit = "%",
        step = 0.01f,
        displayTransform = { "${(it * 100).toInt()}%" },
        editValueTransform = { (it * 100).roundToInt().toString() },
        parseEditValue = { it.toFloatOrNull()?.div(100f) },
        onValueChange = { viewModel.savePillSettings(pillSettings.copy(alpha = it)) }
    )
    Spacer(modifier = Modifier.height(10.dp))
    SettingsSlider(
        label = stringResource(R.string.pill_trigger_area_label),
        value = pillSettings.triggerAreaDp,
        valueRange = 8f..40f,
        unit = "dp",
        step = 1f,
        onValueChange = { viewModel.savePillSettings(pillSettings.copy(triggerAreaDp = it)) }
    )
    Text(
        stringResource(R.string.pill_trigger_area_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(10.dp))
    SettingsSlider(
        label = stringResource(R.string.pill_swipe_sensitivity_label),
        value = pillSettings.swipeThresholdDp,
        valueRange = 4f..40f,
        unit = "dp",
        step = 1f,
        onValueChange = { viewModel.savePillSettings(pillSettings.copy(swipeThresholdDp = it)) }
    )
    Text(
        stringResource(R.string.pill_swipe_sensitivity_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (pillSettings.edgePosition != PillEdgePosition.BOTTOM && pillSettings.swipeInAction == GestureAction.DISABLED) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.pill_restrict_trigger_title), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.pill_restrict_trigger_desc),
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
    }
    Text(stringResource(R.string.pill_position_label), style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))
    val edgeBottomLabel = stringResource(R.string.pill_edge_bottom)
    val edgeLeftLabel = stringResource(R.string.pill_edge_left)
    val edgeRightLabel = stringResource(R.string.pill_edge_right)
    val edgeBothLabel = stringResource(R.string.pill_edge_both)
    GradientDropdownField(
        icon = Icons.Filled.DragIndicator,
        selectedLabel = when (pillSettings.edgePosition) {
            PillEdgePosition.BOTTOM -> edgeBottomLabel
            PillEdgePosition.LEFT   -> edgeLeftLabel
            PillEdgePosition.RIGHT  -> edgeRightLabel
            PillEdgePosition.BOTH   -> edgeBothLabel
        },
        options = listOf(
            edgeBottomLabel to PillEdgePosition.BOTTOM,
            edgeLeftLabel to PillEdgePosition.LEFT,
            edgeRightLabel to PillEdgePosition.RIGHT,
            edgeBothLabel to PillEdgePosition.BOTH
        ),
        isSelected = { it == pillSettings.edgePosition },
        onSelect = { pos ->
            val (w, h) = if (pos == PillEdgePosition.BOTTOM) 140f to 12f else 4f to 90f
            viewModel.savePillSettings(pillSettings.copy(
                edgePosition = pos, widthDp = w, heightDp = h,
                positionYDp = if (pos == PillEdgePosition.BOTTOM) 2f else pillSettings.positionYDp
            ))
        }
    )
    if (pillSettings.edgePosition == PillEdgePosition.BOTTOM) {
        GestureActionDropdown(stringResource(R.string.pill_double_tap_action_label), Icons.Filled.TouchApp, pillSettings.doubleTapAction) {
            viewModel.savePillSettings(pillSettings.copy(doubleTapAction = it))
        }
        GestureActionDropdown(stringResource(R.string.pill_swipe_up_action_label), Icons.Filled.SwipeUp, pillSettings.swipeUpAction) {
            viewModel.savePillSettings(pillSettings.copy(swipeUpAction = it))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.pill_swipe_up_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSlider(
            label = stringResource(R.string.pill_position_from_bottom_label),
            value = pillSettings.positionYDp,
            valueRange = 0f..heightMax,
            unit = "dp",
            step = 1f,
            onValueChange = { viewModel.savePillSettings(pillSettings.copy(positionYDp = it)) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        SettingsSlider(
            label = stringResource(R.string.pill_position_along_edge_label),
            value = pillSettings.positionXPct,
            valueRange = 0f..100f,
            unit = "%",
            step = 1f,
            displayTransform = { "${it.toInt()}%" },
            onValueChange = { viewModel.savePillSettings(pillSettings.copy(positionXPct = it)) }
        )
    }
    if (pillSettings.edgePosition != PillEdgePosition.BOTTOM) {
        GestureActionDropdown(stringResource(R.string.pill_swipe_in_action_label), Icons.Filled.SwapHoriz, pillSettings.swipeInAction) {
            viewModel.savePillSettings(pillSettings.copy(swipeInAction = it))
        }
        GestureActionDropdown(stringResource(R.string.pill_swipe_up_action_label), Icons.Filled.SwipeUp, pillSettings.swipeUpAction) {
            viewModel.savePillSettings(pillSettings.copy(swipeUpAction = it))
        }
        GestureActionDropdown(stringResource(R.string.pill_swipe_down_action_label), Icons.Filled.Notifications, pillSettings.swipeDownAction) {
            viewModel.savePillSettings(pillSettings.copy(swipeDownAction = it))
        }
        GestureActionDropdown(stringResource(R.string.pill_double_tap_action_label), Icons.Filled.TouchApp, pillSettings.doubleTapAction) {
            viewModel.savePillSettings(pillSettings.copy(doubleTapAction = it))
        }
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSlider(
            label = stringResource(R.string.pill_position_along_edge_label),
            value = pillSettings.sidePositionPct,
            valueRange = 0f..100f,
            unit = "%",
            step = 1f,
            displayTransform = { "${it.toInt()}%" },
            onValueChange = { viewModel.savePillSettings(pillSettings.copy(sidePositionPct = it)) }
        )
    }
    // Hold-and-drag on a bottom pill collides with the system home/recents gesture.
    val context = LocalContext.current
    if (!(pillSettings.edgePosition == PillEdgePosition.BOTTOM && context.isGestureNavigation())) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.pill_hold_drag_label), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        val holdDragDisabledLabel = stringResource(R.string.pill_swipe_down_action_disabled)
        val holdDragBrightnessLabel = stringResource(R.string.pill_hold_drag_brightness)
        val holdDragVolumeLabel = stringResource(R.string.pill_hold_drag_volume)
        GradientDropdownField(
            icon = Icons.Filled.BrightnessMedium,
            selectedLabel = when (pillSettings.holdDragAction) {
                PillHoldDragAction.BRIGHTNESS -> holdDragBrightnessLabel
                PillHoldDragAction.VOLUME     -> holdDragVolumeLabel
                PillHoldDragAction.DISABLED   -> holdDragDisabledLabel
            },
            options = listOf(
                holdDragDisabledLabel to PillHoldDragAction.DISABLED,
                holdDragBrightnessLabel to PillHoldDragAction.BRIGHTNESS,
                holdDragVolumeLabel to PillHoldDragAction.VOLUME
            ),
            isSelected = { it == pillSettings.holdDragAction },
            onSelect = { action ->
                viewModel.savePillSettings(pillSettings.copy(holdDragAction = action))
            }
        )
        Text(
            text = stringResource(R.string.pill_hold_drag_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    PillPositionPreview(pillSettings.edgePosition, pillSettings.widthDp, pillSettings.heightDp, pillSettings.alpha, pillSettings.sidePositionPct, pillSettings.positionXPct, pillSettings.triggerOnWholeEdge)
    Spacer(modifier = Modifier.height(8.dp))
    if (pillSettings.edgePosition != PillEdgePosition.BOTTOM && pillSettings.swipeUpAction == GestureAction.SHOW_DOCK) {
        Text(
            text = stringResource(R.string.pill_swipe_up_activate_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DockSectionContent(viewModel: TaskbarViewModel, expanded: Boolean) {
    val taskbarSettings by viewModel.taskbarSettings.collectAsState()

    SettingsSlider(
        label = stringResource(R.string.dock_height_label),
        value = taskbarSettings.heightDp,
        valueRange = 40f..120f,
        unit = "dp",
        step = 1f,
        onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(heightDp = it)) }
    )
    Spacer(modifier = Modifier.height(10.dp))
    SettingsSlider(
        label = stringResource(R.string.dock_pinned_icon_size_label),
        value = taskbarSettings.pinnedIconSizeDp,
        valueRange = 32f..120f,
        unit = "dp",
        step = 1f,
        onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(pinnedIconSizeDp = it)) }
    )
    Spacer(modifier = Modifier.height(10.dp))
    SettingsSlider(
        label = stringResource(R.string.dock_pinned_icon_padding_label),
        value = taskbarSettings.pinnedIconPaddingDp,
        valueRange = 2f..12f,
        unit = "dp",
        step = 1f,
        onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(pinnedIconPaddingDp = it)) }
    )
    Spacer(modifier = Modifier.height(10.dp))
    SettingsSlider(
        label = stringResource(R.string.dock_quick_controls_size_label),
        value = taskbarSettings.quickControlSizeDp,
        valueRange = 32f..120f,
        unit = "dp",
        step = 1f,
        onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(quickControlSizeDp = it)) }
    )
    Spacer(modifier = Modifier.height(10.dp))
    SettingsSlider(
        label = stringResource(R.string.dock_corner_radius_label),
        value = taskbarSettings.cornerRadiusDp,
        valueRange = 0f..32f,
        unit = "dp",
        step = 1f,
        onValueChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(cornerRadiusDp = it)) }
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.dock_hide_info_line_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = taskbarSettings.hideInfoLine,
            onCheckedChange = { viewModel.saveTaskbarSettings(taskbarSettings.copy(hideInfoLine = it)) }
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(R.string.dock_edge_padding_label), style = MaterialTheme.typography.bodyMedium)
    Text(
        stringResource(R.string.dock_edge_padding_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    val dockPaddingDefaultLabel = stringResource(R.string.dock_padding_default)
    val dockPaddingSmallLabel = stringResource(R.string.dock_padding_small)
    val dockPaddingLargeLabel = stringResource(R.string.dock_padding_large)
    GradientDropdownField(
        icon = Icons.Filled.Straighten,
        selectedLabel = when (taskbarSettings.dockPadding) {
            DockPadding.DEFAULT -> dockPaddingDefaultLabel
            DockPadding.SMALL -> dockPaddingSmallLabel
            DockPadding.LARGE -> dockPaddingLargeLabel
        },
        options = listOf(
            dockPaddingDefaultLabel to DockPadding.DEFAULT,
            dockPaddingSmallLabel to DockPadding.SMALL,
            dockPaddingLargeLabel to DockPadding.LARGE
        ),
        isSelected = { it == taskbarSettings.dockPadding },
        onSelect = { viewModel.saveTaskbarSettings(taskbarSettings.copy(dockPadding = it)) }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(R.string.dock_all_apps_button_label), style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))
    val appMenuSideLeftLabel = stringResource(R.string.dock_app_menu_side_left)
    val appMenuSideRightLabel = stringResource(R.string.dock_app_menu_side_right)
    GradientDropdownField(
        icon = Icons.Filled.SwapHoriz,
        selectedLabel = when (taskbarSettings.appMenuButtonSide) {
            AppMenuButtonSide.LEFT -> appMenuSideLeftLabel
            AppMenuButtonSide.RIGHT -> appMenuSideRightLabel
        },
        options = listOf(
            appMenuSideLeftLabel to AppMenuButtonSide.LEFT,
            appMenuSideRightLabel to AppMenuButtonSide.RIGHT
        ),
        isSelected = { it == taskbarSettings.appMenuButtonSide },
        onSelect = { viewModel.saveTaskbarSettings(taskbarSettings.copy(appMenuButtonSide = it)) }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(R.string.dock_icon_pack_label), style = MaterialTheme.typography.bodyMedium)
    Text(
        stringResource(R.string.dock_icon_pack_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    val iconPackPackage by viewModel.iconPackPackage.collectAsState()
    val availableIconPacks by viewModel.availableIconPacks.collectAsState()
    LaunchedEffect(expanded) {
        if (expanded) viewModel.loadIconPacks()
    }
    val iconPackSystemDefaultLabel = stringResource(R.string.dock_icon_pack_system_default)
    GradientDropdownField(
        icon = Icons.Filled.Palette,
        selectedLabel = availableIconPacks.firstOrNull { it.packageName == iconPackPackage }?.label
            ?: iconPackSystemDefaultLabel,
        options = listOf(iconPackSystemDefaultLabel to "") +
            availableIconPacks.map { it.label to it.packageName },
        isSelected = { it == iconPackPackage },
        onSelect = { viewModel.setIconPack(it) }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(R.string.dock_icon_shape_label), style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))
    val iconShapeDefaultLabel = stringResource(R.string.pill_icon_shape_default)
    val iconShapeSquareLabel = stringResource(R.string.pill_icon_shape_square)
    val iconShapeSquircleLabel = stringResource(R.string.pill_icon_shape_squircle)
    val iconShapeCircleLabel = stringResource(R.string.pill_icon_shape_circle)
    GradientDropdownField(
        icon = Icons.Filled.CropSquare,
        selectedLabel = when (taskbarSettings.iconShape) {
            IconShape.DEFAULT -> iconShapeDefaultLabel
            IconShape.SQUARE -> iconShapeSquareLabel
            IconShape.SQUIRCLE -> iconShapeSquircleLabel
            IconShape.CIRCLE -> iconShapeCircleLabel
        },
        options = listOf(
            iconShapeDefaultLabel to IconShape.DEFAULT,
            iconShapeSquareLabel to IconShape.SQUARE,
            iconShapeSquircleLabel to IconShape.SQUIRCLE,
            iconShapeCircleLabel to IconShape.CIRCLE
        ),
        isSelected = { it == taskbarSettings.iconShape },
        onSelect = { viewModel.saveTaskbarSettings(taskbarSettings.copy(iconShape = it)) }
    )
}

@Composable
private fun GestureActionDropdown(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: GestureAction,
    onSelect: (GestureAction) -> Unit
) {
    val options = listOf(
        stringResource(R.string.pill_gesture_action_show_dock) to GestureAction.SHOW_DOCK,
        stringResource(R.string.pill_gesture_action_app_menu) to GestureAction.SHOW_APP_MENU,
        stringResource(R.string.pill_gesture_action_search) to GestureAction.SHOW_SEARCH,
        stringResource(R.string.pill_swipe_down_action_notifications) to GestureAction.SHOW_NOTIFICATIONS,
        stringResource(R.string.pill_swipe_down_action_quick_settings) to GestureAction.SHOW_QUICK_SETTINGS,
        stringResource(R.string.pill_swipe_down_action_power_menu) to GestureAction.POWER_MENU,
        stringResource(R.string.pill_swipe_down_action_screenshot) to GestureAction.TAKE_SCREENSHOT,
        stringResource(R.string.pill_swipe_down_action_flashlight) to GestureAction.TOGGLE_FLASHLIGHT,
        stringResource(R.string.pill_swipe_down_action_lock_screen) to GestureAction.LOCK_SCREEN,
        stringResource(R.string.pill_swipe_down_action_disabled) to GestureAction.DISABLED
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))
    GradientDropdownField(
        icon = icon,
        selectedLabel = options.first { it.second == selected }.first,
        options = options,
        isSelected = { it == selected },
        onSelect = onSelect
    )
}

@Composable
private fun PillPositionPreview(
    edgePosition: PillEdgePosition,
    widthDp: Float,
    heightDp: Float,
    alpha: Float,
    sidePositionPct: Float = 50f,
    positionXPct: Float = 50f,
    triggerOnWholeEdge: Boolean = false
) {
    val pillColor = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    val triggerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val frameColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val frameW = 60.dp
    val frameH = 100.dp
    val pillH = heightDp.coerceIn(2f, 50f).dp
    val sideOffsetFraction = (sidePositionPct - 50f) / 100f
    val bottomOffsetFraction = (positionXPct - 50f) / 100f

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
                            .width(28.dp)
                            .height(10.dp)
                            .background(triggerColor, RoundedCornerShape(8.dp))
                    )
                    // Pill indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .width(28.dp)
                            .height(4.dp)
                            .offset(x = (bottomOffsetFraction * frameW.value).dp, y = (-3).dp)
                            .background(pillColor, RoundedCornerShape(percent = 50))
                    )
                }
                else -> {
                    val sidePillH = pillH.coerceAtMost(frameH - 4.dp)
                    val sideOffset = (sideOffsetFraction * frameH.value).dp
                    val startSide = Alignment.CenterStart to RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    val endSide = Alignment.CenterEnd to RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    val sides = when (edgePosition) {
                        PillEdgePosition.LEFT -> listOf(startSide)
                        PillEdgePosition.RIGHT -> listOf(endSide)
                        else -> listOf(startSide, endSide)
                    }
                    sides.forEach { (alignment, triggerShape) ->
                        Box(
                            modifier = Modifier
                                .align(alignment)
                                .width(6.dp)
                                .height(if (triggerOnWholeEdge) frameH else sidePillH)
                                .offset(y = if (triggerOnWholeEdge) 0.dp else sideOffset)
                                .background(triggerColor, triggerShape)
                        )
                        // Pill indicator
                        Box(
                            modifier = Modifier
                                .align(alignment)
                                .width(4.dp)
                                .height(sidePillH)
                                .offset(y = sideOffset)
                                .background(pillColor, RoundedCornerShape(percent = 50))
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExpandableSection(
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
                    contentDescription = if (expanded) stringResource(R.string.pill_section_collapse_content_description) else stringResource(R.string.pill_section_expand_content_description),
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
fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    step: Float,
    sliderSteps: Int = 0,
    displayTransform: (Float) -> String = { "${it.toInt()} $unit" },
    editValueTransform: (Float) -> String = { it.toInt().toString() },
    parseEditValue: (String) -> Float? = { it.toFloatOrNull() },
    onValueChange: (Float) -> Unit
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var hasFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalHapticEnabled.current

    fun commit(newValue: Float) {
        val coerced = newValue.coerceIn(valueRange)
        localValue = coerced
        onValueChange(coerced)
    }

    fun commitEdit() {
        parseEditValue(editText)?.let { commit(it) }
        isEditing = false
        hasFocus = false
    }

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        commit(localValue - step)
                    },
                    enabled = localValue > valueRange.start,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = stringResource(R.string.pill_slider_decrease_content_description, label),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(if (localValue > valueRange.start) 1f else 0.38f)
                    )
                }
                Box(
                    modifier = Modifier.widthIn(min = 44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEditing) {
                        BasicTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { commitEdit() }),
                            modifier = Modifier
                                .width(48.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) hasFocus = true
                                    else if (hasFocus && isEditing) commitEdit()
                                }
                        )
                    } else {
                        Text(
                            displayTransform(localValue),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable {
                                editText = editValueTransform(localValue)
                                isEditing = true
                            }
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        commit(localValue + step)
                    },
                    enabled = localValue < valueRange.endInclusive,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.pill_slider_increase_content_description, label),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(if (localValue < valueRange.endInclusive) 1f else 0.38f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = localValue,
            onValueChange = { newValue ->
                val rangeSize = valueRange.endInclusive - valueRange.start
                val stepCount = 24
                val oldStep = ((localValue - valueRange.start) / rangeSize * stepCount).toInt()
                val newStep = ((newValue - valueRange.start) / rangeSize * stepCount).toInt()
                if (hapticEnabled && newStep != oldStep) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                localValue = newValue
            },
            onValueChangeFinished = { commit(localValue) },
            valueRange = valueRange,
            steps = sliderSteps,
            track = { FilledPillSliderTrack(it) },
            thumb = {}
        )
    }
}

@Composable
private fun SurfaceTintColorPicker(
    currentColor: Long,
    themeMode: ThemeMode,
    onColorSelected: (Long) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    var selectedTab by remember {
        mutableIntStateOf(
            if (currentColor != 0L && DARK_TINT_PRESETS.any { it.second == currentColor }) 1
            else if (currentColor == 0L && darkTheme) 1
            else 0
        )
    }
    val presets = if (selectedTab == 0) LIGHT_TINT_PRESETS else DARK_TINT_PRESETS
    val isCustomSelected = currentColor != 0L &&
        LIGHT_TINT_PRESETS.none { it.second == currentColor } &&
        DARK_TINT_PRESETS.none { it.second == currentColor }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.pill_surface_tint_color_label), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.pill_surface_tint_color_desc),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TintTab(stringResource(R.string.pill_tint_tab_light), selectedTab == 0) { selectedTab = 0 }
            TintTab(stringResource(R.string.pill_tint_tab_dark), selectedTab == 1) { selectedTab = 1 }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val cells: List<Pair<String, Long>?> = presets + null
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cells.chunked(3).forEach { rowCells ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowCells.forEach { cell ->
                        if (cell == null) {
                            TintCell(
                                label = stringResource(R.string.pill_tint_custom_label),
                                swatchColor = if (isCustomSelected) Color(currentColor)
                                              else MaterialTheme.colorScheme.surface,
                                isSelected = isCustomSelected,
                                showPlus = !isCustomSelected,
                                onClick = { showCustomPicker = true }
                            )
                        } else {
                            val (label, colorValue) = cell
                            TintCell(
                                label = label,
                                swatchColor = when {
                                    colorValue != 0L -> Color(colorValue)
                                    selectedTab == 0 -> Color.White
                                    else -> Color(0xFF121212)
                                },
                                isSelected = currentColor == colorValue,
                                onClick = { onColorSelected(colorValue) }
                            )
                        }
                    }
                }
            }
        }
        // Inline, not a Dialog: this screen also renders inside the overlay's
        // QuickSettingsPanel, where a Dialog has no window token (BadTokenException).
        if (showCustomPicker) {
            Spacer(modifier = Modifier.height(4.dp))
            CustomColorPicker(
                initialColor = if (isCustomSelected) currentColor else 0xFF808080L,
                onDismiss = { showCustomPicker = false },
                onConfirm = { color ->
                    onColorSelected(color)
                    showCustomPicker = false
                }
            )
        }
    }
}

@Composable
private fun TintTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(2.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape
                )
        )
    }
}

@Composable
private fun RowScope.TintCell(
    label: String,
    swatchColor: Color,
    isSelected: Boolean,
    showPlus: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(swatchColor, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (showPlus) {
                    Text(
                        "+",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun <T> GradientDropdownField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedLabel: String,
    options: List<Pair<String, T>>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .clickable { expanded = true }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Text(
                    selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = if (isSelected(value)) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomColorPicker(
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var base by remember { mutableStateOf(Color(initialColor)) }
    // 0 = lightest tint of base, 0.5 = base itself, 1 = black.
    var shade by remember { mutableFloatStateOf(0.5f) }
    val picked = shadeOf(base, shade)
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.pill_custom_color_dialog_title), style = MaterialTheme.typography.titleSmall)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gap = 3.dp
            val stripGap = 12.dp
            val gridWidth = (maxWidth - stripGap) * 0.68f
            val cell = (gridWidth - gap * 9) / 10
            val gridHeight = cell * MATERIAL_PALETTE.size + gap * (MATERIAL_PALETTE.size - 1)
            Row(horizontalArrangement = Arrangement.spacedBy(stripGap)) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    MATERIAL_PALETTE.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEach { value ->
                                val color = Color(value)
                                val selected = color == base
                                Box(
                                    modifier = Modifier
                                        .size(cell)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                        .border(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(3.dp)
                                        )
                                        .clickable {
                                            base = color
                                            shade = 0.5f
                                        }
                                )
                            }
                        }
                    }
                }
                val stripHeightPx = with(density) { gridHeight.toPx() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(gridHeight)
                        .background(Brush.verticalGradient(listOf(shadeOf(base, 0f), base, Color.Black)))
                        .pointerInput(stripHeightPx) {
                            detectTapGestures { shade = (it.y / stripHeightPx).coerceIn(0f, 1f) }
                        }
                        .pointerInput(stripHeightPx) {
                            detectVerticalDragGestures(
                                onDragStart = { shade = (it.y / stripHeightPx).coerceIn(0f, 1f) }
                            ) { change, _ ->
                                change.consume()
                                shade = (change.position.y / stripHeightPx).coerceIn(0f, 1f)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = gridHeight * shade - 10.dp)
                            .size(20.dp)
                            .border(2.dp, if (shade < 0.25f) Color.Black else Color.White, CircleShape)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.pill_custom_color_hex_label), style = MaterialTheme.typography.bodyMedium)
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 28.dp)
                    .background(picked, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            )
            Text(
                "#%06X".format(picked.toArgb() and 0xFFFFFF),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pill_custom_color_cancel_button)) }
            TextButton(onClick = { onConfirm(picked.toArgb().toLong() and 0xFFFFFFFFL) }) {
                Text(stringResource(R.string.pill_custom_color_apply_button))
            }
        }
    }
}

private fun shadeOf(base: Color, fraction: Float): Color =
    if (fraction <= 0.5f) lerp(lerp(base, Color.White, 0.7f), base, fraction / 0.5f)
    else lerp(base, Color.Black, (fraction - 0.5f) / 0.5f)

private val MATERIAL_PALETTE: List<List<Long>> = listOf(
    listOf(0xFFFFFFFF, 0xFFF5F5F5, 0xFFEEEEEE, 0xFFE0E0E0, 0xFFBDBDBD, 0xFF9E9E9E, 0xFF757575, 0xFF616161, 0xFF424242, 0xFF000000),
    listOf(0xFFFFEBEE, 0xFFFFCDD2, 0xFFEF9A9A, 0xFFE57373, 0xFFEF5350, 0xFFF44336, 0xFFE53935, 0xFFD32F2F, 0xFFC62828, 0xFFB71C1C),
    listOf(0xFFFCE4EC, 0xFFF8BBD0, 0xFFF48FB1, 0xFFF06292, 0xFFEC407A, 0xFFE91E63, 0xFFD81B60, 0xFFC2185B, 0xFFAD1457, 0xFF880E4F),
    listOf(0xFFF3E5F5, 0xFFE1BEE7, 0xFFCE93D8, 0xFFBA68C8, 0xFFAB47BC, 0xFF9C27B0, 0xFF8E24AA, 0xFF7B1FA2, 0xFF6A1B9A, 0xFF4A148C),
    listOf(0xFFEDE7F6, 0xFFD1C4E9, 0xFFB39DDB, 0xFF9575CD, 0xFF7E57C2, 0xFF673AB7, 0xFF5E35B1, 0xFF512DA8, 0xFF4527A0, 0xFF311B92),
    listOf(0xFFE8EAF6, 0xFFC5CAE9, 0xFF9FA8DA, 0xFF7986CB, 0xFF5C6BC0, 0xFF3F51B5, 0xFF3949AB, 0xFF303F9F, 0xFF283593, 0xFF1A237E),
    listOf(0xFFE3F2FD, 0xFFBBDEFB, 0xFF90CAF9, 0xFF64B5F6, 0xFF42A5F5, 0xFF2196F3, 0xFF1E88E5, 0xFF1976D2, 0xFF1565C0, 0xFF0D47A1),
    listOf(0xFFE1F5FE, 0xFFB3E5FC, 0xFF81D4FA, 0xFF4FC3F7, 0xFF29B6F6, 0xFF03A9F4, 0xFF039BE5, 0xFF0288D1, 0xFF0277BD, 0xFF01579B),
    listOf(0xFFE0F7FA, 0xFFB2EBF2, 0xFF80DEEA, 0xFF4DD0E1, 0xFF26C6DA, 0xFF00BCD4, 0xFF00ACC1, 0xFF0097A7, 0xFF00838F, 0xFF006064),
    listOf(0xFFE0F2F1, 0xFFB2DFDB, 0xFF80CBC4, 0xFF4DB6AC, 0xFF26A69A, 0xFF009688, 0xFF00897B, 0xFF00796B, 0xFF00695C, 0xFF004D40),
    listOf(0xFFE8F5E9, 0xFFC8E6C9, 0xFFA5D6A7, 0xFF81C784, 0xFF66BB6A, 0xFF4CAF50, 0xFF43A047, 0xFF388E3C, 0xFF2E7D32, 0xFF1B5E20),
    listOf(0xFFF1F8E9, 0xFFDCEDC8, 0xFFC5E1A5, 0xFFAED581, 0xFF9CCC65, 0xFF8BC34A, 0xFF7CB342, 0xFF689F38, 0xFF558B2F, 0xFF33691E),
    listOf(0xFFF9FBE7, 0xFFF0F4C3, 0xFFE6EE9C, 0xFFDCE775, 0xFFD4E157, 0xFFCDDC39, 0xFFC0CA33, 0xFFAFB42B, 0xFF9E9D24, 0xFF827717),
    listOf(0xFFFFFDE7, 0xFFFFF9C4, 0xFFFFF59D, 0xFFFFF176, 0xFFFFEE58, 0xFFFFEB3B, 0xFFFDD835, 0xFFFBC02D, 0xFFF9A825, 0xFFF57F17),
    listOf(0xFFFFF8E1, 0xFFFFECB3, 0xFFFFE082, 0xFFFFD54F, 0xFFFFCA28, 0xFFFFC107, 0xFFFFB300, 0xFFFFA000, 0xFFFF8F00, 0xFFFF6F00),
    listOf(0xFFFFF3E0, 0xFFFFE0B2, 0xFFFFCC80, 0xFFFFB74D, 0xFFFFA726, 0xFFFF9800, 0xFFFB8C00, 0xFFF57C00, 0xFFEF6C00, 0xFFE65100),
    listOf(0xFFFBE9E7, 0xFFFFCCBC, 0xFFFFAB91, 0xFFFF8A65, 0xFFFF7043, 0xFFFF5722, 0xFFF4511E, 0xFFE64A19, 0xFFD84315, 0xFFBF360C),
    listOf(0xFFEFEBE9, 0xFFD7CCC8, 0xFFBCAAA4, 0xFFA1887F, 0xFF8D6E63, 0xFF795548, 0xFF6D4C41, 0xFF5D4037, 0xFF4E342E, 0xFF3E2723),
    listOf(0xFFECEFF1, 0xFFCFD8DC, 0xFFB0BEC5, 0xFF90A4AE, 0xFF78909C, 0xFF607D8B, 0xFF546E7A, 0xFF455A64, 0xFF37474F, 0xFF263238)
)
