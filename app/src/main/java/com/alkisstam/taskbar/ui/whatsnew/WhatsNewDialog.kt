package com.alkisstam.taskbar.ui.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

data class WhatsNewRelease(
    val versionName: String,
    val versionCode: Int,
    val highlights: List<String>
)

val whatsNewReleases = listOf(
    WhatsNewRelease(
        versionName = "1.3.5",
        versionCode = 38,
        highlights = listOf(
            "New Clipboard / Notes panel — capture text, links, images, PDFs, and Office docs from any app's share sheet",
            "Clips, Favorites, and Notes tabs with copy, share, pin, and favorite actions",
            "Category filter (Text/Images/Files/Links) and a Clipboard quick control tile",
            "Transparency slider for translucent panels (30–100% opacity)"
        )
    ),
    WhatsNewRelease(
        versionName = "1.3.6",
        versionCode = 39,
        highlights = listOf(
            "Clipboard panel now floats above the dock instead of hiding it",
            "Bottom pill: choose Double Tap or Swipe Up to activate, plus a position slider",
            "Restrict the pill's trigger area to just the pill instead of the full screen edge",
            "Fixed the pill height slider getting stuck around 18dp"
        )
    ),
    WhatsNewRelease(
        versionName = "1.3.7",
        versionCode = 40,
        highlights = listOf(
            "New Wifi, Bluetooth, and Quick Share quick controls",
            "Design tab reordered — Theme now appears first",
            "Every slider in the app now uses the filled-pill style from the Volume/Brightness panel",
            "Caffeine quick control shows a countdown badge on its icon when labels are off"
        )
    ),
    WhatsNewRelease(
        versionName = "1.3.8",
        versionCode = 41,
        highlights = listOf(
            "Fixed crashes when opening or sharing saved clips",
            "App drawer and quick-control tiles keep refreshing after toggling the dock off and on",
            "Backup and restore no longer risk freezing the UI on cloud storage",
            "Broad crash hardening across service starts and system launches"
        )
    ),
    WhatsNewRelease(
        versionName = "1.3.9",
        versionCode = 42,
        highlights = listOf(
            "New Dock Corner Radius slider in Design → Dock Size & Appearance",
            "Dock edge-padding presets (Default / Small / Large) so the dock clears rounded display corners",
            "Fixed an app-loading crash on some devices caused by a system icon error",
            "The dock no longer pops up on its own just from opening the app"
        )
    ),
    WhatsNewRelease(
        versionName = "1.3.10",
        versionCode = 43,
        highlights = listOf(
            "Fixed sharing to the clipboard crashing the app and turning off the accessibility service",
            "Apps whose icon fails to load now show with a placeholder instead of disappearing from the all-apps menu",
            "Left/right trigger pill now stays on-screen after rotating to landscape"
        )
    ),
    WhatsNewRelease(
        versionName = "1.4.1",
        versionCode = 45,
        highlights = listOf(
            "Fixed the Media volume slider not changing volume on Oppo, OnePlus and Realme phones",
            "Reordering pinned apps has moved to Settings → Apps → Pinned Apps",
            "New Search settings — toggle Fuzzy Search and Show Recent Apps in General settings",
            "Music Panel now hides while the Calculator is open, and reappears after",
            "Tapping the album art now also hides the dock and Music Panel",
            "Long notes in the Clipboard panel scroll instead of getting cut off",
            "Tapping Music with nothing playing now shows a \"No Media Playing\" message instead of doing nothing",
            "New To-Dos tab in the Clipboard panel — check items off and they move to a Completed section"
        )
    )
)

@Composable
fun WhatsNewDialog(releases: List<WhatsNewRelease>, onDismiss: () -> Unit) {
    val title = if (releases.size == 1) "What's New in ${releases.first().versionName}" else "What's New"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                releases.sortedByDescending { it.versionCode }.forEach { release ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (releases.size > 1) {
                            Text(release.versionName, style = MaterialTheme.typography.titleSmall)
                        }
                        release.highlights.forEach { line ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                                Text(line, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    )
}
