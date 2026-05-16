# TaskBar – Android

A system-overlay taskbar for Android that renders above all other apps — including the system navigation bar when the Accessibility Service is enabled.

**Current version:** 1.0.7 (versionCode 8)

## Features

### Taskbar
- **Persistent overlay taskbar** docked at the bottom of the screen
  - Configurable width, height, and vertical position
  - **Drag-to-reorder** pinned apps directly in the taskbar row (long-press to start dragging)
  - **Optional app labels** shown below each icon (toggle in *Design → Taskbar Size & Appearance*)
- **Navigation bar overlay** – enable the Accessibility Service to draw the taskbar *above* the system navigation bar

### Trigger Pill
- **Trigger pill** – a small draggable pill that reveals the taskbar on swipe-up, swipe-down, swipe-in, or double-tap
  - Configurable size, opacity, and screen position

### App Menu
- **App Menu** – floating panel that slides up from above the taskbar:
  - 3-column scrollable app grid
  - Quick Controls column: Torch, Ring mode cycle (Normal/Vibrate/Silent), Auto-rotate, Auto-brightness, DND, QR Scanner, Power menu
  - Full-screen app search
  - Long-press any app to pin/unpin from the taskbar
- **Quick Controls Strip** – an always-visible horizontal row that sits directly above the taskbar (enable in *General → Behaviour*); auto-refreshes on system ringer/rotation/brightness changes. When enabled, the quick controls column is removed from the app menu to avoid duplication.

### Pinned Apps
- Launch pinned apps directly from the taskbar
- **Long-press shortcuts** – long-pressing a pinned app shows its launcher shortcuts (up to 4) plus an *Unpin* option
- Manage pinned apps in *Settings → Pinned Apps* tab with drag-to-reorder and tap-to-unpin

### Theming
- **Material You** – Dynamic Color (Android 12+), fallback palette for older devices
- **Dark / Light / System** theme toggle in *General → Theme*
- **Surface Tint Color** – nine preset color swatches in *General → Theme* that uniformly tint the taskbar, app menu panel, and quick controls strip

### Behaviour
- **Auto-hide in Fullscreen** – hides the overlay when the foreground app goes fullscreen
- **Auto-hide in Landscape** – hides the overlay when the device rotates to landscape
- **Auto-hide on keyboard** – overlay hides while the soft keyboard is visible
- **Hide on lockscreen** – overlay is hidden on screen-off and restored on unlock
- **Boot autostart** – re-enables the overlay on reboot (if permissions were granted)

### Settings
- Three-tab settings screen: **General**, **Pinned Apps**, **Design**
- **Design** tab has three collapsible sections: Trigger Gesture, Pill Size & Appearance, Taskbar Size & Appearance

## Requirements

- Android Studio Ladybug or newer
- Android SDK 35 / min SDK 26
- Kotlin 2.1

## Build

1. Open the `TaskBarAndroid` directory in Android Studio
2. Let Gradle sync complete
3. Run on a physical device (overlay doesn't work on emulators without extra setup)
4. On first launch, grant **"Draw over other apps"** and optionally **"Modify system settings"**
5. To draw above the navigation bar, enable **TaskBar Overlay** in *Settings → Accessibility*

## Permissions

| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the taskbar above all other apps |
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
MainActivity                 (Settings screen – General / Pinned Apps / Design tabs)
TaskBarAccessibilityService  (AccessibilityService – enables TYPE_ACCESSIBILITY_OVERLAY)
OverlayService               (ForegroundService – WindowManager window host)
  ├── overlayView    (ComposeView)
  │     ├── AppMenuPanel     (sliding-up panel: AppGrid + QuickControls column)
  │     └── TaskbarView      (pinned apps row – drag-to-reorder, labels, tint)
  ├── quickStripView (ComposeView – always-visible quick controls strip above taskbar)
  ├── pillView       (ComposeView – trigger pill, draggable)
  └── searchView     (ComposeView – full-screen app search)

data/
  AppRepository            (PackageManager → installed apps; refreshes on package changes)
  PreferencesRepository    (DataStore – pinned apps, theme, tint color, pill/taskbar settings)
  QuickControlsRepository  (Camera, AudioManager, Settings.System; notifies on system changes)

viewmodel/
  TaskbarViewModel         (@HiltViewModel – overlay, theme, tint, pinned apps, pill/taskbar settings)
  AppMenuViewModel         (@HiltViewModel – app search, quick controls, menu state)
```
