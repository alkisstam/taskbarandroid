package com.taskbar.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taskbar_prefs")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class PillGesture { SWIPE_UP, SWIPE_DOWN, SWIPE_IN, DOUBLE_TAP }

data class TaskbarSettings(
    val positionYDp: Float = 0f,
    val widthFraction: Float = 0.9f,
    val heightDp: Float = 64f
)

data class PillSettings(
    val gesture: PillGesture = PillGesture.SWIPE_UP,
    val widthDp: Float = 48f,
    val heightDp: Float = 32f,
    val alpha: Float = 0.75f,
    val positionYDp: Float = 80f,
    val positionXDp: Float = 16f
)

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val PINNED_APPS_KEY = stringPreferencesKey("pinned_apps")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val OVERLAY_ENABLED_KEY = booleanPreferencesKey("overlay_enabled")
        private val PILL_GESTURE_KEY = stringPreferencesKey("pill_gesture")
        private val PILL_WIDTH_KEY = floatPreferencesKey("pill_width")
        private val PILL_HEIGHT_KEY = floatPreferencesKey("pill_height")
        private val PILL_ALPHA_KEY = floatPreferencesKey("pill_alpha")
        private val PILL_POSITION_Y_KEY = floatPreferencesKey("pill_position_y")
        private val PILL_POSITION_X_KEY = floatPreferencesKey("pill_position_x")
        private val TASKBAR_POSITION_Y_KEY = floatPreferencesKey("taskbar_position_y")
        private val TASKBAR_WIDTH_KEY = floatPreferencesKey("taskbar_width_fraction")
        private val TASKBAR_HEIGHT_KEY = floatPreferencesKey("taskbar_height_dp")

        private fun serializePinnedApps(packages: List<String>): String =
            JSONArray(packages).toString()

        private fun deserializePinnedApps(stored: String): List<String> {
            return try {
                val arr = JSONArray(stored)
                List(arr.length()) { arr.getString(it) }
            } catch (e: JSONException) {
                // Migrate from legacy "||"-delimited format
                stored.split("||").filter { it.isNotBlank() }
            }
        }
    }

    val pinnedApps: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[PINNED_APPS_KEY]?.let { deserializePinnedApps(it) } ?: emptyList()
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val overlayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[OVERLAY_ENABLED_KEY] ?: false
    }

    suspend fun savePinnedApps(packages: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[PINNED_APPS_KEY] = serializePinnedApps(packages)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[OVERLAY_ENABLED_KEY] = enabled
        }
    }

    suspend fun pinApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PINNED_APPS_KEY]
                ?.let { deserializePinnedApps(it) }
                ?.toMutableList() ?: mutableListOf()
            if (!current.contains(packageName)) {
                current.add(packageName)
                prefs[PINNED_APPS_KEY] = serializePinnedApps(current)
            }
        }
    }

    suspend fun unpinApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PINNED_APPS_KEY]
                ?.let { deserializePinnedApps(it) }
                ?.toMutableList() ?: mutableListOf()
            current.remove(packageName)
            prefs[PINNED_APPS_KEY] = serializePinnedApps(current)
        }
    }

    val pillSettings: Flow<PillSettings> = context.dataStore.data.map { prefs ->
        PillSettings(
            gesture = when (prefs[PILL_GESTURE_KEY]) {
                "SWIPE_DOWN" -> PillGesture.SWIPE_DOWN
                "SWIPE_IN"   -> PillGesture.SWIPE_IN
                "DOUBLE_TAP" -> PillGesture.DOUBLE_TAP
                else          -> PillGesture.SWIPE_UP
            },
            widthDp     = prefs[PILL_WIDTH_KEY]      ?: 48f,
            heightDp    = prefs[PILL_HEIGHT_KEY]     ?: 32f,
            alpha       = prefs[PILL_ALPHA_KEY]      ?: 0.75f,
            positionYDp = prefs[PILL_POSITION_Y_KEY] ?: 80f,
            positionXDp = prefs[PILL_POSITION_X_KEY] ?: 16f
        )
    }

    val taskbarSettings: Flow<TaskbarSettings> = context.dataStore.data.map { prefs ->
        TaskbarSettings(
            positionYDp    = prefs[TASKBAR_POSITION_Y_KEY] ?: 0f,
            widthFraction  = prefs[TASKBAR_WIDTH_KEY]      ?: 0.9f,
            heightDp       = prefs[TASKBAR_HEIGHT_KEY]     ?: 64f
        )
    }

    suspend fun saveTaskbarSettings(settings: TaskbarSettings) {
        context.dataStore.edit { prefs ->
            prefs[TASKBAR_POSITION_Y_KEY] = settings.positionYDp
            prefs[TASKBAR_WIDTH_KEY]      = settings.widthFraction
            prefs[TASKBAR_HEIGHT_KEY]     = settings.heightDp
        }
    }

    suspend fun savePillSettings(settings: PillSettings) {
        context.dataStore.edit { prefs ->
            prefs[PILL_GESTURE_KEY]      = settings.gesture.name
            prefs[PILL_WIDTH_KEY]        = settings.widthDp
            prefs[PILL_HEIGHT_KEY]       = settings.heightDp
            prefs[PILL_ALPHA_KEY]        = settings.alpha
            prefs[PILL_POSITION_Y_KEY]   = settings.positionYDp
            prefs[PILL_POSITION_X_KEY]   = settings.positionXDp
        }
    }
}
