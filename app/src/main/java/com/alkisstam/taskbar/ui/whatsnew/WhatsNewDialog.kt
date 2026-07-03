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
