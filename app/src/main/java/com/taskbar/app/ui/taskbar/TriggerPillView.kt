package com.taskbar.app.ui.taskbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.taskbar.app.data.PillGesture
import com.taskbar.app.data.PillSettings
import com.taskbar.app.util.Constants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun TriggerPillView(
    isCollapsed: Boolean,
    pillSettings: PillSettings,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isCollapsed,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        when (pillSettings.gesture) {
            PillGesture.DOUBLE_TAP -> DoubleTapPill(pillSettings, onExpand)
            PillGesture.SWIPE_UP   -> SwipePill(pillSettings, onExpand, direction = SwipeDir.UP)
            PillGesture.SWIPE_DOWN -> SwipePill(pillSettings, onExpand, direction = SwipeDir.DOWN)
            PillGesture.SWIPE_IN   -> SwipePill(pillSettings, onExpand, direction = SwipeDir.IN)
        }
    }
}

private enum class SwipeDir { UP, DOWN, IN }

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

@Composable
private fun DoubleTapPill(pillSettings: PillSettings, onExpand: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tapJob by remember { mutableStateOf<Job?>(null) }
    var tapCount by remember { mutableStateOf(0) }

    PillShape(
        pillSettings = pillSettings,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures {
                tapCount++
                if (tapCount == 1) {
                    tapJob = scope.launch {
                        delay(Constants.DOUBLE_TAP_WINDOW_MS)
                        tapCount = 0
                    }
                } else if (tapCount >= 2) {
                    tapJob?.cancel()
                    tapCount = 0
                    onExpand()
                }
            }
        }
    )
}

@Composable
private fun SwipePill(
    pillSettings: PillSettings,
    onExpand: () -> Unit,
    direction: SwipeDir
) {
    var firedThisGesture by remember { mutableStateOf(false) }
    PillShape(
        pillSettings = pillSettings,
        modifier = Modifier.pointerInput(direction) {
            detectDragGestures(
                onDragStart = { firedThisGesture = false },
                onDrag = { _, dragAmount ->
                    if (!firedThisGesture) {
                        val triggered = when (direction) {
                            SwipeDir.UP   -> dragAmount.y < -Constants.SWIPE_TRIGGER_THRESHOLD_PX && abs(dragAmount.y) > abs(dragAmount.x)
                            SwipeDir.DOWN -> dragAmount.y > Constants.SWIPE_TRIGGER_THRESHOLD_PX  && abs(dragAmount.y) > abs(dragAmount.x)
                            SwipeDir.IN   -> dragAmount.x > Constants.SWIPE_TRIGGER_THRESHOLD_PX  && abs(dragAmount.x) > abs(dragAmount.y)
                        }
                        if (triggered) {
                            firedThisGesture = true
                            onExpand()
                        }
                    }
                }
            )
        }
    )
}
