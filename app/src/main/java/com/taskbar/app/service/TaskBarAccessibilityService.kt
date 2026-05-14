package com.taskbar.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class TaskBarAccessibilityService : AccessibilityService() {

    val accessibilityWindowManager: WindowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onServiceConnected() {
        instance = this
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
    }

    fun showPowerMenu() {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: TaskBarAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
