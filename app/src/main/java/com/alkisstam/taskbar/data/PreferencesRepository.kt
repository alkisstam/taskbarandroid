package com.alkisstam.taskbar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.emptyPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taskbar_prefs")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class GestureAction { SHOW_DOCK, SHOW_NOTIFICATIONS, SHOW_QUICK_SETTINGS, DISABLED }

private fun String?.toGestureAction() = when (this) {
    "SHOW_NOTIFICATIONS"  -> GestureAction.SHOW_NOTIFICATIONS
    "SHOW_QUICK_SETTINGS" -> GestureAction.SHOW_QUICK_SETTINGS
    "DISABLED"            -> GestureAction.DISABLED
    else                  -> GestureAction.SHOW_DOCK
}

enum class PillEdgePosition { BOTTOM, LEFT, RIGHT, BOTH }

enum class DockPadding { DEFAULT, SMALL, LARGE }

val DockPadding.bottomGapDp: Float
    get() = when (this) {
        DockPadding.DEFAULT -> 20f
        DockPadding.SMALL -> 28f
        DockPadding.LARGE -> 40f
    }

val DockPadding.widthFraction: Float
    get() = when (this) {
        DockPadding.DEFAULT -> 0.98f
        DockPadding.SMALL -> 0.94f
        DockPadding.LARGE -> 0.88f
    }

private fun String?.toPillEdgePosition() = when (this) {
    "LEFT"   -> PillEdgePosition.LEFT
    "RIGHT"  -> PillEdgePosition.RIGHT
    "BOTH"   -> PillEdgePosition.BOTH
    "BOTTOM" -> PillEdgePosition.BOTTOM
    else     -> PillEdgePosition.RIGHT
}

data class TaskbarSettings(
    val positionYDp: Float = 20f,
    val heightDp: Float = 60f,
    val showControlLabels: Boolean = false,
    val pinnedIconSizeDp: Float = 40f,
    val quickControlSizeDp: Float = 42f,
    val cornerRadiusDp: Float = 16f,
    val dockPadding: DockPadding = DockPadding.DEFAULT
)

data class PillSettings(
    val swipeUpAction: GestureAction = GestureAction.SHOW_DOCK,
    val swipeDownAction: GestureAction = GestureAction.SHOW_DOCK,
    val doubleTapAction: GestureAction = GestureAction.SHOW_DOCK,
    val widthDp: Float = 4f,
    val heightDp: Float = 50f,
    val alpha: Float = 0.40f,
    val positionYDp: Float = 12f,
    val positionXPct: Float = 4f,
    val edgePosition: PillEdgePosition = PillEdgePosition.BOTTOM,
    val sidePositionPct: Float = 50f,
    val triggerAreaDp: Float = 18f,
    val restrictTriggerToPill: Boolean = false
)

@Singleton
class PreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // DataStore's .data Flow throws IOException on read/corruption; fall back to
    // defaults instead of crashing every collector across the app.
    private val safeData: Flow<Preferences> = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    companion object {
        private val PINNED_APPS_KEY = stringPreferencesKey("pinned_apps")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val OVERLAY_ENABLED_KEY = booleanPreferencesKey("overlay_enabled")
        private val PILL_SWIPE_UP_ACTION_KEY = stringPreferencesKey("pill_swipe_up_action")
        private val PILL_SWIPE_DOWN_ACTION_KEY = stringPreferencesKey("pill_swipe_down_action")
        private val PILL_DOUBLE_TAP_ACTION_KEY = stringPreferencesKey("pill_double_tap_action")
        private val PILL_WIDTH_KEY = floatPreferencesKey("pill_width")
        private val PILL_HEIGHT_KEY = floatPreferencesKey("pill_height")
        private val PILL_ALPHA_KEY = floatPreferencesKey("pill_alpha")
        private val PILL_POSITION_Y_KEY = floatPreferencesKey("pill_position_y")
        private val PILL_POSITION_X_KEY = floatPreferencesKey("pill_position_x")
        private val PILL_POSITION_X_PCT_KEY = floatPreferencesKey("pill_position_x_pct")
        private val PILL_EDGE_POSITION_KEY = stringPreferencesKey("pill_edge_position")
        private val PILL_SIDE_POSITION_PCT_KEY = floatPreferencesKey("pill_side_position_pct")
        private val TASKBAR_POSITION_Y_KEY = floatPreferencesKey("taskbar_position_y")
        private val TASKBAR_HEIGHT_KEY = floatPreferencesKey("taskbar_height_dp")
        private val TASKBAR_CONTROL_LABELS_KEY = booleanPreferencesKey("taskbar_control_labels")
        private val PINNED_ICON_SIZE_KEY = floatPreferencesKey("pinned_icon_size_dp")
        private val QUICK_CONTROL_SIZE_KEY = floatPreferencesKey("quick_control_size_dp")
        private val TASKBAR_CORNER_RADIUS_KEY = floatPreferencesKey("taskbar_corner_radius_dp")
        private val TASKBAR_DOCK_PADDING_KEY = stringPreferencesKey("taskbar_dock_padding")
        private val SURFACE_TINT_COLOR_KEY = stringPreferencesKey("surface_tint_color")
        private val AUTO_HIDE_FULLSCREEN_KEY = booleanPreferencesKey("auto_hide_fullscreen")
        private val AUTO_HIDE_LANDSCAPE_KEY = booleanPreferencesKey("auto_hide_landscape")
        private val QUICK_CONTROLS_ENABLED_KEY = booleanPreferencesKey("quick_controls_enabled")
        private val CONTROLS_ORDER_KEY = stringPreferencesKey("controls_order")
        private val CONTROLS_DISABLED_KEY = stringPreferencesKey("controls_disabled_ids")
        private val TASKBAR_VISIBLE_KEY = booleanPreferencesKey("taskbar_visible")
        private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
        private val LAST_SEEN_VERSION_CODE_KEY = intPreferencesKey("last_seen_version_code")
        private val MUSIC_PANEL_ENABLED_KEY = booleanPreferencesKey("music_panel_enabled")
        private val MUSIC_PANEL_OPEN_KEY = booleanPreferencesKey("music_panel_open")
        private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback")
        private val PANEL_OUTLINE_KEY = booleanPreferencesKey("panel_outline_enabled")
        private val APP_GRID_COLUMNS_KEY = intPreferencesKey("app_grid_columns")
        private val APP_GRID_ROWS_KEY = intPreferencesKey("app_grid_rows")
        private val PILL_TRIGGER_AREA_KEY = floatPreferencesKey("pill_trigger_area")
        private val PILL_RESTRICT_TRIGGER_KEY = booleanPreferencesKey("pill_restrict_trigger")
        private val TRANSLUCENT_MODE_KEY = booleanPreferencesKey("translucent_mode")
        private val TRANSLUCENT_ALPHA_KEY = floatPreferencesKey("translucent_alpha")

        val ALL_CONTROL_IDS = listOf("torch", "ringer", "rotate", "brightness_slider", "dnd", "qr", "power", "volume", "screenshot", "lockscreen", "caffeine", "clipboard", "calculator", "wifi", "bluetooth", "share")

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

    val pinnedApps: Flow<List<String>> = safeData.map { prefs ->
        prefs[PINNED_APPS_KEY]?.let { deserializePinnedApps(it) } ?: emptyList()
    }

    val themeMode: Flow<ThemeMode> = safeData.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val overlayEnabled: Flow<Boolean> = safeData.map { prefs ->
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

    val pillSettings: Flow<PillSettings> = safeData.map { prefs ->
        PillSettings(
            swipeUpAction    = prefs[PILL_SWIPE_UP_ACTION_KEY].toGestureAction(),
            swipeDownAction  = prefs[PILL_SWIPE_DOWN_ACTION_KEY].toGestureAction(),
            doubleTapAction  = prefs[PILL_DOUBLE_TAP_ACTION_KEY].toGestureAction(),
            widthDp      = prefs[PILL_WIDTH_KEY]           ?: 4f,
            heightDp     = prefs[PILL_HEIGHT_KEY]          ?: 50f,
            alpha        = prefs[PILL_ALPHA_KEY]            ?: 0.40f,
            positionYDp      = prefs[PILL_POSITION_Y_KEY]         ?: 12f,
            positionXPct     = prefs[PILL_POSITION_X_PCT_KEY]     ?: 4f,
            edgePosition     = prefs[PILL_EDGE_POSITION_KEY].toPillEdgePosition(),
            sidePositionPct  = prefs[PILL_SIDE_POSITION_PCT_KEY]  ?: 50f,
            triggerAreaDp    = prefs[PILL_TRIGGER_AREA_KEY]       ?: 18f,
            restrictTriggerToPill = prefs[PILL_RESTRICT_TRIGGER_KEY] ?: false
        )
    }

    val autoHideInFullscreen: Flow<Boolean> = safeData.map { prefs ->
        prefs[AUTO_HIDE_FULLSCREEN_KEY] ?: false
    }

    suspend fun setAutoHideInFullscreen(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_HIDE_FULLSCREEN_KEY] = enabled
        }
    }

    val autoHideInLandscape: Flow<Boolean> = safeData.map { prefs ->
        prefs[AUTO_HIDE_LANDSCAPE_KEY] ?: false
    }

    suspend fun setAutoHideInLandscape(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_HIDE_LANDSCAPE_KEY] = enabled
        }
    }

    val hapticFeedbackEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[HAPTIC_FEEDBACK_KEY] ?: true
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HAPTIC_FEEDBACK_KEY] = enabled
        }
    }

    val quickControlsEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[QUICK_CONTROLS_ENABLED_KEY] ?: true
    }

    suspend fun setQuickControlsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[QUICK_CONTROLS_ENABLED_KEY] = enabled
        }
    }

    val surfaceTintColor: Flow<Long> = safeData.map { prefs ->
        prefs[SURFACE_TINT_COLOR_KEY]?.toLongOrNull() ?: 0L
    }

    suspend fun setSurfaceTintColor(color: Long) {
        context.dataStore.edit { prefs ->
            prefs[SURFACE_TINT_COLOR_KEY] = color.toString()
        }
    }

    val taskbarSettings: Flow<TaskbarSettings> = safeData.map { prefs ->
        TaskbarSettings(
            positionYDp        = prefs[TASKBAR_POSITION_Y_KEY]     ?: 20f,
            heightDp           = prefs[TASKBAR_HEIGHT_KEY]          ?: 60f,
            showControlLabels  = prefs[TASKBAR_CONTROL_LABELS_KEY]  ?: false,
            pinnedIconSizeDp   = prefs[PINNED_ICON_SIZE_KEY]        ?: 40f,
            quickControlSizeDp = prefs[QUICK_CONTROL_SIZE_KEY]      ?: 42f,
            cornerRadiusDp     = prefs[TASKBAR_CORNER_RADIUS_KEY]   ?: 16f,
            dockPadding        = prefs[TASKBAR_DOCK_PADDING_KEY]?.let { runCatching { DockPadding.valueOf(it) }.getOrNull() } ?: DockPadding.DEFAULT
        )
    }

    suspend fun saveTaskbarSettings(settings: TaskbarSettings) {
        context.dataStore.edit { prefs ->
            prefs[TASKBAR_POSITION_Y_KEY]     = settings.positionYDp
            prefs[TASKBAR_HEIGHT_KEY]         = settings.heightDp
            prefs[TASKBAR_CONTROL_LABELS_KEY] = settings.showControlLabels
            prefs[PINNED_ICON_SIZE_KEY]       = settings.pinnedIconSizeDp
            prefs[QUICK_CONTROL_SIZE_KEY]     = settings.quickControlSizeDp
            prefs[TASKBAR_CORNER_RADIUS_KEY]  = settings.cornerRadiusDp
            prefs[TASKBAR_DOCK_PADDING_KEY]   = settings.dockPadding.name
        }
    }

    suspend fun savePillSettings(settings: PillSettings) {
        context.dataStore.edit { prefs ->
            prefs[PILL_SWIPE_UP_ACTION_KEY]    = settings.swipeUpAction.name
            prefs[PILL_SWIPE_DOWN_ACTION_KEY]  = settings.swipeDownAction.name
            prefs[PILL_DOUBLE_TAP_ACTION_KEY]  = settings.doubleTapAction.name
            prefs[PILL_WIDTH_KEY]          = settings.widthDp
            prefs[PILL_HEIGHT_KEY]         = settings.heightDp
            prefs[PILL_ALPHA_KEY]          = settings.alpha
            prefs[PILL_POSITION_Y_KEY]         = settings.positionYDp
            prefs[PILL_POSITION_X_PCT_KEY]     = settings.positionXPct
            prefs[PILL_EDGE_POSITION_KEY]      = settings.edgePosition.name
            prefs[PILL_SIDE_POSITION_PCT_KEY]  = settings.sidePositionPct
            prefs[PILL_TRIGGER_AREA_KEY]       = settings.triggerAreaDp
            prefs[PILL_RESTRICT_TRIGGER_KEY]   = settings.restrictTriggerToPill
        }
    }

    val taskbarVisible: Flow<Boolean> = safeData.map { prefs ->
        prefs[TASKBAR_VISIBLE_KEY] ?: true
    }

    suspend fun setTaskbarVisible(visible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TASKBAR_VISIBLE_KEY] = visible
        }
    }

    val controlsOrder: Flow<List<String>> = safeData.map { prefs ->
        val saved = prefs[CONTROLS_ORDER_KEY]
            ?.let { deserializeStringList(it) }
            ?.takeIf { it.isNotEmpty() }
        if (saved == null) {
            ALL_CONTROL_IDS
        } else {
            // Merge any new control IDs that were added in app updates
            val newIds = ALL_CONTROL_IDS.filter { it !in saved }
            if (newIds.isNotEmpty()) saved + newIds else saved
        }
    }

    suspend fun saveControlsOrder(order: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[CONTROLS_ORDER_KEY] = serializeStringList(order)
        }
    }

    val controlsDisabledIds: Flow<Set<String>> = safeData.map { prefs ->
        prefs[CONTROLS_DISABLED_KEY]
            ?.let { deserializeStringList(it).toSet() }
            ?: emptySet()
    }

    suspend fun saveControlsDisabledIds(ids: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[CONTROLS_DISABLED_KEY] = serializeStringList(ids.toList())
        }
    }

    val onboardingComplete: Flow<Boolean> = safeData.map { prefs ->
        prefs[ONBOARDING_COMPLETE_KEY] ?: false
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE_KEY] = true
        }
    }

    val lastSeenVersionCode: Flow<Int> = safeData.map { prefs ->
        prefs[LAST_SEEN_VERSION_CODE_KEY] ?: 0
    }

    suspend fun setLastSeenVersionCode(code: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SEEN_VERSION_CODE_KEY] = code
        }
    }

    val musicPanelEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[MUSIC_PANEL_ENABLED_KEY] ?: false
    }

    suspend fun setMusicPanelEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[MUSIC_PANEL_ENABLED_KEY] = enabled
        }
    }

    val musicPanelOpen: Flow<Boolean> = safeData.map { prefs ->
        prefs[MUSIC_PANEL_OPEN_KEY] ?: false
    }

    suspend fun setMusicPanelOpen(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[MUSIC_PANEL_OPEN_KEY] = value
        }
    }

    val panelOutlineEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[PANEL_OUTLINE_KEY] ?: false
    }

    suspend fun setPanelOutlineEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PANEL_OUTLINE_KEY] = enabled
        }
    }

    val translucentMode: Flow<Boolean> = safeData.map { prefs ->
        prefs[TRANSLUCENT_MODE_KEY] ?: false
    }

    suspend fun setTranslucentMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TRANSLUCENT_MODE_KEY] = enabled
        }
    }

    val translucentAlpha: Flow<Float> = safeData.map { prefs ->
        prefs[TRANSLUCENT_ALPHA_KEY] ?: 0.80f
    }

    suspend fun setTranslucentAlpha(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[TRANSLUCENT_ALPHA_KEY] = value
        }
    }

    val appGridColumns: Flow<Int> = safeData.map { prefs ->
        prefs[APP_GRID_COLUMNS_KEY] ?: 4
    }

    suspend fun setAppGridColumns(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[APP_GRID_COLUMNS_KEY] = value
        }
    }

    val appGridRows: Flow<Int> = safeData.map { prefs ->
        prefs[APP_GRID_ROWS_KEY] ?: 4
    }

    suspend fun setAppGridRows(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[APP_GRID_ROWS_KEY] = value
        }
    }

    suspend fun exportToJson(): String {
        val prefs = safeData.first()
        return JSONObject().apply {
            prefs[PINNED_APPS_KEY]?.let { put("pinned_apps", it) }
            prefs[THEME_MODE_KEY]?.let { put("theme_mode", it) }
            prefs[OVERLAY_ENABLED_KEY]?.let { put("overlay_enabled", it) }
            prefs[PILL_SWIPE_UP_ACTION_KEY]?.let { put("pill_swipe_up_action", it) }
            prefs[PILL_SWIPE_DOWN_ACTION_KEY]?.let { put("pill_swipe_down_action", it) }
            prefs[PILL_DOUBLE_TAP_ACTION_KEY]?.let { put("pill_double_tap_action", it) }
            prefs[PILL_WIDTH_KEY]?.let { put("pill_width", it) }
            prefs[PILL_HEIGHT_KEY]?.let { put("pill_height", it) }
            prefs[PILL_ALPHA_KEY]?.let { put("pill_alpha", it) }
            prefs[PILL_POSITION_Y_KEY]?.let { put("pill_position_y", it) }
            prefs[PILL_POSITION_X_PCT_KEY]?.let { put("pill_position_x_pct", it) }
            prefs[PILL_EDGE_POSITION_KEY]?.let { put("pill_edge_position", it) }
            prefs[PILL_SIDE_POSITION_PCT_KEY]?.let { put("pill_side_position_pct", it) }
            prefs[TASKBAR_POSITION_Y_KEY]?.let { put("taskbar_position_y", it) }
            prefs[TASKBAR_HEIGHT_KEY]?.let { put("taskbar_height_dp", it) }
            prefs[TASKBAR_CONTROL_LABELS_KEY]?.let { put("taskbar_control_labels", it) }
            prefs[PINNED_ICON_SIZE_KEY]?.let { put("pinned_icon_size_dp", it) }
            prefs[QUICK_CONTROL_SIZE_KEY]?.let { put("quick_control_size_dp", it) }
            prefs[TASKBAR_CORNER_RADIUS_KEY]?.let { put("taskbar_corner_radius_dp", it) }
            prefs[TASKBAR_DOCK_PADDING_KEY]?.let { put("taskbar_dock_padding", it) }
            prefs[SURFACE_TINT_COLOR_KEY]?.let { put("surface_tint_color", it) }
            prefs[AUTO_HIDE_FULLSCREEN_KEY]?.let { put("auto_hide_fullscreen", it) }
            prefs[AUTO_HIDE_LANDSCAPE_KEY]?.let { put("auto_hide_landscape", it) }
            prefs[QUICK_CONTROLS_ENABLED_KEY]?.let { put("quick_controls_enabled", it) }
            prefs[CONTROLS_ORDER_KEY]?.let { put("controls_order", it) }
            prefs[CONTROLS_DISABLED_KEY]?.let { put("controls_disabled_ids", it) }
            prefs[MUSIC_PANEL_ENABLED_KEY]?.let { put("music_panel_enabled", it) }
            prefs[HAPTIC_FEEDBACK_KEY]?.let { put("haptic_feedback", it) }
            prefs[PANEL_OUTLINE_KEY]?.let { put("panel_outline_enabled", it) }
            prefs[APP_GRID_COLUMNS_KEY]?.let { put("app_grid_columns", it) }
            prefs[APP_GRID_ROWS_KEY]?.let { put("app_grid_rows", it) }
            prefs[PILL_TRIGGER_AREA_KEY]?.let { put("pill_trigger_area", it) }
            prefs[PILL_RESTRICT_TRIGGER_KEY]?.let { put("pill_restrict_trigger", it) }
        }.toString()
    }

    suspend fun importFromJson(json: String) {
        val obj = JSONObject(json)
        context.dataStore.edit { prefs ->
            if (obj.has("pinned_apps")) prefs[PINNED_APPS_KEY] = obj.getString("pinned_apps")
            if (obj.has("theme_mode")) prefs[THEME_MODE_KEY] = obj.getString("theme_mode")
            if (obj.has("overlay_enabled")) prefs[OVERLAY_ENABLED_KEY] = obj.getBoolean("overlay_enabled")
            if (obj.has("pill_swipe_up_action")) prefs[PILL_SWIPE_UP_ACTION_KEY] = obj.getString("pill_swipe_up_action")
            if (obj.has("pill_swipe_down_action")) prefs[PILL_SWIPE_DOWN_ACTION_KEY] = obj.getString("pill_swipe_down_action")
            if (obj.has("pill_double_tap_action")) prefs[PILL_DOUBLE_TAP_ACTION_KEY] = obj.getString("pill_double_tap_action")
            if (obj.has("pill_width")) prefs[PILL_WIDTH_KEY] = obj.getDouble("pill_width").toFloat()
            if (obj.has("pill_height")) prefs[PILL_HEIGHT_KEY] = obj.getDouble("pill_height").toFloat()
            if (obj.has("pill_alpha")) prefs[PILL_ALPHA_KEY] = obj.getDouble("pill_alpha").toFloat()
            if (obj.has("pill_position_y")) prefs[PILL_POSITION_Y_KEY] = obj.getDouble("pill_position_y").toFloat()
            if (obj.has("pill_position_x_pct")) prefs[PILL_POSITION_X_PCT_KEY] = obj.getDouble("pill_position_x_pct").toFloat()
            if (obj.has("pill_edge_position")) prefs[PILL_EDGE_POSITION_KEY] = obj.getString("pill_edge_position")
            if (obj.has("pill_side_position_pct")) prefs[PILL_SIDE_POSITION_PCT_KEY] = obj.getDouble("pill_side_position_pct").toFloat()
            if (obj.has("taskbar_position_y")) prefs[TASKBAR_POSITION_Y_KEY] = obj.getDouble("taskbar_position_y").toFloat()
            if (obj.has("taskbar_height_dp")) prefs[TASKBAR_HEIGHT_KEY] = obj.getDouble("taskbar_height_dp").toFloat()
            if (obj.has("taskbar_control_labels")) prefs[TASKBAR_CONTROL_LABELS_KEY] = obj.getBoolean("taskbar_control_labels")
            if (obj.has("pinned_icon_size_dp")) prefs[PINNED_ICON_SIZE_KEY] = obj.getDouble("pinned_icon_size_dp").toFloat()
            if (obj.has("quick_control_size_dp")) prefs[QUICK_CONTROL_SIZE_KEY] = obj.getDouble("quick_control_size_dp").toFloat()
            if (obj.has("taskbar_corner_radius_dp")) prefs[TASKBAR_CORNER_RADIUS_KEY] = obj.getDouble("taskbar_corner_radius_dp").toFloat()
            if (obj.has("taskbar_dock_padding")) prefs[TASKBAR_DOCK_PADDING_KEY] = obj.getString("taskbar_dock_padding")
            if (obj.has("surface_tint_color")) prefs[SURFACE_TINT_COLOR_KEY] = obj.getString("surface_tint_color")
            if (obj.has("auto_hide_fullscreen")) prefs[AUTO_HIDE_FULLSCREEN_KEY] = obj.getBoolean("auto_hide_fullscreen")
            if (obj.has("auto_hide_landscape")) prefs[AUTO_HIDE_LANDSCAPE_KEY] = obj.getBoolean("auto_hide_landscape")
            if (obj.has("quick_controls_enabled")) prefs[QUICK_CONTROLS_ENABLED_KEY] = obj.getBoolean("quick_controls_enabled")
            if (obj.has("controls_order")) prefs[CONTROLS_ORDER_KEY] = obj.getString("controls_order")
            if (obj.has("controls_disabled_ids")) prefs[CONTROLS_DISABLED_KEY] = obj.getString("controls_disabled_ids")
            if (obj.has("music_panel_enabled")) prefs[MUSIC_PANEL_ENABLED_KEY] = obj.getBoolean("music_panel_enabled")
            if (obj.has("haptic_feedback")) prefs[HAPTIC_FEEDBACK_KEY] = obj.getBoolean("haptic_feedback")
            if (obj.has("panel_outline_enabled")) prefs[PANEL_OUTLINE_KEY] = obj.getBoolean("panel_outline_enabled")
            if (obj.has("app_grid_columns")) prefs[APP_GRID_COLUMNS_KEY] = obj.getInt("app_grid_columns")
            if (obj.has("app_grid_rows")) prefs[APP_GRID_ROWS_KEY] = obj.getInt("app_grid_rows")
            if (obj.has("pill_trigger_area")) prefs[PILL_TRIGGER_AREA_KEY] = obj.getDouble("pill_trigger_area").toFloat()
            if (obj.has("pill_restrict_trigger")) prefs[PILL_RESTRICT_TRIGGER_KEY] = obj.getBoolean("pill_restrict_trigger")
        }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { prefs ->
            val onboarding = prefs[ONBOARDING_COMPLETE_KEY]
            prefs.clear()
            if (onboarding == true) prefs[ONBOARDING_COMPLETE_KEY] = true
        }
    }

}
