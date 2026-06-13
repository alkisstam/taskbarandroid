package com.alkisstam.taskbar.ui.taskbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.GestureAction
import com.alkisstam.taskbar.data.PillEdgePosition
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

        val isBottom = pillSettings.edgePosition == PillEdgePosition.BOTTOM

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pillSettings, dockRevealMaxDragPx) {
                    var totalDragY = 0f
                    var swipeFiredLocal = false
                    detectDragGestures(
                        onDragStart = {
                            totalDragY = 0f
                            swipeFiredLocal = false
                        },
                        onDragEnd = {
                            val upward = -totalDragY
                            when {
                                pillSettings.swipeUpAction == GestureAction.SHOW_DOCK && upward > 0 -> {
                                    val progress = (upward / dockRevealMaxDragPx).coerceIn(0f, 1f)
                                    if (progress >= 0.4f) onRevealCommit() else onRevealCancel()
                                }
                            }
                            totalDragY = 0f
                            swipeFiredLocal = false
                        },
                        onDragCancel = {
                            if (pillSettings.swipeUpAction == GestureAction.SHOW_DOCK && -totalDragY > 0) {
                                onRevealCancel()
                            }
                            totalDragY = 0f
                            swipeFiredLocal = false
                        }
                    ) { _, drag ->
                        totalDragY += drag.y
                        val upward = -totalDragY
                        if (!swipeFiredLocal) {
                            when {
                                pillSettings.swipeUpAction == GestureAction.SHOW_DOCK && upward > 0 && abs(drag.y) > abs(drag.x) -> {
                                    onRevealProgress((upward / dockRevealMaxDragPx).coerceIn(0f, 1f))
                                }
                                pillSettings.swipeUpAction != GestureAction.SHOW_DOCK &&
                                    drag.y < -Constants.SWIPE_TRIGGER_THRESHOLD_PX && abs(drag.y) > abs(drag.x) -> {
                                    swipeFiredLocal = true
                                    onAction(pillSettings.swipeUpAction)
                                }
                                drag.y > Constants.SWIPE_TRIGGER_THRESHOLD_PX && abs(drag.y) > abs(drag.x) -> {
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
        ) {
            if (isBottom) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    PillShape(pillSettings = pillSettings)
                }
            } else {
                val totalH = maxHeight
                val pillH = pillSettings.heightDp.coerceAtLeast(2f).dp
                val availableH = totalH - pillH
                val yOffset = (pillSettings.sidePositionPct / 100f * availableH.value).dp
                    .coerceIn(0.dp, availableH.coerceAtLeast(0.dp))
                Box(modifier = Modifier.offset(y = yOffset).align(Alignment.TopCenter)) {
                    PillShape(pillSettings = pillSettings)
                }
            }
        }
    }
}

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
