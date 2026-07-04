package com.alkisstam.taskbar.data

import android.graphics.Bitmap

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap?
)
