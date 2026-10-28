package com.alkisstam.taskbar.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesRepositoryTest {

    @Test
    fun `pinned apps JSON round-trip`() {
        val pkgs = listOf("com.a", "com.b", "com.c")
        assertEquals(pkgs, PreferencesRepository.deserializePinnedApps(PreferencesRepository.serializePinnedApps(pkgs)))
    }

    @Test
    fun `pinned apps legacy delimiter format migrates`() {
        assertEquals(listOf("com.a", "com.b"), PreferencesRepository.deserializePinnedApps("com.a||com.b"))
    }

    @Test
    fun `string list round-trip`() {
        val ids = listOf("torch", "ringer", "share")
        assertEquals(ids, PreferencesRepository.deserializeStringList(PreferencesRepository.serializeStringList(ids)))
    }

    @Test
    fun `corrupt string list yields empty`() {
        assertEquals(emptyList<String>(), PreferencesRepository.deserializeStringList("garbage"))
    }

    @Test
    fun `app groups JSON round-trip`() {
        val groups = listOf(
            AppGroup("g1", "Social", "social", listOf("com.a", "com.b")),
            AppGroup("g2", "Empty", "folder", emptyList())
        )
        assertEquals(groups, PreferencesRepository.deserializeAppGroups(PreferencesRepository.serializeAppGroups(groups)))
    }

    @Test
    fun `corrupt app groups yields empty`() {
        assertEquals(emptyList<AppGroup>(), PreferencesRepository.deserializeAppGroups("garbage"))
    }

    private fun fullBackup() = JSONObject().apply {
        put("pinned_apps", "[\"com.a\",\"com.b\"]")
        put("theme_mode", "DARK")
        put("overlay_enabled", true)
        put("pill_swipe_up_action", "SHOW_DOCK")
        put("pill_swipe_down_action", "POWER_MENU")
        put("pill_double_tap_action", "DISABLED")
        put("pill_hold_drag_action", "VOLUME")
        put("pill_width", 12.5)
        put("pill_height", 80.5)
        put("pill_alpha", 0.75)
        put("pill_position_y", 6.5)
        put("pill_position_x_pct", 30.5)
        put("pill_edge_position", "BOTH")
        put("pill_side_position_pct", 40.5)
        put("taskbar_position_y", 10.5)
        put("taskbar_height_dp", 64.5)
        put("taskbar_control_labels", true)
        put("taskbar_keep_expanded", true)
        put("pinned_icon_size_dp", 44.5)
        put("pinned_icon_padding_dp", 6.5)
        put("quick_control_size_dp", 40.5)
        put("taskbar_corner_radius_dp", 20.5)
        put("taskbar_dock_padding", "LARGE")
        put("taskbar_app_menu_side", "RIGHT")
        put("taskbar_icon_shape", "SQUIRCLE")
        put("hidden_apps", "[\"com.hidden\"]")
        put("surface_tint_color", "4282664004")
        put("auto_hide_fullscreen", true)
        put("auto_hide_landscape", true)
        put("dock_always_visible", true)
        put("pin_dock_control_enabled", true)
        put("disable_on_lockscreen", true)
        put("quick_controls_enabled", true)
        put("quick_settings_panel_enabled", true)
        put("controls_order", "[\"torch\",\"dnd\"]")
        put("controls_disabled_ids", "[\"qr\"]")
        put("music_panel_enabled", true)
        put("always_show_music_panel", true)
        put("haptic_feedback", true)
        put("panel_outline_enabled", true)
        put("app_grid_columns", 5)
        put("app_grid_icon_size", 64.0)
        put("pill_trigger_area", 24.5)
        put("pill_restrict_trigger", true)
        put("fuzzy_search_enabled", true)
        put("show_recent_apps", true)
        put("show_recent_apps_row", true)
        put("translucent_mode", true)
        put("translucent_alpha", 0.5)
        put("grain_alpha", 0.125)
        put("blur_radius_dp", 24.5)
        put("blur_tint", true)
        put("icons_pill_auto_hide", true)
        put("blur_tint_alpha", 0.25)
        put("icon_pack_package", "com.iconpack")
        put("app_language_tag", "el")
        put("app_sort_order", "USAGE")
        put("app_groups", PreferencesRepository.serializeAppGroups(listOf(AppGroup("g1", "Tools", "tools", listOf("com.a")))))
    }

    private fun assertSameJson(expected: JSONObject, actual: JSONObject) {
        assertEquals(expected.keys().asSequence().toSet(), actual.keys().asSequence().toSet())
        expected.keys().forEach { key ->
            assertEquals(key, expected.get(key).toString(), actual.get(key).toString())
        }
    }

    @Test
    fun `backup round-trip restores every setting`() {
        val backup = fullBackup()
        val prefs = mutablePreferencesOf()
        PreferencesRepository.importPrefs(prefs, backup)
        assertSameJson(backup, PreferencesRepository.exportPrefs(prefs))
    }

    @Test
    fun `partial backup only overwrites present settings`() {
        val prefs = mutablePreferencesOf()
        PreferencesRepository.importPrefs(prefs, fullBackup())
        PreferencesRepository.importPrefs(prefs, JSONObject().put("theme_mode", "LIGHT").put("app_grid_columns", 3))
        val expected = fullBackup().put("theme_mode", "LIGHT").put("app_grid_columns", 3)
        assertSameJson(expected, PreferencesRepository.exportPrefs(prefs))
    }

    @Test
    fun `empty prefs export empty backup`() {
        assertEquals(0, PreferencesRepository.exportPrefs(mutablePreferencesOf()).length())
    }
}
