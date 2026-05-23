package com.alkisstam.taskbar.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentAppsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun isPermissionGranted(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getRecentPackages(limit: Int = 15, excludePackages: Set<String> = emptySet()): List<String> {
        if (!isPermissionGranted()) return emptyList()
        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
        val end = System.currentTimeMillis()
        val start = end - 7L * 24 * 60 * 60 * 1000
        return usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .filter { it.packageName !in excludePackages && it.lastTimeUsed > 0 }
            .sortedByDescending { it.lastTimeUsed }
            .map { it.packageName }
            .distinct()
            .take(limit)
    }
}
