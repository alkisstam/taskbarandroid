package com.alkisstam.taskbar.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager

internal fun overlayWindowType() =
    if (TaskBarAccessibilityService.isRunning())
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    else
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

internal fun Context.overlayLayoutParams(interactive: Boolean = true, focusable: Boolean = false): WindowManager.LayoutParams {
    val usingAccessibility = TaskBarAccessibilityService.isRunning()
    val flags = (if (!focusable) WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE else 0) or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (!interactive) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0) or
            (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
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

internal fun Context.quickStripLayoutParams(interactive: Boolean = false, yOffsetDp: Float = 0f): WindowManager.LayoutParams {
    val usingAccessibility = TaskBarAccessibilityService.isRunning()
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (!interactive) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0) or
            (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
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

internal fun Context.pillLayoutParams(positionXDp: Float = 16f, positionYDp: Float = 80f): WindowManager.LayoutParams {
    val usingAccessibility = TaskBarAccessibilityService.isRunning()
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
    val density = resources.displayMetrics.density
    return WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayWindowType(),
        flags,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.START
        x = (positionXDp * density).toInt()
        y = (positionYDp * density).toInt()
    }
}

internal fun Context.searchLayoutParams(): WindowManager.LayoutParams {
    val usingAccessibility = TaskBarAccessibilityService.isRunning()
    val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
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
    val usingAccessibility = TaskBarAccessibilityService.isRunning()
    val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
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

internal fun Context.volumeScrimLayoutParams(): WindowManager.LayoutParams {
    val usingAccessibility = TaskBarAccessibilityService.isRunning()
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
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
    val usingAccessibility = TaskBarAccessibilityService.isRunning()
    val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (usingAccessibility) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else 0)
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
