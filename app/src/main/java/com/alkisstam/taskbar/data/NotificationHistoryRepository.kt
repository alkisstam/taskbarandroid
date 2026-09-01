package com.alkisstam.taskbar.data

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_history_prefs")

data class NotificationEntry(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

@Singleton
class NotificationHistoryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NotificationHistoryRepo"
        private val NOTIFICATIONS_KEY = stringPreferencesKey("notifications")
        private const val MAX_NOTIFICATIONS = 100
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val labelCache = mutableMapOf<String, String>()
    // PendingIntents can't be serialized, so quick actions only exist for notifications
    // captured during this process's lifetime.
    private val actionsByKey = mutableMapOf<String, List<Notification.Action>>()

    fun actionsFor(id: String): List<Notification.Action> =
        synchronized(actionsByKey) { actionsByKey[id] ?: emptyList() }

    private val safeData: Flow<Preferences> = context.notificationHistoryDataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val notifications: Flow<List<NotificationEntry>> = safeData.map { prefs ->
        prefs[NOTIFICATIONS_KEY]?.let { deserializeEntries(it) } ?: emptyList()
    }

    fun record(sbn: StatusBarNotification) {
        if (sbn.isOngoing) return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (sbn.packageName == context.packageName) return
        if (notification.category == Notification.CATEGORY_TRANSPORT) return
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().stripImageSpanChar()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()).stripImageSpanChar()
        if (title.isBlank() && text.isBlank()) return

        val actions = notification.actions
            ?.filter { it.actionIntent != null && it.remoteInputs.isNullOrEmpty() && !it.title.isNullOrBlank() }
            .orEmpty()
        scope.launch {
            // resolveAppLabel does a binder IPC to the package manager; record() runs on
            // the listener's main thread, and that call stalling has caused ANRs.
            val entry = NotificationEntry(
                id = sbn.key,
                packageName = sbn.packageName,
                appLabel = resolveAppLabel(sbn.packageName),
                title = title,
                text = text,
                timestamp = sbn.postTime
            )
            try {
                context.notificationHistoryDataStore.edit { prefs ->
                    val current = prefs[NOTIFICATIONS_KEY]?.let { deserializeEntries(it) } ?: emptyList()
                    val updated = (listOf(entry) + current.filter { it.id != entry.id }).take(MAX_NOTIFICATIONS)
                    prefs[NOTIFICATIONS_KEY] = serializeEntries(updated)
                    synchronized(actionsByKey) {
                        if (actions.isEmpty()) actionsByKey.remove(entry.id) else actionsByKey[entry.id] = actions
                        actionsByKey.keys.retainAll(updated.map { it.id }.toSet())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record notification", e)
            }
        }
    }

    suspend fun remove(id: String) {
        context.notificationHistoryDataStore.edit { prefs ->
            val current = prefs[NOTIFICATIONS_KEY]?.let { deserializeEntries(it) } ?: emptyList()
            prefs[NOTIFICATIONS_KEY] = serializeEntries(current.filter { it.id != id })
        }
        synchronized(actionsByKey) { actionsByKey.remove(id) }
    }

    suspend fun clearAll() {
        context.notificationHistoryDataStore.edit { prefs ->
            prefs[NOTIFICATIONS_KEY] = serializeEntries(emptyList())
        }
        synchronized(actionsByKey) { actionsByKey.clear() }
    }

    private fun String.stripImageSpanChar(): String = replace("￼", "").trim()

    private fun resolveAppLabel(packageName: String): String {
        synchronized(labelCache) { labelCache[packageName] }?.let { return it }
        val label = try {
            val pm = context.packageManager
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (e: Exception) {
            packageName
        }
        synchronized(labelCache) { labelCache[packageName] = label }
        return label
    }

    internal fun serializeEntries(entries: List<NotificationEntry>): String {
        val arr = JSONArray()
        entries.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("packageName", item.packageName)
                put("appLabel", item.appLabel)
                put("title", item.title)
                put("text", item.text)
                put("timestamp", item.timestamp)
            })
        }
        return arr.toString()
    }

    internal fun deserializeEntries(json: String): List<NotificationEntry> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            try {
                val obj = arr.getJSONObject(i)
                NotificationEntry(
                    id = obj.getString("id"),
                    packageName = obj.getString("packageName"),
                    appLabel = obj.getString("appLabel"),
                    title = obj.getString("title"),
                    text = obj.getString("text"),
                    timestamp = obj.getLong("timestamp")
                )
            } catch (e: Exception) {
                Log.w(TAG, "Skipping corrupt notification record at index $i", e)
                null
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse notifications JSON", e)
        emptyList()
    }
}
