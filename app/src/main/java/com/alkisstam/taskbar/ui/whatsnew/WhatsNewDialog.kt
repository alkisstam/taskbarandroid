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
        versionName = "1.3.6",
        versionCode = 39,
        highlights = listOf(
            "Clipboard panel now floats above the dock instead of hiding it",
            "Bottom pill: choose Double Tap or Swipe Up to activate, plus a position slider",
            "Restrict the pill's trigger area to just the pill instead of the full screen edge",
            "Fixed the pill height slider getting stuck around 18dp"
        )
    )
)

@Composable
fun WhatsNewDialog(release: WhatsNewRelease, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's New in ${release.versionName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                release.highlights.forEach { line ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•  ", style = MaterialTheme.typography.bodyMedium)
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    )
}
