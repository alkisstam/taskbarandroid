package com.alkisstam.taskbar.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import com.alkisstam.taskbar.data.PillEdgePosition

internal fun overlayWindowType() =
    if (TaskBarAccessibilityService.instance != null)
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    else
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

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
    edgePosition: PillEdgePosition = PillEdgePosition.BOTTOM,
    isRight: Boolean = false,
    sidePositionPct: Float = 50f,
    triggerAreaDp: Float = 18f
): WindowManager.LayoutParams {
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    val density = resources.displayMetrics.density
    val triggerPx = (triggerAreaDp * density).toInt()
    val sideStripPx = triggerPx
    val bottomStripPx = triggerPx
    val side = if (edgePosition == PillEdgePosition.BOTH) {
        if (isRight) PillEdgePosition.RIGHT else PillEdgePosition.LEFT
    } else edgePosition
    return WindowManager.LayoutParams(
        if (side == PillEdgePosition.BOTTOM) WindowManager.LayoutParams.MATCH_PARENT else sideStripPx,
        if (side == PillEdgePosition.BOTTOM) bottomStripPx else WindowManager.LayoutParams.MATCH_PARENT,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        when (side) {
            PillEdgePosition.LEFT -> {
                gravity = Gravity.START or Gravity.TOP
                x = 0
                y = 0
            }
            PillEdgePosition.RIGHT -> {
                gravity = Gravity.END or Gravity.TOP
                x = 0
                y = 0
            }
            else -> {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                x = 0
                y = 0
            }
        }
    }
}

internal fun Context.searchLayoutParams(focusable: Boolean = false): WindowManager.LayoutParams {
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
    }
}

internal fun Context.volumePanelLayoutParams(yOffsetDp: Float): WindowManager.LayoutParams {
    
    val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
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

internal fun Context.musicPanelLayoutParams(yOffsetDp: Float): WindowManager.LayoutParams {
    
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
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
