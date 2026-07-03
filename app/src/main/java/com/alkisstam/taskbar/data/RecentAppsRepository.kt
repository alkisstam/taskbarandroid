package com.alkisstam.taskbar.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log
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
        return try {
            val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
            val end = System.currentTimeMillis()
            val start = end - 7L * 24 * 60 * 60 * 1000
            val events = usageStatsManager.queryEvents(start, end)
            val event = android.app.usage.UsageEvents.Event()
            val lastUsed = mutableMapOf<String, Long>()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    val pkg = event.packageName
                    if (event.timeStamp > (lastUsed[pkg] ?: 0L)) lastUsed[pkg] = event.timeStamp
                }
            }
            lastUsed.entries
                .filter { it.key !in excludePackages }
                .sortedByDescending { it.value }
                .map { it.key }
                .take(limit)
        } catch (e: Exception) {
            Log.w("RecentAppsRepository", "Failed to query recent apps", e)
            emptyList()
        }
    }
}
