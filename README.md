# Floating Dock

A system-overlay dock for Android that renders above all other apps — including the system navigation bar when the Accessibility Service is enabled.

**Current version:** 1.3.0 (versionCode 33)

## Features

### Dock

- **Persistent overlay dock** fixed at the bottom of the screen (98% width)
- **Drag-to-reorder** pinned apps directly in the dock row (long-press to start dragging)
- **Status bar strip** at the bottom of the dock: current time & date (left), battery level + icon (right)
- **Swipe up** on the dock to expand a Quick Controls row above the pinned apps; swipe down to collapse
- **Visibility persists** across service restarts — dock stays hidden if you dismissed it

### Trigger Pill

- Small pill that reveals the dock without opening the App Menu
- **Bottom position:** double-tap pill or Home button to activate
- **Left / Right / Both:** swipe up in the trigger area to activate
- Configurable size, opacity, and position along the edge

### App Menu

- **Floating panel** that slides up from above the dock
- **4-row, 3-column** scrollable app grid
- Full-screen app search
- Long-press any app to pin/unpin from the dock

### Quick Controls

- Expandable row above pinned apps (swipe up on dock to reveal)
- Controls: Music, Torch, Ringer, Rotate, Brightness, DND, QR Code, Power, Volume, Screenshot, Lock Screen
- Reorderable and individually enable/disable in the **Controls** settings tab
- **Music Panel:** floating player with album art, playback controls (prev/play-pause/next)
- **Volume Panel:** per-stream vertical sliders (Media, Ring, Alarm)
- **Brightness Panel:** horizontal slider with auto-brightness toggle

### Theming

- **Material You** – Dynamic Color (Android 12+), fallback palette for older devices
- **Dark / Light / System** theme toggle in *General → Theme*
- **Surface Tint Color** – nine preset color swatches that tint the dock, app menu, and control panels

### Behaviour

- **Auto-hide in Fullscreen** – hides when the foreground app goes fullscreen
- **Auto-hide in Landscape** – hides when the device rotates to landscape
- **Hide on screen-off / lockscreen** – overlay hidden on screen-off, restored on unlock
- **Boot autostart** – re-enables the overlay after reboot

### Settings

Four-tab settings screen: **General**, **Apps**, **Controls**, **Design**

- **Apps** tab: manage pinned apps (drag-to-reorder, tap to pin/unpin); browse all installed apps
- **Controls** tab: Quick Controls master toggle, show/hide labels, reorder Active Controls, enable/disable individual controls
- **Design** tab: Pill Size & Appearance, Dock Size & Appearance

## Requirements

- Android Studio Ladybug or newer
- Android SDK 35 / min SDK 26
- Kotlin 2.1

## Build

1. Open the `TaskBarAndroid` directory in Android Studio
2. Let Gradle sync complete
3. Run on a physical device (overlay doesn't work on emulators without extra setup)
4. On first launch, grant **"Draw over other apps"** and optionally **"Modify system settings"**
5. To draw above the navigation bar, enable **Floating Dock Overlay** in *Settings → Accessibility*

## Permissions

| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the dock above all other apps |
| `QUERY_ALL_PACKAGES` | List all installed apps |
| `FOREGROUND_SERVICE` | Keep the overlay service alive in the background |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required for special-use foreground service type (Android 14+) |
| `POST_NOTIFICATIONS` | Show the persistent foreground service notification |
| `CHANGE_AUDIO_SETTINGS` | Ring / vibrate / silent cycle |
| `WRITE_SETTINGS` | Auto-rotate and auto-brightness toggles |
| `ACCESS_NOTIFICATION_POLICY` | Do Not Disturb toggle |
| `RECEIVE_BOOT_COMPLETED` | Re-enable overlay after reboot |
| `BIND_ACCESSIBILITY_SERVICE` | Draw overlay above the system navigation bar via `TYPE_ACCESSIBILITY_OVERLAY` |

## Architecture

```
TaskBarApplication           (Hilt entry point)
MainActivity                 (Settings screen – General / Apps / Controls / Design tabs)
TaskBarAccessibilityService  (AccessibilityService – TYPE_ACCESSIBILITY_OVERLAY + back gesture)
OverlayService               (ForegroundService – WindowManager window host)
  ├── overlayView        (ComposeView – AppMenuPanel: 4×3 app grid + search)
  ├── taskbarView        (ComposeView – TaskbarView: handle, quick controls row, pinned apps, status bar)
  ├── pillView/pillView2 (ComposeView – TriggerPillContent: left/right/bottom edge pill)
  ├── searchView         (ComposeView – FloatingSearchBar)
  ├── volumePanelView    (ComposeView – VolumePanel: per-stream vertical sliders)
  ├── brightnessPanelView(ComposeView – BrightnessPanel: brightness slider)
  ├── musicPanelView     (ComposeView – MusicPanel: MediaSession player)
  └── volumeScrimView    (ComposeView – transparent clickable backdrop)

data/
  AppRepository            (PackageManager → installed apps; refreshes on package changes)
  MediaRepository          (MediaController – active MediaSession state)
  PreferencesRepository    (DataStore – pinned apps, theme, tint, pill/dock settings, visibility state)
  QuickControlsRepository  (Camera, AudioManager, Settings.System; notifies on system changes)

viewmodel/
  TaskbarViewModel         (@HiltViewModel – overlay, theme, tint, pinned apps, dock/pill settings, battery)
  AppMenuViewModel         (@HiltViewModel – app search, quick controls, media state, panel visibility)
```
