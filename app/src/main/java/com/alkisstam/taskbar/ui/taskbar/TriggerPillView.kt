package com.alkisstam.taskbar.ui.taskbar

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.GestureAction
import com.alkisstam.taskbar.data.PillEdgePosition
import com.alkisstam.taskbar.data.PillHoldDragAction
import com.alkisstam.taskbar.data.PillSettings
import com.alkisstam.taskbar.util.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun TriggerPillView(
    isCollapsed: Boolean,
    pillSettings: PillSettings,
    onAction: (GestureAction) -> Unit,
    dockRevealMaxDragPx: Float = 200f,
    onRevealProgress: (Float) -> Unit = {},
    onRevealCommit: () -> Unit = {},
    onRevealCancel: () -> Unit = {},
    onHoldDragStart: () -> Unit = {},
    onHoldDrag: (upwardPx: Float) -> Unit = {},
    onHoldDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isCollapsed,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val scope = rememberCoroutineScope()
        var tapJob by remember { mutableStateOf<Job?>(null) }
        var tapCount by remember { mutableIntStateOf(0) }
        var holdActive by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current

        val isBottom = pillSettings.edgePosition == PillEdgePosition.BOTTOM
        val context = LocalContext.current
        val holdDragEnabled = pillSettings.holdDragAction != PillHoldDragAction.DISABLED &&
            !(isBottom && context.isGestureNavigation())

        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isBottom && pillSettings.swipeInAction != GestureAction.DISABLED) Modifier.systemGestureExclusion() else Modifier)
                .pointerInput(pillSettings, dockRevealMaxDragPx) {
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var swipeFiredLocal = false
                    detectDragGestures(
                        onDragStart = {
                            holdActive = false
                            totalDragX = 0f
                            totalDragY = 0f
                            swipeFiredLocal = false
                        },
                        onDragEnd = {
                            if (holdActive) return@detectDragGestures
                            val upward = -totalDragY
                            when {
                                pillSettings.swipeUpAction == GestureAction.SHOW_DOCK && upward > 0 -> {
                                    val progress = (upward / dockRevealMaxDragPx).coerceIn(0f, 1f)
                                    if (progress >= 0.4f) onRevealCommit() else onRevealCancel()
                                }
                            }
                            totalDragX = 0f
                            totalDragY = 0f
                            swipeFiredLocal = false
                        },
                        onDragCancel = {
                            if (holdActive) return@detectDragGestures
                            if (pillSettings.swipeUpAction == GestureAction.SHOW_DOCK && -totalDragY > 0) {
                                onRevealCancel()
                            }
                            totalDragX = 0f
                            totalDragY = 0f
                            swipeFiredLocal = false
                        }
                    ) { _, drag ->
                        if (holdActive) return@detectDragGestures
                        totalDragX += drag.x
                        totalDragY += drag.y
                        val upward = -totalDragY
                        // Judge the whole swipe, not single events: high touch sample rates (Pixel)
                        // split a swipe into deltas too small to cross the threshold on their own.
                        val horizontal = abs(totalDragX) > abs(totalDragY)
                        val threshold = pillSettings.swipeThresholdDp.dp.toPx()
                        if (!swipeFiredLocal) {
                            when {
                                // Side pills only: either direction counts, since swiping outward from an edge isn't physically possible.
                                !isBottom && pillSettings.swipeInAction != GestureAction.DISABLED &&
                                    abs(totalDragX) > threshold && horizontal -> {
                                    swipeFiredLocal = true
                                    onAction(pillSettings.swipeInAction)
                                }
                                pillSettings.swipeUpAction == GestureAction.SHOW_DOCK && upward > 0 && !horizontal -> {
                                    onRevealProgress((upward / dockRevealMaxDragPx).coerceIn(0f, 1f))
                                }
                                pillSettings.swipeUpAction != GestureAction.SHOW_DOCK && pillSettings.swipeUpAction != GestureAction.DISABLED &&
                                    upward > threshold && !horizontal -> {
                                    swipeFiredLocal = true
                                    onAction(pillSettings.swipeUpAction)
                                }
                                !isBottom && pillSettings.swipeDownAction != GestureAction.DISABLED &&
                                    totalDragY > threshold && !horizontal -> {
                                    swipeFiredLocal = true
                                    onAction(pillSettings.swipeDownAction)
                                }
                            }
                        }
                    }
                }
                .pointerInput(pillSettings) {
                    detectTapGestures {
                        tapCount++
                        if (tapCount == 1) {
                            tapJob = scope.launch {
                                delay(Constants.DOUBLE_TAP_WINDOW_MS)
                                tapCount = 0
                            }
                        } else if (tapCount == 2) {
                            tapJob?.cancel()
                            tapCount = 0
                            onAction(pillSettings.doubleTapAction)
                        } else {
                            tapJob?.cancel()
                            tapCount = 1
                            tapJob = scope.launch {
                                delay(Constants.DOUBLE_TAP_WINDOW_MS)
                                tapCount = 0
                            }
                        }
                    }
                }
                .pointerInput(holdDragEnabled) {
                    if (!holdDragEnabled) return@pointerInput
                    var totalUp = 0f
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            holdActive = true
                            totalUp = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHoldDragStart()
                        },
                        onDragEnd = { holdActive = false; onHoldDragEnd() },
                        onDragCancel = { holdActive = false; onHoldDragEnd() }
                    ) { change, drag ->
                        change.consume()
                        totalUp -= drag.y
                        onHoldDrag(totalUp)
                    }
                }
        ) {
            if (pillSettings.triggerOnWholeEdge) {
                val availableH = maxHeight - pillSettings.heightDp.coerceAtLeast(2f).dp
                val yOffset = (pillSettings.sidePositionPct / 100f * availableH.value).dp
                    .coerceIn(0.dp, availableH.coerceAtLeast(0.dp))
                Box(modifier = Modifier.offset(y = yOffset).align(Alignment.TopCenter)) {
                    PillShape(pillSettings = pillSettings)
                }
            } else {
                PillShape(pillSettings = pillSettings)
            }
        }
    }
}

// navigation_mode is a hidden Settings.Secure key: 0 = 3-button, 1 = 2-button, 2 = gesture.
internal fun Context.isGestureNavigation() =
    Settings.Secure.getInt(contentResolver, "navigation_mode", 0) == 2

@Composable
private fun PillShape(
    pillSettings: PillSettings,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .width(pillSettings.widthDp.coerceAtLeast(2f).dp)
            .height(pillSettings.heightDp.coerceAtLeast(2f).dp),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = pillSettings.alpha),
        shadowElevation = if (pillSettings.alpha > 0.1f) 4.dp else 0.dp
    ) {
        Box { content() }
    }
}
