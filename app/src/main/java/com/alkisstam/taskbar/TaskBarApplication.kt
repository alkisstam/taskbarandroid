package com.alkisstam.taskbar

import android.app.Application
import android.os.Build
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TaskBarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Tells pairip license-check crashes from sideloads apart from real Play installs.
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
        FirebaseCrashlytics.getInstance().setCustomKey("installer", installer ?: "none")
    }
}
