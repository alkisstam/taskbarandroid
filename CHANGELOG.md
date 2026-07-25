# Changelog

All notable changes to Floating Dock are documented here.

---

## [1.5.3] - 2026-07-25

Stability release.

### Fixed
- **Native crash while loading app icons** — concurrent app-list reloads could render the same vector icon from two threads at once, crashing the process in the system renderer. Reloads are now serialized and each icon gets its own drawable copy.
- **Freeze/kill from icon cleanup pressure** — the same overlapping reloads doubled native drawable churn, which could stall finalization long enough for the system to kill the app. Fixed by the same reload serialization.

---

## [1.5.2] - 2026-07-24

Keep controls expanded setting.

### Added
- **Keep controls expanded** — Settings → Controls → Quick Controls → Keep controls expanded. When on, the dock opens with quick controls already showing instead of collapsed; swipe up/down still expands/collapses it manually.
- **Always Show Panel** — Settings → General → Music Panel → Always Show Panel. When on, tapping the Music quick control opens the panel even with no active media session, instead of showing "No Media Playing".

### Changed
- **Target API level raised to Android 16 (API 36)** — compileSdk/targetSdk bumped from 35 to 36 per Play Console requirement.

### Fixed
- **Music panel getting stuck half-open and faded after swiping up the dock** — the panel now follows the swipe smoothly and springs fully open when the finger lifts, instead of freezing at the last drag position.
- **Background blur lingering for a moment after closing the Clipboard, Notes, or Notification panels** — the blur now disappears together with the panel.

---

## [1.5.1] - 2026-07-20

Icon shape setting.

### Added
- **Icon Shape** — Settings → Design → Dock Size & Appearance → Icon Shape lets you choose Default, Square, Squircle, or Circle for app icons across the dock, app menu, search, recent apps, notifications, and the pinned/hidden app pickers.

### Fixed
- **Rare native crash loading app icons under memory pressure** — the low-memory check now runs before every icon decode instead of every 8th, closing a gap where the process could still be killed by the OS before the check caught up.

---

## [1.5.0] - 2026-07-18

Icon packs and a dedicated Notes panel.

### Added
- **Icon pack support** — Settings → Design → Dock Size & Appearance → Icon Pack lists installed icon packs (ADW/Nova/GO format) and applies the selected pack to app icons across the dock, app menu, search, and pickers. Apps missing from the pack keep their default icon; icons re-theme live without a restart, and the selection is included in backup/restore.
- **Notes panel** — Notes and To-Dos moved out of the Clipboard panel into their own full-screen panel with the same design, opened by a new "Notes" quick control in Controls. The Clipboard panel now has just the Clips and Favorites tabs.
- **Permissions overview** — Settings → General → Permissions now lists every permission the app uses (required and optional) with its granted status; tap a row to open the matching grant screen.

### Changed
- **Favorites tab shows clips only** — notes no longer appear in the Clipboard panel's Favorites tab, and the favorite star was removed from note cards (pinning notes remains).
- **Notes and Clipboard tab bars redesigned** — the selected tab now shows its icon and label side by side in a pill, matching the Settings tab bar.
- **Refreshed default settings** — new installs start with tuned defaults: theme transparency 90% and grain 8%; taller slimmer side pill (4×90dp, 30% opacity, 16dp trigger area, restricted to the pill); bottom pill preset 140×12dp at 2dp from the bottom edge; dock with 42dp icons, 8dp icon spacing, 44dp quick controls, and 22dp corners; 4×5 app grid; vibrate feedback and fuzzy search off. Existing installs keep their saved settings.

### Fixed
- **Dock now follows the system theme while running** — with the System theme selected, switching the device between light and dark mode updates the dock and panels immediately; previously they kept the old colors until restarted. Color presets adapt too: each preset swaps to its counterpart shade (e.g. Sky ↔ Slate) when the device theme flips. Custom colors and fixed Light/Dark themes are unaffected.
- **Crash loading app icons when the device is low on memory** — icon loading now pauses while the system reports low memory (apps briefly show a placeholder icon) and fills the icons in once memory recovers, instead of the dock being killed mid-load.

---

## [1.4.4] - 2026-07-15

Quick actions for notifications, easier app hiding, in-app updates and reviews, and a lock screen option.

### Added
- **Notification quick actions** — notifications in the Notification History panel now show their action buttons (Mark as read, Archive, Like…) where the app provides them; tap to trigger the action without opening the app. Actions that need typed input (inline reply) aren't shown. Available for notifications received since the dock last started.
- **Add App button in Hidden Apps** — Settings → Apps → Hidden Apps now always shows, with a + button that opens a searchable list of installed apps; tap an app to hide it from the App Menu and search.
- **In-app updates** — when a new version is available on Google Play, the app downloads it in the background and shows a restart prompt, no Play Store visit needed.
- **In-app review prompt** — after a few opens, the app may ask for a Play Store rating without leaving the app.
- **Disable on Lock Screen** — new toggle in Settings → General. When on, the dock and its trigger stay completely off while the lock screen is showing (previously the hidden trigger could still open the dock there), and come back as soon as you unlock.

### Fixed
- **New note/to-do card now opens right above the keyboard** — it previously jumped to the top of the screen when the keyboard opened. The composer card also gained a subtle outline.
- **Dock now sits above the navigation bar** — with 3-button navigation the dock and its floating panels no longer overlap the nav bar.
- **App menu no longer gets stuck open over Settings** — tapping outside the menu now always closes it, even while the Settings screen is open.
- **Crash loading app icons on Xiaomi (MIUI) devices** — icons now load directly from each app's resources, bypassing the MIUI icon theming hook that could kill the app under memory pressure. Trade-off: MIUI theme icon masks no longer apply to dock icons.

---

## [1.4.3] - 2026-07-09

Design update — the Theme section in Design has been redesigned — plus two new quick controls.

### Added
- **Notification History panel** — a new "Notifs" quick control opens a panel listing recent notifications (up to 100): app icon and name, title, message, and relative time. Notifications from the same app are grouped under an expandable arrow when there's more than one. Tap an entry to open the app, swipe left or right to remove it, or Clear all. Uses the Notification Access permission already granted for the Music Panel; ongoing, media, and silent housekeeping notifications are filtered out. History starts from when this version is installed.
- **Telegram button in General settings** — a circular Telegram button (t.me/floatingdock) now sits next to Contact & Feedback.
- **Pinned Icon Padding slider** — Design → Dock Size & Appearance now has a slider (2-12dp) controlling the gap between pinned app icons independently of icon size.
- **Mobile Data quick control** — shows whether mobile data is on; tapping opens the system Internet panel to toggle Wi-Fi or mobile data (Android 10+; older versions open network settings).
- **Light and Dark color preset tabs** — surface tint presets are now grouped into Light and Dark tabs, each a 3×3 grid of swatch cards with a check badge on the active one. Two new presets per side: Lavender and Pearl (light), Forest and Espresso (dark).
- **Auto preset** — the first cell in each tab. It follows your theme: switch between Light, Dark, or System and the dock and panels adapt automatically. Picking a Light/Dark/System theme mode also resets a mismatched preset (e.g. a dark preset while switching to Light) back to Auto; custom colors are always kept.
- **Theme Style** — a new button under the color presets opens a Solid / Transparent chooser, replacing the old "Translucent panels" toggle. Solid shows the Panel Outline toggle; Transparent shows the Transparency and Grain sliders.
- **Grain slider** — control the film-grain texture on transparent surfaces from 0% (off) to 30%; previously fixed at 10%. Grain now also covers the Brightness panel and the search bar, which were missing it.
- **Frosted backdrop for Clipboard and Search** — with Transparent style on Android 12+, the screen behind the Clipboard panel and the app search blurs while they're open (respects the system's cross-window blur setting; falls back to the regular translucent look where unavailable).
- **Dropdown pickers in Design** — Theme Style, Pill Position, and Dock Edge Padding now open as a dropdown menu from a single gradient pill, replacing the old popup dialog and button rows.
- **Segmented Light/Dark/System toggle** — the three theme buttons are now one connected pill with the active option highlighted.
- **Recent apps row in the App Menu** — new "Show Recent Apps in All Apps Panel" toggle in Settings → Apps adds a row of recently-opened apps under the search bar, separated by a divider and aligned to the App Grid's column count.
- **All Apps preview resized** — the All Apps list in Settings → Apps now shows a 4-row view instead of a taller fixed height.
- **Telegram button in General settings** — a circular Telegram button (t.me/floatingdock) now sits next to Contact & Feedback.
- **Pinned Icon Padding slider** — Design → Dock Size & Appearance now has a slider (2-12dp) controlling the gap between pinned app icons independently of icon size.
- **Notification History grouping and swipe-to-dismiss** — notifications from the same app are grouped under an expandable arrow when there's more than one; swipe a notification left or right to remove it.
- **All Apps button side** — Design → Dock Size & Appearance now has a Left/Right dropdown for where the all-apps button sits in the dock row.
- **Pill position along the bottom edge** — when the trigger pill is set to the Bottom edge position, a new "Position along edge" slider moves it left-to-right instead of always centering it.
- **Hide App** — long-press an app in the App Menu for a new "Hide App" option; hidden apps disappear from the app grid and search, and can be unhidden from a new "Hidden Apps" list in Settings → Apps.
- **Scrollable clip text cards** — clip and note cards in the Clipboard panel and Clips tab now scroll internally instead of truncating when the text is longer than the card.
- **To-do overflow menu** — each to-do now has a three-dot menu (Edit, Pin, Delete) instead of separate icon buttons; pinned to-dos sort to the top of the list.

### Fixed
- **Expanded calculator no longer extends past the top of the screen** — buttons had grown with screen width, so revealing the scientific rows pushed the panel off-screen; keys now use a compact fixed height.
- **Clipboard frosted backdrop no longer blurs the dock** — the blur now sits behind the dock and pill, so they stay crisp while the screen behind the Clipboard panel frosts. The blur layer is also fully released while idle instead of staying on screen invisibly.
- **Open panels no longer vanish when the Accessibility Service toggles** — turning the Accessibility Service on or off while the Clipboard, Volume, Brightness, or Calculator panel was open made it disappear until reopened, and could hide the Music Panel permanently. All panels now come back exactly as they were.
- **Stray icon glyph in Notification History text** — some notifications (weather, sports scores) embed inline icons that left behind a leftover placeholder character rendered as a stray icon over the title/text; it's now stripped.
- **Removed the overlapping delete icon in Notification History** — a delete icon was rendering on top of the notification text at rest instead of only during a swipe; swiping still shows a red background as feedback.
- Silenced an `OnBackInvokedCallback` warning surfaced in Firebase Test Lab logs by enabling predictive back in the manifest.

---

## [1.4.2] - 2026-07-08

Stability release — a full crash/ANR audit of the app, with every finding fixed.

### Fixed
- **Permission buttons crashed on some devices** — tapping a "Grant Permission" button in onboarding or Settings crashed the app on devices missing the corresponding system settings screen (seen with battery optimization and write-settings on some OEM builds). All permission requests now fail gracefully with a message instead.
- **Ghost dock after unlocking** — stopping the dock right after unlocking the screen could leave untouchable dock windows stuck on screen until the app was force-closed.
- **Music Panel could crash when the playing app closed** — reading the media session at the exact moment the music app died could crash the whole dock; it now just clears the panel. Also fixed a race in the session-retry logic that could leave stale playback state.
- **"Permission not granted" loops on some OEMs** — Notification Access and Accessibility Service could show as not granted even after granting, because some devices store the setting in a shortened format. Affected onboarding, the Music Panel settings card, and the media listener itself.
- **Caffeine could permanently change your screen timeout** — if the system killed the app while Caffeine was active, your original screen timeout was never restored. It's now saved and restored on the next start.
- **Clipboard panel stuttered while scrolling** — image thumbnails and file previews were loaded on the UI thread; they now load in the background.
- **Backup didn't include Translucent Mode and Transparency** — both are now exported and restored.
- **Drag-to-reorder could snap back** — reordering pinned apps or active controls quickly could occasionally save a stale order; the order is now saved once, when you drop.
- **Dock stopped dismissing on home press after switching launchers** — the list of launcher apps was only read once; it now refreshes periodically.

### Changed
- **Battery optimization step now opens the system list** — instead of the direct request dialog (restricted by Play policy), onboarding opens the battery optimization list; find Floating Dock and choose "Don't optimize".
- **Shared files are capped at 50 MB and shared text at 100,000 characters** — prevents a mislabeled huge share from silently filling storage.
- **Lower memory use** — app icons are now stored at a fixed size instead of whatever size the system provides (some themed icons were 1024px+).
- **Reset Settings no longer shows the dock as stopped while it's still running.**

---

## [1.4.1] - 2026-07-07

### Added
- **Calculator panel** — a new quick control opens a floating calculator above the dock. Shows a running expression with a live preview total; swipe up on the handle to reveal scientific functions (sin, cos, tan, ln, log, ^, π). Follows the same auto-hide rules as the other panels (dismisses on Wifi/Bluetooth/Share, mutually exclusive with Volume/Brightness/Music/Clipboard, tap-outside to dismiss).
- **Edit clips and notes in the Clipboard panel** — text and link clips, and notes, can now be edited in place instead of only copied, shared, or deleted.
- **Music Panel matches the dock's design settings** — the floating Music Panel now follows the Corner Radius and Edge Padding settings from Design instead of using fixed values; tapping the album art opens the currently playing app.
- **Wifi, Bluetooth, and Share now auto-hide the dock** — tapping these quick controls hides the dock and Music Panel, matching the existing behavior for QR, Power, Screenshot, and Lock Screen.
- **New Search section in General settings** — toggle Fuzzy Search on/off, and toggle Show Recent Apps to see your 5 most recently opened apps when you tap the search bar with nothing typed.
- **New To-Dos tab in the Clipboard panel** — add to-do items with the same "+" composer as Notes; checking an item off moves it into a collapsed "Completed (N)" section below the open ones.
- **Haptic feedback on slider drags** — dragging any slider (Corner Radius, Transparency, Pill/Dock sizing, App Grid, Volume, Brightness) now gives a tick per step when Vibrate Feedback is on; previously only long-press and drag-reorder triggered it.

### Changed
- **Reordering pinned apps moved to Settings** — drag-to-reorder on the dock was replaced by the existing drag-to-reorder in Settings → Apps → Pinned Apps.
- **Music Panel now hides while the Calculator panel is open** and reappears once it's closed, matching how it already behaves around Volume and Brightness.
- **Tapping the album art** now also hides the dock and Music Panel (it already opened the now-playing app), consistent with every other "launch and get out of the way" action.
- **Long notes in the Clipboard panel scroll instead of getting cut off** — the note card stays the same size, but you can now scroll within it to read the rest.
- **Surface Tint Color now also applies to Search, Volume/Brightness, and Calculator panels** — previously only the Dock, App Menu, and Clipboard panel picked up the tint.
- **New note/to-do composer docks above the keyboard** — starting a new note or to-do now opens the keyboard automatically and shows the composer just above it, instead of an inline card at the top of the list you had to scroll to and tap into. Saving still puts the new item at the top of the list as before.
- **Onboarding: Accessibility Service moved to optional permissions** — the dock works without it; it's only needed to draw the pill above the system navigation bar. A confirmation dialog still explains what it's for before requesting it. Added an info dialog when disabling battery optimization (some devices still throttle background activity and may need manual exclusion) and when selecting the bottom pill position (needs Accessibility Service to sit above the nav bar).

### Fixed
- **Media volume slider did nothing on Oppo, OnePlus and Realme phones** — on ColorOS-based ROMs the system blocks apps from setting the media stream volume directly, so the Media slider moved but the volume never changed (the Ring, Notification and Alarm sliders were unaffected). The slider now adjusts media volume the same way the hardware volume keys do, which these devices allow.
- **Tapping "Music" with nothing playing silently did nothing** — it now shows a "No Media Playing" toast instead of opening an empty panel.
- **Music Panel sometimes never appeared even with media playing and notification access granted** (seen on Samsung One UI) — the app could fail to detect the current media session right after the listener connects and never retry. It now retries with backoff and asks the system to reconnect the listener if needed.
- **Clipboard panel's tab labels wrapped to two lines** once a 4th tab (To-Dos) was added — the selected tab's icon and label now stack vertically instead of side by side so all four tabs fit on one line; the pill is also now a fixed size across all four tabs instead of resizing with the label.
- **The app menu couldn't be dismissed by tapping its icon again or tapping outside it** — only the back gesture closed it. The dock's own window stayed touchable after the menu opened, and the Volume/Brightness/Calculator panel windows stayed touchable even while hidden — both silently absorbed the tap meant to dismiss the menu.

---

## [1.3.10] - 2026-07-04

### Fixed
- **Sharing to Floating Dock crashed the app and stopped the accessibility service** — sharing anything (text, link, image, document) to the dock's clipboard crashed the whole app, which also turned off the accessibility service until it was re-enabled manually. The share now saves silently as intended.
- **Not all installed apps appeared in the all-apps menu** — on some devices an app whose icon failed to load was dropped from the list entirely; such apps now show with a placeholder icon instead of going missing.
- **Side trigger pill went off-screen in landscape** — a left/right pill positioned by percentage (e.g. 50%) stayed at its portrait position after rotating, pushing it off or partly off the screen; the pill now repositions correctly when the orientation changes.

---

## [1.3.9] - 2026-07-04

### Added
- **Dock corner radius** — a new slider in Design → Dock Size & Appearance adjusts how rounded the dock's corners are (0–32dp).
- **Dock edge padding** — a Default / Small / Large preset insets the dock from the screen edges so its corners clear the rounded display corners on phones like the Pixel 10; the floating panels move up with it.

### Fixed
- **Crash while loading apps on some devices** — an out-of-memory error thrown by the system while rendering an app icon (seen on certain MIUI builds) no longer crashes the app-list load; the offending app is skipped instead.
- **Dock appeared on its own when opening the app** — the dock no longer pops up and persists just from launching Floating Dock; it stays hidden until you trigger it.
- **Dock wouldn't hide while the app was open** — a dock triggered manually while the settings screen is foreground now hides normally instead of lingering.

---

## [1.3.8] - 2026-07-03

### Fixed
- **Tapping a saved clip could crash** — opening or sharing a clip (document, PDF, image, or link) with no app installed to handle it now shows a brief message instead of crashing.
- **App drawer silently stopped refreshing after toggling the dock off and on** — the installed-app list and the live state of the ringer/rotate/brightness tiles stopped updating once the dock was turned off and back on within the same session; they now keep working.
- **Backup export/restore no longer runs on the main thread** — writing or reading a backup file (especially to cloud storage like Drive) could freeze the UI; the file work now happens off the main thread.
- **Crash hardening** — foreground-service starts, Quick Share / QR-scanner / email launches, and capturing shared content into the clipboard are now guarded so a missing app or denied permission can no longer crash Floating Dock.

---

## [1.3.7] - 2026-07-02

### Added
- **Wifi, Bluetooth, and Quick Share quick controls** — Wifi opens the system Wi-Fi panel, Bluetooth opens Bluetooth settings, Share launches Quick Share directly for sending files to nearby devices.

### Changed
- **Design tab reordered** — Theme section now appears first, above Pill Size & Appearance and Dock Size & Appearance.
- **Filled-pill slider style** — every slider in the app (Design tab, App Grid rows/columns, RGB color picker) now matches the Volume/Brightness panel look: rounded track with a distinct empty-track tone and subtle outline, primary-colored fill, no visible thumb.

---

## [1.3.6] - 2026-07-02

### Added
- **Bottom pill gesture picker** — Design > Pill Size & Appearance, when edge position is Bottom: choose whether Double Tap or Swipe Up activates the pill (default Double Tap), with a note that Swipe Up works better on 3-button navigation devices.
- **Position from bottom slider** — Bottom pill can now be lifted off the very bottom edge instead of sitting flush against it.
- **Restrict Trigger to Pill** — Pill Size & Appearance: confine the touch-trigger area to the pill's own bounds instead of the full screen edge.
- **Share-hint card in Clips tab** — pinned card explaining that any app's Share sheet can save content into the clipboard panel; dismissible, shown until tapped away.
- **Contact & Feedback button** — General settings: opens an email draft for support/feedback without displaying the address in the UI.
- **What's New now shows skipped releases** — updating across multiple versions (e.g. straight from 1.3.4) shows the changelog for every version in between, not just the latest.

### Changed
- **Clipboard panel now floats above the dock** — instead of hiding the dock while open, the panel renders as a card above it (like the all-apps panel); dock stays visible throughout.
- **Clipboard panel appearance** — all four corners now rounded (previously only the top), panel fills available height between the status bar (4dp gap) and the dock instead of a fixed 85% fraction, 2dp side margins.
- **Clipboard panel narrow-screen fixes** — action-button rows (clip cards, notes) and the Clips/Favorites/Notes tab bar now wrap/distribute instead of risking clipping on narrow phones.
- **Clipboard external-open autohide** — opening a file, image, or link, or tapping Share, now temporarily hides the dock and music panel (if open), matching the existing behavior when launching an app from the all-apps menu.
- **Bottom pill defaults** — width 220dp, height 20dp, position-from-bottom 12dp (previously 130×8dp, flush at bottom).

### Fixed
- **Pill height slider clamped at ~18dp** — the bottom pill's touch window height was hardcoded to the Trigger Area size, silently clipping any Height value above it; the window now grows to fit the configured height.
- **"Window type can not be changed after the window is added" crash** — toggling the accessibility service could change a window's type while other flag/position updates were mid-flight, applying the new type via `updateViewLayout` instead of a fresh add; this is now blocked so only a full remove-and-re-add can change a window's type.

---

## [1.3.5] - 2026-07-01

### Added
- **Transparency slider** — Design > Theme: when Translucent Panels is enabled, a slider (30–100%) controls panel opacity across the dock, app menu, volume, brightness, music, and clipboard panels. Defaults to 80% to match prior behavior.
- **Clipboard / Notes panel** — new Quick Controls tile opens a floating panel (Clips, Favorites, Notes) reachable from the dock.
- **Share sheet capture** — any app's share sheet shows "TaskBar Clipboard" as a target; saves text, URLs, images, PDFs, text files (`.txt`, `.md`, `.csv`, `.html`, etc.), and now Word/Excel/PowerPoint documents (`.docx`, `.doc`, `.xlsx`, `.xls`, `.pptx`, `.ppt`).
- **Clips tab** — card per saved item with type icon, source app label, timestamp, and (for PDFs, text files, and documents) the original filename. Supports copy, share, pin, favorite, and delete actions.
- **Category filter** — scrollable All/Text/Images/Files/Links strip at the top of the Clips tab.
- **Favorites tab** — combined, timestamp-sorted view of starred clips and starred notes.
- **Notes tab** — multiple note cards with copy, share, pin, favorite, edit, and delete actions; "+" FAB to compose a new note inline.
- **URL clips** — tapping a URL clip opens it in the default browser.
- **Image clips** — thumbnail preview; tap to open in the default image viewer.
- **PDF, document, and text file clips** — filename shown on the card; tap to open in the associated app.
- **Share from panel** — every clip and note card has a share icon that forwards content to the Android share sheet.
- **Dock sync** — the dock hides while the clipboard panel is open and reappears when the panel is dismissed (back gesture or tap-outside). Opening a link, file, image, or the share sheet keeps the dock hidden instead of flashing it back in.
- **Clipboard tile in Controls settings** — the Clipboard tile now appears in the Controls tab so it can be reordered and toggled like other quick controls.

### Changed
- **Panel Outline** — now hides entirely (instead of graying out) while Translucent Panels is enabled, since the two modes are mutually exclusive.

---

## [1.3.4] - 2026-06-30

### Changed
- **Translucent panels** — dock, app menu, volume, brightness, and music panels now render a grain/noise texture when Translucent Panels is enabled, giving a frosted-glass appearance.

---

## [1.3.3] - 2026-06-28

### Added
- **Translucent panels** — General > Theme: toggle semi-transparent dock, app menu, volume, brightness, music, and search panels. On Android 12+, volume, brightness, and music panels also apply window-level blur behind them for a frosted-glass look.
- **Glassmorphism border** — when Translucent Panels is enabled, all panels show a subtle 1dp outline border to simulate the glass edge.

### Changed
- **Panel Outline** — border reduced from 2dp to 1dp. The toggle is automatically disabled and cleared when Translucent Panels is enabled (the two modes are mutually exclusive).

### Fixed
- **Accessibility service reconnect** — re-enabling the accessibility service no longer leaves orphaned duplicate overlay views, which previously caused overlapping dock bars and required a manual stop/start cycle to recover.

---

## [1.3.2] - 2026-06-27

### Added
- **Panel Outline toggle** — General > Theme: enable/disable the green border on the dock, app menu, volume, brightness, and music panels.
- **App Grid configuration** — Apps tab: "App Grid" card with Columns and Rows sliders (3–6) to customise the floating all-apps grid layout.
- **Music panel auto-hide** — music panel now hides automatically when the volume or brightness panel is triggered.

### Changed
- **Icon size slider range** — Pinned App Icon Size and Quick Controls Size sliders in Design > Dock Size & Appearance now go up to 60 dp (was 50 dp).
- **App menu button size** — the grid icon button in the dock now matches the Pinned App Icon Size setting instead of being fixed at 48 dp.

---

## [1.3.1] - 2026-06-15

### Changed
- **Brightness slider** — now horizontal (drag right to increase); removed "Bright" label.
- **Quick Controls description** — updated to explain swipe-up/swipe-down gestures.
- **Surface Tint Color description** — shortened to "Applies to the dock and app menu".

### Fixed
- **Quick Controls toggle text overflow** — description text no longer clips into the switch on narrow screens.

---

## [1.3.0] - 2026-06-14

### Added
- **Dock status bar** — slim strip at the bottom of the dock showing current time & date (left) and battery level with icon (right). Time updates every minute; battery icon reflects charge level and charging state.
- **Apps tab hint** — instructional text at the top of the Apps settings tab explaining how to pin and reorder apps.
- **Pill hint for side positions** — "Swipe Up in Trigger Area to Activate the Dock" hint shown below the preview graphic when Left, Right, or Both is selected (matches the existing Bottom hint).

### Changed
- **App menu — quick controls removed** — the quick controls column is no longer shown in the all-apps panel. The app grid now fills the full panel width.
- **App grid size** — increased from 320 dp to 340 dp tall, reliably showing 4 rows of app icons.
- **Dock item spacing** — pinned apps and quick controls rows now use `spacedBy(4 dp, CenterHorizontally)`: items are centred with consistent 4 dp gaps when few, and scroll with 4 dp gaps when many.
- **Circle icons across the app** — AppMenuButton (grid icon in dock), app icons in the Apps settings tab and Pinned Apps manager, and control icons in the Controls settings tab (Active Controls and All Controls) are now displayed as circles.
- **Brightness panel outline** — updated to match the Volume panel: 2 dp `TaskbarOutlineGreen` border instead of the 1 dp `outlineVariant` border.
- **Pill position defaults** — switching to Bottom now sets 130 × 8 dp (wider bar); switching to Left/Right/Both now sets 4 × 60 dp (taller, narrower pill).

### Fixed
- **Dock visibility persists across service restarts** — the dock's hidden/visible state is now written to DataStore on every `show`/`hide` call and read back when the service starts. Stopping and restarting the service no longer forces the dock visible if it was dismissed.

---

## [1.2.8] - 2026-06-12

### Fixed
- **Touch blocking in third-party apps** — the overlay window is now removed from the input dispatcher when the menu is closed, fixing unresponsive taps in apps like Play Store and Google.
- **Back gesture dismissal** — the taskbar window is now focusable while the dock is visible, so the back gesture correctly dismisses it when an app is in the foreground.

### Changed
- **App menu opens above the taskbar** — the all-apps panel now slides up above the taskbar strip instead of replacing it; the dock remains visible and interactive while the menu is open.

---

## [1.2.6] - 2026-06-09

### Fixed
- **Overlay visible on lockscreen** — the overlay now hides automatically when the device is locked and reappears on unlock.

---

## [1.2.5] - 2026-06-09

### Changed
- **Circle app icons** — app icons in the all-apps panel, pinned apps, and recent apps now display as circles instead of squares.
- **Dynamic icon sizing** — taskbar app icons scale with the taskbar height (clamped 24–48dp).

### Fixed
- **Pinned row clipping** — when Recent Apps was disabled but the controls strip was visible, pinned app icons were cut off on the right. The pinned row now expands to match the controls strip width.

---

## [1.2.4] - 2026-06-04

### Fixed
- **Crash on Android 8/9** — checking Usage Stats permission used an API-29-only method (`unsafeCheckOpNoThrow`), causing a crash on devices running Android 8 or 9. Now falls back to the compatible method on older API levels.
- **Crash with duplicate app entries** — apps with multiple launcher activities appeared twice in the app grid, triggering a duplicate key crash in the UI. Deduplicated by package name on load.

---

## [1.2.3] - 2026-06-02

### Fixed
- **Bottom pill position** — selecting Bottom in Pill Size & Appearance now persists correctly across restarts (was loading as Right due to a missing case in the preference parser).

---

## [1.2.2] - 2026-06-02

### Added
- **Accessibility Service as required permission** — moved from the optional permissions page to the required permissions page in onboarding, alongside "Draw over other apps". Heading updated to "Two permissions required".

### Changed
- **Default pill position** — changed from Bottom to Right on fresh installs.
- **Default control labels** — "Show Control Labels" now defaults to off on fresh installs.

### Fixed
- **Transparent navigation bar** — Settings and Onboarding screens now draw content behind the system navigation bar (edge-to-edge). The floating nav pill and onboarding buttons row are inset above the nav bar; tab scroll content gains matching bottom padding so nothing is obscured.

---

## [1.2.1] - 2026-05-31

### Added
- **Welcome onboarding flow** — first install now shows a 3-page onboarding: a welcome screen with a phone-and-dock graphic, a required-permissions screen with a pill-positions graphic, and an optional-permissions screen. Navigation via page-dot indicators and Next / Get Started buttons.
- **Battery optimisation exclusion** — new optional permission card in onboarding (and Settings) to exclude the app from battery optimisation, preventing the system from killing the dock in the background.

### Fixed
- **Landscape auto-hide** — the auto-hide-in-landscape setting now works correctly; previously the preference value was never loaded in the overlay service so the setting had no effect.
- **Recents full-width when no pinned apps** — when Recent Apps is enabled and no apps are pinned, the recent apps section now expands to fill the full dock width instead of being constrained to 40 %.

---

## [1.2.0] - 2026-05-27

### Added
- **Pill edge positions** — trigger pill can now be placed on the Left, Right, Both sides, or the Bottom of the screen. Left/Right/Both positions are vertically centred 2 dp from the edge; Bottom is fixed at the bottom centre.
- **Position along edge slider** — when a side position is selected, a slider (0–100%, default 50% = centre) controls the vertical placement of the pill along the edge. The phone-frame preview updates live.
- **Position defaults on switch** — switching to Bottom auto-sets the pill to 180 × 20 dp (horizontal bar); switching to a side auto-sets it to 12 × 120 dp (vertical bar).
- **Device-dependent slider maxes** — Width and Height slider maximums now scale with the device's screen dimensions (width max = screen width dp; height max = half screen height dp).
- **Floating bottom pill navigation in Settings** — tabs moved from a top tab bar to a floating pill at the bottom of the screen, with icons for each section (General, Apps, Controls, Design).

### Changed
- **Controls row centred in dock** — quick controls are now centred horizontally when fewer controls are shown than the full dock width.
- **Pinned / Recent split** — when Recent Apps are enabled the dock expands to full width and allocates 60 % of the available space to pinned apps and 40 % to recent apps.

### Fixed
- **Panel toggles no longer hide the taskbar** — tapping an icon (app menu, volume, brightness, music) a second time now closes only that panel; the main dock stays visible. The dismiss chain is: app menu → volume → brightness → music → hide taskbar.
- **App icon rounded corners** — restored adaptive icon so the launcher applies the correct squircle mask; added per-density foreground layers and updated the background colour to match the new icon.

---

## [1.1.9] - 2026-05-26

### Added
- **Gesture "Disable" option** — each trigger gesture (swipe up, swipe down, double tap) can now be set to "Disabled" so it performs no action.
- **Notification Listener permission card** — added to the onboarding screen; required for the music panel to show media controls.
- **Usage Access permission card** — added to the onboarding screen; required for the Recent Apps section in the taskbar.

### Changed
- **Controls strip merged into dock** — the floating quick controls strip is removed as a separate window. When "Show Controls in Dock" is enabled, controls appear as a row directly inside the taskbar surface above the pinned apps, separated by a subtle divider. The taskbar height adapts automatically.
- **Gesture actions are now independent** — each of the three trigger gestures (swipe up, swipe down, double tap) is independently configurable to: Show Dock, Notifications, Quick Settings, or Disabled. The "Swipe In" gesture has been removed.
- **"Enable Controls Strip" renamed to "Show Controls in Dock"** — label and description updated to match the new behaviour.

### Fixed
- **Auto-hide in Landscape** — taskbar and pill are now correctly hidden when the setting is enabled and the device is already in landscape when the service refreshes its views.

---

## [1.1.8] - 2026-05-25

### Fixed
- **Edge-to-edge (Android 15)** — removed deprecated `setStatusBarColor`, `setNavigationBarColor`, and `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` calls by switching the XML theme from MDC `Theme.Material3.DayNight.NoActionBar` to `Theme.AppCompat.DayNight.NoActionBar` and dropping the `com.google.android.material` library.

---

## [1.1.7] - 2026-05-23

### Added
- **Caffeine control** — new quick control that cycles screen timeout through 3 min → 5 min → 10 min → device default. Label shows the active duration when on. Requires Write Settings permission (same as Brightness).
- **Recent Apps taskbar section** — optional second section in the taskbar showing recently used apps. Pinned apps are excluded from recents to avoid duplicates. Enable in General settings (requires Usage Access permission). Pinned section is capped at ~3 visible icons and scrolls independently.

### Changed
- **Controls tab** — Active Controls list is now a horizontal scrollable row (icon + label tiles), matching the Pinned Apps style.

---

## [1.1.6] - 2026-05-20

### Changed
- **Taskbar icons** — long press now drags to reorder directly. The long-press popup (shortcuts + unpin) has been removed; use the apps panel to pin or unpin apps.

---

## [1.1.5] - 2026-05-20

### Changed
- **Taskbar width** — now auto-sizes to fit pinned apps (capped at 95% of screen width). The manual width slider has been removed from Design settings.
- **Long-press popup** — narrowed to 140dp. When the controls strip and/or music panel are visible, the popup positions itself above them.
- **Volume/brightness sliders** — now draw on top of the music panel instead of behind it.
- **Music panel** — disabled by default on fresh installs.

### Added
- **Notifications permission** — added to the onboarding screen alongside the other permission cards (required on Android 13+ for music panel media session access).

---

## [1.1.4] - 2026-05-20

### Changed
- **Music Panel** — no longer auto-shows when media plays. A Music toggle icon now appears first in the controls strip (or in the apps panel controls column when the strip is off), visible whenever the Music Panel is enabled in settings. Tap it to show or hide the panel.
- **Music Panel position** — dynamically repositions: floats above the controls strip when strip is on, above the taskbar when strip is off, and above the apps panel when the apps panel is open. Hides automatically when search is triggered.
- **Screenshot / Lock Screen from apps panel** — tapping either control now dismisses the apps panel and hides the taskbar automatically (previously only worked from the strip).

### Added
- **Swipe to change Settings tabs** — swipe horizontally across the Settings screen to switch between General, Pinned Apps, Controls, and Design tabs.

---

## [1.1.3] - 2026-05-19

### Added
- **Music Panel** — a pill-shaped floating card that appears above the taskbar when media is playing or paused. Shows album art, track title, artist, and previous/play-pause/next transport controls. Visible whenever a media session is active in the system notifications (persists while paused, hides when the notification is cleared). Appears only when the taskbar is triggered and dismisses with it on outside tap. Requires Notification Access (one-time grant). Enable/disable toggle and permission button available in General settings.

### Fixed
- **Screenshot and Lock Screen controls missing from Controls tab** — both controls appeared in the quick strip but were not listed in the Controls tab in Taskbar Settings, making them impossible to reorder or disable.

---

## [1.1.2] - 2026-05-19

### Added
- **Search Enter-to-Launch** — Press the Enter/Go key while searching to instantly launch the top search result app.
- **Screenshot quick control** — New control tile to take a screenshot via Accessibility Service (Android 9+). Only shown when Accessibility Service is enabled.
- **Lock Screen quick control** — New control tile to lock the device screen via Accessibility Service (Android 9+). Only shown when Accessibility Service is enabled.

### Changed
- **Enhanced onboarding permissions** — Added optional permission requests for "Modify System Settings" (required for brightness and auto-rotate controls) and "Do Not Disturb Access" (required for DND control) during first-time setup.
- **Accessibility Service description updated** — Now mentions screenshot and lock screen capabilities.

---

## [1.1.1] - 2026-05-19

### Fixed
- **Lockscreen unlock not showing taskbar** — simplified `ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON` / `ACTION_USER_PRESENT` handling to match sidebar project approach. Previously setting `visibility = GONE` during screen off caused WindowManager to remove views on some Android versions; now just ensures views exist on screen-on and lets insets listener manage visibility.
- **Quick strip not closing on outside tap** — `observeOverlayInteractivity()` and `attachInsetsListener()` now correctly include quick strip visibility in interactivity calculations.
- **Volume/Brightness scrim race condition** — combined separate `observeVolumePanelVisibility()` and `observeBrightnessPanelVisibility()` into single atomic `observeVolumeAndBrightnessPanels()` using `combine()` to prevent conflicting scrim visibility updates.
- **Overlay tile rapid-toggle race condition** — added `Mutex` to `OverlayTileService.onClick()` to prevent state corruption on rapid tile taps.
- **Repository resource leak** — `AppRepository` and `QuickControlsRepository` cleanup now called in `OverlayService.onDestroy()`.

---

## [1.1.0] - 2026-05-18

### Changed
- **App renamed to Floating Dock** — rebranded from "TaskBar" to "Floating Dock" for better distinction on app stores.
- **Package name changed** — new package name is `com.alkisstam.taskbar` (previously `com.taskbar.app`).

---

## [1.0.9] - 2026-05-17

### Changed
- **Brightness tile merged with Auto-Brightness** — the separate *Auto-Bright* tile is removed. The brightness slider panel now has a tappable icon at the top of the slider that toggles auto-brightness on/off. The icon shows `BrightnessAuto` (primary-tinted, highlighted) when auto is on and `BrightnessHigh` (muted) when manual; the slider track dims to 40 % opacity while auto-brightness is active. Dragging the slider while auto is on automatically switches the device to manual mode.

### Fixed
- **Brightness slider stutters on drag** — `localCurrent` state was keyed on `brightnessLevel`, so every `onBrightnessChange` callback triggered a `remember` re-evaluation that snapped `localCurrent` back mid-drag. Fixed by using a stable key (no key), matching the `VolumeSliderColumn` pattern.
- **`userPresentReceiver` registered on every `onStartCommand` call** — with `START_STICKY` the service can be restarted, calling `onStartCommand` again and adding a second registration of the same receiver with no guard. The receiver was also redundant: `lockscreenReceiver` already handles `ACTION_USER_PRESENT` via `showOverlay()`. Removed `userPresentReceiver` entirely.
- **`setBrightness` triggered brightness-mode content observer on every drag tick** — the method unconditionally wrote `SCREEN_BRIGHTNESS_MODE_MANUAL` before each brightness write. When the device was already in manual mode this was a no-op value-wise, but some Android builds still notify observers on the write. Fixed by only writing the mode when transitioning from auto.
- **`finalize()` used for resource cleanup in `@Singleton` classes** — `AppRepository` and `QuickControlsRepository` defined `finalize()` as a fallback to call `cleanup()`. Hilt keeps a strong reference to `@Singleton` objects for the entire process lifetime, so `finalize()` is never invoked in practice. Removed; `cleanup()` must be called from an explicit lifecycle hook.

---

## [1.0.8] - 2026-05-17

### Added
- **Controls tab** — new fourth tab in Settings replacing the old Controls section in General. Contains the Quick Controls master toggle, the Controls Strip sub-toggle, a reorderable *Active Controls* list (long-press to drag), and per-control enable/disable switches for all nine controls (Torch, Ringer, Rotate, Auto-Brightness, Brightness, DND, QR, Power, Volume). Order configured here is shared by both the strip and the app-menu column.
- **Quick Controls master toggle** — single switch that disables the strip and the app-menu column simultaneously and greys out all Controls sub-settings when off. Toggle immediately applies without needing to reopen the panel.
- **Volume panel** — separate `WindowManager` overlay window containing four vertical pill-sliders (Media, Ring, Notification, Alarm). Opens above the quick-controls strip when the Volume tile is tapped; dismissed by tapping anywhere outside. Ring slider is always shown regardless of ringer mode.
- **Brightness panel** — separate `WindowManager` overlay window with a single vertical brightness pill-slider (0–255). Opens when the *Brightness* tile is tapped; dismissed by tapping anywhere outside. Automatically switches the device to manual brightness mode on first drag.
- **Custom color picker** — a *Custom…* swatch at the end of the Surface Tint preset row opens a dialog with R, G, B sliders (0–255 each) and a live preview swatch, letting the user set any arbitrary color as the surface tint.

### Changed
- **Quick Controls Strip layout** — fixed 70 dp height (was dynamic), flexible min-width that shrinks when fewer controls are active, 2 dp spacing between icon tiles.
- **Volume slider width** — narrowed from 44 dp to 40 dp (corner radius 22 dp → 20 dp).
- **Controls Strip toggle** — moved from the General tab to the Controls tab.
- **Auto-Brightness tile label** — renamed from *Bright* to *Auto-Bright* to distinguish it from the new Brightness slider tile.
- **Surface Tint preset "Forest" removed** — replaced with *Sand* (`#FFF3E0`, a warm light tone).

### Fixed
- **Volume slider locks mid-drag** — `SideEffect` that synced `localCurrent` from the ViewModel fired during active drag gestures; if `AudioManager.getStreamVolume` returned the pre-write value (async flush), it reset `localCurrent` and froze the slider. Fixed by guarding the sync with an `isDragging` flag set in `onDragStarted` / `onDragStopped`.
- **Volume slider stale current on rapid drag** — drag lambda read `stream.current` from the last recompose instead of a locally-tracked value, causing incorrect target values on fast swipes. Fixed by introducing `localCurrent` state updated synchronously in the drag lambda.
- **Volume slider accumulator leaks between gestures** — `dragAccumulator` was never reset between separate drag sessions; leftover sub-step fraction caused an unexpected immediate step at the start of the next drag. Fixed by resetting to `0f` in `onDragStarted`.
- **Volume panel corner transparency** — `ComposeView` hosting the panel lacked an explicit transparent background; set `setBackgroundColor(Color.TRANSPARENT)` on the view.
- **Master Quick Controls toggle did not immediately hide strip** — the strip's visibility observer checked `quickControlsEnabled` but the toggle wrote a new value that required the strip state to also be re-evaluated; fixed by ensuring the strip observer combines both flags correctly.

---

## [1.0.7] - 2026-05-16

### Added
- **Drag-to-reorder on taskbar** — long-press any pinned app icon in the taskbar to drag and reorder it in place. Order is persisted immediately.
- **Show App Labels toggle** — new toggle in *Design → Taskbar Size & Appearance*; when enabled, the app name is shown below each icon in the taskbar row.
- **App Shortcuts on long-press** — long-pressing a pinned app now shows its static launcher shortcuts (up to 4) above the *Unpin* option. Tapping a shortcut launches it directly.
- **Quick Controls auto-refresh** — the quick controls strip and app menu panel now update automatically when ringer mode, auto-rotate, or brightness mode changes at the system level (no manual refresh needed).
- **Surface Tint Color picker** — a new color swatch row in *General → Theme* applies a uniform background tint to all three UI surfaces simultaneously: taskbar, app menu panel, and quick controls strip. Nine presets including Default (theme color), Navy, Deep Purple, Forest, Slate, Charcoal, Rose, and Midnight.

### Fixed
- **Long-press popup covered by quick controls strip** — the popup menu that opens on long-press of a pinned app is now shifted upward by an extra `taskbarHeight + 4 dp` when the quick controls strip is enabled, so it always clears the strip.
- **Icon thread safety** — app icons are now stored as `Bitmap` instead of `Drawable`, loaded once on `Dispatchers.IO`, and rendered synchronously in the UI thread with no conversion step.
- **App list stale after install/uninstall** — `AppRepository` now registers a `BroadcastReceiver` for `PACKAGE_ADDED`, `PACKAGE_REMOVED`, and `PACKAGE_REPLACED` and reloads the app list automatically.
- **BootReceiver coroutine leak** — `BootReceiver.onReceive` now uses `goAsync()` with a dedicated coroutine scope, ensuring `PendingResult.finish()` is always called.
- **Accessibility service starts unconditionally** — `TaskBarAccessibilityService.onServiceConnected` now reads the `overlayEnabled` preference before starting `OverlayService`; the service is no longer started if the user had disabled the overlay.
- **Unused CAMERA permission** — removed from `AndroidManifest.xml`.
- **`pinnedPackages` recomputed on every recomposition** — wrapped in `remember(pinnedApps)` to avoid allocating a new `Set` on every frame.

---

## [1.0.6] - 2026-05-15

### Added
- **Quick Controls Strip** — a new always-visible panel that appears directly above the taskbar, showing the same quick controls (torch, ringer, rotate, brightness, DND, QR, power) in a horizontal scrollable row. Enable via the new *Quick Controls Strip* toggle in Behaviour settings. The strip is hidden automatically when the app menu or search is open. When enabled, the quick controls column inside the app menu is removed to avoid duplication.
- **Pinned Apps horizontal row in Settings** — the Pinned Apps card now shows icons in a horizontal scrollable row matching the taskbar layout; long-press and drag any icon to reorder, tap the ✕ badge to unpin.

### Fixed
- **Broadcast receiver crash on Android 13+** — `registerReceiver` for custom `ACTION_SETTINGS_OPEN` / `ACTION_SETTINGS_CLOSE` actions was missing the required `RECEIVER_NOT_EXPORTED` flag, causing a `SecurityException` on API 33+.
- **Quick Controls Strip touch passthrough** — strip overlay window now uses `WRAP_CONTENT` height with dynamic `FLAG_NOT_TOUCHABLE` toggling; when hidden the window passes all touches through to the taskbar and pill below.

---

## [1.0.5] - 2026-05-15

### Added
- **All Apps grid in Pinned Apps settings** — a 4-column scrollable `LazyVerticalGrid` of all installed apps inside the Pinned Apps tab; each icon has a badge overlay (✓ pinned / + unpinned) to toggle pinning directly from settings.
- **Auto-hide in Landscape** — new "Behaviour" toggle; hides the overlay and pill when the device rotates to landscape and restores them on portrait.
- **Taskbar always visible in settings** — the taskbar is force-shown while the TaskBar settings activity is in the foreground; tapping outside it no longer closes it, and it is restored to normal behaviour when leaving settings.

### Fixed
- **Lockscreen: search overlay not hidden** — `searchView` was missing from the `ACTION_SCREEN_OFF` hide logic; it is now hidden alongside the overlay and pill.
- **App menu close animation** — exit was a short upward-quarter slide + fade, making it look like a fade-out; changed to a full slide-down matching the open animation.

### Changed
- **Pill width maximum** — increased from 120 dp to 220 dp.

---

## [1.0.4] - 2026-05-15

### Fixed
- **Fullscreen auto-hide never restored taskbar** — the insets observer only hid the taskbar when fullscreen was detected but never showed it again on exit; added `showTaskbar()` in the non-fullscreen branch.
- **`ACTION_SCREEN_ON` callback stacking** — rapid screen-on events could queue multiple deferred "show" callbacks; fixed by calling `removeCallbacksAndMessages` before re-posting the delayed runnable.
- **Search keyboard shown on every layout pass** — `onPlaced` fires on every recomposition, causing `keyboardController.show()` to spam on every keystroke; moved the call into `LaunchedEffect(isSearching)` so it fires once per search open.
- **`observersStarted` flag not reset on destroy** — flag is now cleared in `onDestroy()` for correctness on service restart.
- **Unused `DpOffset` import in `PinnedAppItem`** — dead import leftover from the old `DropdownMenu` removed.

---

## [1.0.3] - 2026-05-14

### Fixed
- **Fullscreen auto-hide observer never attached** — `observeFullscreenAutoHide()` was called before `overlayView` existed, causing immediate return. Fixed by attaching via `addOverlayView()` using `OnApplyWindowInsetsListener` (API 30+).
- **Keyboard showing used arbitrary delay** — replaced `delay(100)` magic number with `.onPlaced { keyboardController?.show() }` for reliable keyboard display once the field is laid out.
- **Context menu offset hardcoded in pixels** — changed from `IntOffset(0, -160/-180)` to density-aware `with(LocalDensity.current) { 56.dp/64.dp.roundToPx() }` for consistent positioning across screen densities.
- **Lockscreen flicker race condition** — added 300ms Handler delay for `ACTION_SCREEN_ON` to let keyguard state stabilize, with proper callback cleanup on other events to prevent flicker.

---

## [1.0.2] - 2026-05-14

### Added
- **Auto-hide in Fullscreen** — new "Behaviour" toggle in General settings; hides the taskbar when the foreground app goes fullscreen (status bar insets disappear, API 31+).
- **Hide on lockscreen** — overlay and pill are automatically hidden on screen-off / lockscreen and restored on unlock via `ACTION_SCREEN_OFF`, `ACTION_SCREEN_ON`, and `ACTION_USER_PRESENT` broadcasts.
- **Hide taskbar on search launch** — launching an app from the floating search bar now also collapses the taskbar, consistent with launching from the pinned row or app grid.

### Fixed
- **App menu opens above taskbar** — `AppMenuPanel` was an independent full-screen overlay; restructured as an inline `Column` child so it slides in from directly above the taskbar with a 4 dp gap.
- **Taskbar closes when opening app menu** — removed the `|| menuVisible` condition; taskbar now stays visible while the menu is open and only hides when a pinned app is tapped.
- **Background tap behaviour** — tapping outside the app menu now dismisses the menu (without hiding the taskbar); tapping outside the taskbar (when menu is closed) still hides it.
- **App panel slide animation** — panel now slides in from ¼ of its own height with a fade, appearing to emerge from just above the taskbar instead of sliding up from the screen bottom.
- **Search keyboard not re-opening** — `LaunchedEffect(Unit)` only fired once per composition; changed to `LaunchedEffect(isSearching)` + explicit `SoftwareKeyboardController.show()` with a short delay so the keyboard reliably appears on every search open.
- **Long-press context menu** — replaced `DropdownMenu` with a custom `Popup` + `Surface(RoundedCornerShape(12.dp))`; menu is now compact, rounded, and positioned above the icon (taskbar: −160 px offset; app grid: −180 px offset).

### Optimised
- **Observer leak on service restart** — `OverlayService.onStartCommand` was re-registering all flow observers on every `START_STICKY` restart; guarded with `observersStarted` flag.
- **Swipe trigger fires once per gesture** — `SwipePill` was calling `onExpand()` on every drag frame past the threshold; now uses a `firedThisGesture` flag reset on `onDragStart`.
- **DataStore write storm from sliders** — settings sliders were persisting to DataStore on every pixel of drag; now use local `mutableFloatStateOf` for preview and only write on `onValueChangeFinished`.
- **`combine()` unsafe cast** — replaced array-based `combine { values -> values[0] as Boolean }` with the typed 3-argument lambda overload.
- **Dead DataStore keys removed** — `TORCH_ENABLED_KEY`, `RING_MODE_ENABLED_KEY`, `AUTO_ROTATE_ENABLED_KEY`, `AUTO_BRIGHTNESS_ENABLED_KEY` were declared but never read or written.
- **Removed redundant `isAccessibilityServiceEnabled()`** — was a wrapper around the existing `isAccessibilityEnabled` `StateFlow`.
- **Wrong icon for Swipe In gesture** — was showing `SwipeUp`; now correctly shows `SwipeRight`.

---

## [1.0.1] - 2026-05-13

### Added
- **Accessibility Service overlay** — `TaskBarAccessibilityService` uses `TYPE_ACCESSIBILITY_OVERLAY` so the taskbar and pill draw above the system navigation bar. Enable via *Settings → Accessibility → TaskBar Overlay*.
- **Design tab** — replaced the flat "Pill" tab with three collapsible drop-down sections:
  - *Trigger Gesture* — choose swipe-up, swipe-down, swipe-in, or double-tap
  - *Pill Size & Appearance* — width, height, opacity, position Y/X sliders + live preview
  - *Taskbar Size & Appearance* — position from bottom, width (%), height sliders
- **Taskbar settings persistence** — `TaskbarSettings` data class stored in DataStore; `TaskbarView` reacts live to width, height, and vertical position changes.
- **Auto-close taskbar on app launch** — tapping a pinned app or any app in the all-apps grid now hides the taskbar immediately after launching.
- **Quick Controls — DND** — toggle Do Not Disturb (Priority ↔ All). Tapping without permission opens the system DND access settings.
- **Quick Controls — QR Scanner** — opens the device's built-in QR/barcode scanner (Oppo/ColorOS `coloros.intent.action.CAMERA_SCANNER`, ZXing, and camera as fallbacks).
- **Quick Controls — Power menu** — shows the system power dialog via `GLOBAL_ACTION_POWER_DIALOG` (requires Accessibility Service to be active).
- **`ACCESS_NOTIFICATION_POLICY` permission** — declared for DND control.
- **Gradle & dependency upgrades** — Gradle wrapper 8 → 9.4.1, Hilt 2.54 → 2.57, Compose BOM 2024.12 → 2025.05 to support Kotlin 2.2.

### Fixed
- **Overlay not visible** — `OverlayService` was creating `TYPE_ACCESSIBILITY_OVERLAY` windows with the wrong `WindowManager` token. Fixed by routing all `addView` / `updateViewLayout` calls through `TaskBarAccessibilityService.accessibilityWindowManager`.
- **Crash on overlay start** — missing `ViewTreeLifecycleOwner` on the search overlay wrapper `FrameLayout`; fixed by setting lifecycle, `ViewModelStore`, and `SavedStateRegistry` owners on the wrapper view.
- **QR scanner not opening** — generic ZXing/GMS intents don't resolve on Oppo devices; now tries `coloros.intent.action.CAMERA_SCANNER` first.
