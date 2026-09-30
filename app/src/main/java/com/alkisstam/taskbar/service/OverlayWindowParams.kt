package com.alkisstam.taskbar.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.alkisstam.taskbar.data.PillEdgePosition
import com.alkisstam.taskbar.data.PillSettings

internal fun overlayWindowType() =
    if (TaskBarAccessibilityService.instance != null)
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    else
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

private fun Context.applyBlurBehind(params: WindowManager.LayoutParams, blur: Boolean, radiusDp: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (blur && radiusDp > 0f && getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true) {
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        params.blurBehindRadius = (radiusDp * resources.displayMetrics.density).toInt()
    }
}

internal fun Context.overlayLayoutParams(interactive: Boolean = true, focusable: Boolean = false): WindowManager.LayoutParams {
    val flags = (if (!focusable) WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0) or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (!interactive) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0) or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = 0
    }
}

internal fun Context.pillLayoutParams(
    settings: PillSettings,
    isRight: Boolean = false,
    touchable: Boolean = true
): WindowManager.LayoutParams {
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (!touchable) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)
    val density = resources.displayMetrics.density
    val triggerPx = (settings.triggerAreaDp * density).toInt()
    val sideStripPx = triggerPx
    val bottomStripPx = maxOf(triggerPx, (settings.heightDp * density).toInt())
    val side = if (settings.edgePosition == PillEdgePosition.BOTH) {
        if (isRight) PillEdgePosition.RIGHT else PillEdgePosition.LEFT
    } else settings.edgePosition
    val restrict = settings.restrictTriggerToPill
    val sideLengthPx = if (restrict) (settings.heightDp * density).toInt() else WindowManager.LayoutParams.MATCH_PARENT
    val bottomLengthPx = if (restrict) (settings.widthDp * density).toInt() else WindowManager.LayoutParams.MATCH_PARENT
    return WindowManager.LayoutParams(
        if (side == PillEdgePosition.BOTTOM) bottomLengthPx else sideStripPx,
        if (side == PillEdgePosition.BOTTOM) bottomStripPx else sideLengthPx,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        when (side) {
            PillEdgePosition.LEFT -> {
                gravity = Gravity.START or Gravity.TOP
                x = 0
                y = if (restrict) sidePositionOffsetPx(settings, density) else 0
            }
            PillEdgePosition.RIGHT -> {
                gravity = Gravity.END or Gravity.TOP
                x = 0
                y = if (restrict) sidePositionOffsetPx(settings, density) else 0
            }
            else -> {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                x = bottomPositionOffsetPx(settings, density)
                y = (settings.positionYDp * density).toInt()
            }
        }
    }
}

private fun Context.sidePositionOffsetPx(settings: PillSettings, density: Float): Int {
    val screenHeightPx = resources.displayMetrics.heightPixels
    val pillHeightPx = (settings.heightDp * density).toInt()
    val availableH = (screenHeightPx - pillHeightPx).coerceAtLeast(0)
    return (settings.sidePositionPct / 100f * availableH).toInt()
}

private fun Context.bottomPositionOffsetPx(settings: PillSettings, density: Float): Int {
    val screenWidthPx = resources.displayMetrics.widthPixels
    val pillWidthPx = (settings.widthDp * density).toInt()
    val availableW = (screenWidthPx - pillWidthPx).coerceAtLeast(0)
    return ((settings.positionXPct - 50f) / 100f * availableW).toInt()
}

internal fun Context.searchLayoutParams(focusable: Boolean = false, blurBehind: Boolean = false, blurRadiusDp: Float = 0f): WindowManager.LayoutParams {
    val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (!focusable) WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0) or
            (if (!focusable) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        // Without this the system picks ADJUST_PAN and shifts the whole window above the
        // keyboard, doubling the inset-driven composer lift done in Compose.
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        applyBlurBehind(this, blurBehind, blurRadiusDp)
    }
}

internal fun Context.clipboardBlurLayoutParams(blurBehind: Boolean = false, blurRadiusDp: Float = 0f): WindowManager.LayoutParams {
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    // Collapse to 0x0 when idle so the always-attached window has no surface to composite.
    val size = if (blurBehind) WindowManager.LayoutParams.MATCH_PARENT else 0
    return WindowManager.LayoutParams(
        size,
        size,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        applyBlurBehind(this, blurBehind, blurRadiusDp)
    }
}

internal fun Context.volumePanelLayoutParams(yOffsetDp: Float, translucentMode: Boolean = false, active: Boolean = true): WindowManager.LayoutParams {
    val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (!active) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0)
    val density = resources.displayMetrics.density
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = (yOffsetDp * density).toInt()
    }
}

internal fun Context.levelIndicatorLayoutParams(): WindowManager.LayoutParams {
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = (72 * resources.displayMetrics.density).toInt()
    }
}

internal fun Context.taskbarLayoutParams(interactive: Boolean = true): WindowManager.LayoutParams {
    val flags = (if (!interactive) WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0) or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (!interactive) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = 0
    }
}

internal fun Context.volumeScrimLayoutParams(active: Boolean = false): WindowManager.LayoutParams {
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (!active) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }
}

internal fun Context.musicPanelLayoutParams(yOffsetDp: Float, translucentMode: Boolean = false, active: Boolean = true): WindowManager.LayoutParams {
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (!active) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)
    val density = resources.displayMetrics.density
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = (yOffsetDp * density).toInt()
    }
}

// Invisible window over a pinned dock that forwards its touches to the (then untouchable)
// full-screen dock window. Only this window moves/resizes, and it draws nothing, so the
// dock itself never flickers.
internal fun Context.dockTouchProxyLayoutParams(bounds: Rect?): WindowManager.LayoutParams {
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            (if (bounds == null) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0)
    return WindowManager.LayoutParams(
        bounds?.width() ?: 1,
        bounds?.height() ?: 1,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = bounds?.left ?: 0
        y = bounds?.top ?: 0
    }
}
