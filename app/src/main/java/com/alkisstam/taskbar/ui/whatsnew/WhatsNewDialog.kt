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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.R

data class WhatsNewRelease(
    val versionName: String,
    val versionCode: Int,
    val highlights: List<String>
)

val whatsNewReleases = listOf(
    WhatsNewRelease(
        versionName = "1.5.12",
        versionCode = 67,
        highlights = listOf(
            "App panels — add up to 3 named rows of apps with their own icon in Settings → Apps, and open them from the pill above the dock",
            "Pin apps with the new + button in Settings → Apps",
            "Quick Controls, your panels, Music and Pin Dock now live in a pill above the dock, which can auto-hide until you swipe up on the dock",
            "Floating windows — turn on in Settings → General, then long-press any app and choose Open in floating window. Devices without built-in support need Shizuku",
            "New Swipe In action for side pills, which no longer trigger Back",
            "Every pill gesture gets an action dropdown, with new Show App Menu and Search Apps actions",
            "Fixed the dock and pill disappearing in Instagram, Camera, Maps and similar apps"
        )
    ),
    WhatsNewRelease(
        versionName = "1.5.13",
        versionCode = 68,
        highlights = listOf(
            "The All Apps menu is now fullscreen, with search at the bottom next to the sort menu",
            "New Icon Size setting for the All Apps menu in Settings → Apps",
            "App panels and Quick Controls close from their own icon, on the App Menu button's side",
            "Tapping outside closes the dock and the open panel together, and that panel reopens next time",
            "New Swipe Sensitivity setting for the pill, and pill swipes are more reliable on Pixel and similar phones",
            "Restrict Trigger to Pill is back for side pills, which now only block Back when a Swipe In action is set",
            "Auto-hide in Fullscreen now works and hides the pill too",
            "Fixed the pill disappearing after a few seconds on some devices, and the pill's App Menu and Search actions bringing up the dock"
        )
    ),
    WhatsNewRelease(
        versionName = "1.5.14",
        versionCode = 69,
        highlights = listOf(
            "Hide the App Menu button from the dock, or hide the dock's time, date and battery line",
            "Pinned app icons and Quick Controls go up to 120dp, and the dock grows taller to fit them",
            "The custom tint now uses a palette color picker",
            "Better layouts on tablets and in landscape for the All Apps menu, panels and calculator",
            "Fixed a crash on launch on some Android 14 devices and freezes while music plays",
            "Crash reports no longer include other apps' package names or on-screen text",
            "Fixed the dock staying hidden after unlocking, or turning see-through after battery saver"
        )
    ),
    WhatsNewRelease(
        versionName = "1.5.15",
        versionCode = 70,
        highlights = listOf(
            "Fixed the dock crashing on Android 8, 9 and 10",
            "Notification history is only saved while its control is turned on",
            "Notes, to-dos and clipboard items are now included in your phone's backup",
            "Deleting a shared text file from the clipboard now frees its storage",
            "Shared images keep their original format instead of being saved as JPG",
            "The clipboard and notes panels reopen after you view an image or file",
            "The dock and its panels shrink to fit their icons, with an option to center the icons pill",
            "The Quick Settings tile opens the app when the overlay permission is missing"
        )
    ),
    WhatsNewRelease(
        versionName = "1.5.16",
        versionCode = 71,
        highlights = listOf(
            "New About section at the end of General, with the app version, What's New, the Privacy Policy and the Terms of Service",
            "Turning off notification history now deletes the stored notifications",
            "Fixed the calculator giving 0 when continuing from a negative result",
            "Stopping the dock while Caffeine is on puts your normal screen timeout back right away",
            "The clipboard panel no longer slows down or crashes with large text files",
            "Clips whose file didn't come back with a backup are hidden instead of showing blank",
            "Sharing to the clipboard only accepts files from other apps",
            "Crash reports no longer include the window names of other apps"
        )
    ),
    WhatsNewRelease(
        versionName = "1.5.17",
        versionCode = 72,
        highlights = listOf(
            "New icon shapes: Cookie, Clover and Pebble, and a smoother Squircle",
            "Dock and app grid icons change shape while you press them",
            "The All Apps button turns into a cookie when the App Menu opens",
            "Long-press menus open above the icon instead of covering it",
            "Settings menus open centered, fit their text and show an icon for each option",
            "Panels open closer to the dock, and the music panel steps aside while another panel is open",
            "Fixed a crash when opening search or a note"
        )
    ),
    WhatsNewRelease(
        versionName = "1.5.19",
        versionCode = 74,
        highlights = listOf(
            "New icon shapes: Arrow, Pixel Circle and Sunny",
            "Cookie and Clover now use the official Material 3 shapes",
            "Up to 5 app panels, edited in a pop-up and previewed as they look on the dock",
            "Settings choices are now button groups and split buttons",
            "Panel buttons above the dock shrink to fit"
        )
    )
)

@Composable
fun WhatsNewDialog(releases: List<WhatsNewRelease>, onDismiss: () -> Unit) {
    val title = if (releases.size == 1) {
        stringResource(R.string.whats_new_dialog_title_single, releases.first().versionName)
    } else {
        stringResource(R.string.whats_new_dialog_title)
    }
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.whats_new_dialog_dismiss)) }
        }
    )
}
