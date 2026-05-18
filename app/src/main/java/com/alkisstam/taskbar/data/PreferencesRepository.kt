package com.alkisstam.taskbar.data

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
    val positionYDp: Float = 20f,
    val widthFraction: Float = 0.9f,
    val heightDp: Float = 70f,
    val showLabels: Boolean = false
)

data class PillSettings(
    val gesture: PillGesture = PillGesture.SWIPE_UP,
    val widthDp: Float = 10f,
    val heightDp: Float = 60f,
    val alpha: Float = 0.60f,
    val positionYDp: Float = 80f,
    val positionXDp: Float = 16f
)

@Singleton
class PreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
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
        private val TASKBAR_SHOW_LABELS_KEY = booleanPreferencesKey("taskbar_show_labels")
        private val SURFACE_TINT_COLOR_KEY = stringPreferencesKey("surface_tint_color")
        private val AUTO_HIDE_FULLSCREEN_KEY = booleanPreferencesKey("auto_hide_fullscreen")
        private val AUTO_HIDE_LANDSCAPE_KEY = booleanPreferencesKey("auto_hide_landscape")
        private val QUICK_CONTROLS_STRIP_KEY = booleanPreferencesKey("quick_controls_strip")
        private val QUICK_CONTROLS_ENABLED_KEY = booleanPreferencesKey("quick_controls_enabled")
        private val CONTROLS_ORDER_KEY = stringPreferencesKey("controls_order")
        private val CONTROLS_DISABLED_KEY = stringPreferencesKey("controls_disabled_ids")
        private val TASKBAR_VISIBLE_KEY = booleanPreferencesKey("taskbar_visible")
        private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")

        val ALL_CONTROL_IDS = listOf("torch", "ringer", "rotate", "brightness_slider", "dnd", "qr", "power", "volume")

        private fun serializeStringList(list: List<String>): String = JSONArray(list).toString()
        private fun deserializeStringList(stored: String): List<String> =
            try { val a = JSONArray(stored); List(a.length()) { a.getString(it) } }
            catch (e: JSONException) { emptyList() }

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
            widthDp     = prefs[PILL_WIDTH_KEY]      ?: 10f,
            heightDp    = prefs[PILL_HEIGHT_KEY]     ?: 60f,
            alpha       = prefs[PILL_ALPHA_KEY]      ?: 0.60f,
            positionYDp = prefs[PILL_POSITION_Y_KEY] ?: 80f,
            positionXDp = prefs[PILL_POSITION_X_KEY] ?: 16f
        )
    }

    val autoHideInFullscreen: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_HIDE_FULLSCREEN_KEY] ?: false
    }

    suspend fun setAutoHideInFullscreen(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_HIDE_FULLSCREEN_KEY] = enabled
        }
    }

    val autoHideInLandscape: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_HIDE_LANDSCAPE_KEY] ?: false
    }

    suspend fun setAutoHideInLandscape(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_HIDE_LANDSCAPE_KEY] = enabled
        }
    }

    val quickControlsStripEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[QUICK_CONTROLS_STRIP_KEY] ?: false
    }

    suspend fun setQuickControlsStripEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[QUICK_CONTROLS_STRIP_KEY] = enabled
        }
    }

    val quickControlsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[QUICK_CONTROLS_ENABLED_KEY] ?: true
    }

    suspend fun setQuickControlsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[QUICK_CONTROLS_ENABLED_KEY] = enabled
        }
    }

    val surfaceTintColor: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[SURFACE_TINT_COLOR_KEY]?.toLongOrNull() ?: 0L
    }

    suspend fun setSurfaceTintColor(color: Long) {
        context.dataStore.edit { prefs ->
            prefs[SURFACE_TINT_COLOR_KEY] = color.toString()
        }
    }

    val taskbarSettings: Flow<TaskbarSettings> = context.dataStore.data.map { prefs ->
        TaskbarSettings(
            positionYDp   = prefs[TASKBAR_POSITION_Y_KEY]  ?: 20f,
            widthFraction = prefs[TASKBAR_WIDTH_KEY]       ?: 0.9f,
            heightDp      = prefs[TASKBAR_HEIGHT_KEY]      ?: 70f,
            showLabels    = prefs[TASKBAR_SHOW_LABELS_KEY] ?: false
        )
    }

    suspend fun saveTaskbarSettings(settings: TaskbarSettings) {
        context.dataStore.edit { prefs ->
            prefs[TASKBAR_POSITION_Y_KEY]  = settings.positionYDp
            prefs[TASKBAR_WIDTH_KEY]       = settings.widthFraction
            prefs[TASKBAR_HEIGHT_KEY]      = settings.heightDp
            prefs[TASKBAR_SHOW_LABELS_KEY] = settings.showLabels
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

    val taskbarVisible: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TASKBAR_VISIBLE_KEY] ?: true
    }

    suspend fun setTaskbarVisible(visible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TASKBAR_VISIBLE_KEY] = visible
        }
    }

    val controlsOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[CONTROLS_ORDER_KEY]
            ?.let { deserializeStringList(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: ALL_CONTROL_IDS
    }

    suspend fun saveControlsOrder(order: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[CONTROLS_ORDER_KEY] = serializeStringList(order)
        }
    }

    val controlsDisabledIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[CONTROLS_DISABLED_KEY]
            ?.let { deserializeStringList(it).toSet() }
            ?: emptySet()
    }

    suspend fun saveControlsDisabledIds(ids: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[CONTROLS_DISABLED_KEY] = serializeStringList(ids.toList())
        }
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETE_KEY] ?: false
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE_KEY] = true
        }
    }
}
