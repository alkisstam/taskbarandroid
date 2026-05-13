# TaskBar – Android

A system-overlay taskbar for Android that renders above all other apps — including the system navigation bar when the Accessibility Service is enabled.

## Features

- **Persistent overlay taskbar** docked at the bottom of the screen
  - Configurable width, height, and vertical position
- **Navigation bar overlay** – enable the Accessibility Service to draw the taskbar *above* the system navigation bar
- **Trigger pill** – a small draggable pill that reveals the taskbar on swipe-up, swipe-down, swipe-in, or double-tap
  - Configurable size, opacity, and screen position
- **App Menu** – floating panel with:
  - 3-column scrollable app grid (80% width)
  - Quick Controls column (20% width): Torch, Ring mode, Auto-rotate, Auto-brightness
  - Full-screen app search
  - Long-press any app to pin/unpin from the taskbar
- **Pinned apps** – launch pinned apps directly from the taskbar; drag-to-reorder inline
- **Material You** – Dynamic Color (Android 12+), fallback palette for older devices; dark/light/system theme toggle
- **Auto-hide** when the soft keyboard appears
- **Design tab** – three collapsible sections: Trigger Gesture, Pill Size & Appearance, Taskbar Size & Appearance
- **Boot autostart** – re-enables the overlay on reboot (if permissions were granted)

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
| `CAMERA` | Torch (flashlight) control |
| `CHANGE_AUDIO_SETTINGS` | Ring / vibrate / silent cycle |
| `WRITE_SETTINGS` | Auto-rotate and auto-brightness toggles |
| `RECEIVE_BOOT_COMPLETED` | Re-enable overlay after reboot |
| `BIND_ACCESSIBILITY_SERVICE` | Draw overlay above the system navigation bar via `TYPE_ACCESSIBILITY_OVERLAY` |

## Architecture

```
TaskBarApplication           (Hilt entry point)
MainActivity                 (Settings screen – General / Pinned Apps / Design tabs)
TaskBarAccessibilityService  (AccessibilityService – enables TYPE_ACCESSIBILITY_OVERLAY)
OverlayService               (ForegroundService – WindowManager window host)
  ├── overlayView  (ComposeView)
  │     ├── AppMenuPanel   (sliding-up panel: AppGrid 80% + QuickControls 20%)
  │     └── TaskbarView    (horizontal row with pinned apps)
  ├── pillView     (ComposeView – trigger pill, draggable)
  └── searchView   (ComposeView – full-screen app search)

data/
  AppRepository            (PackageManager → installed apps)
  PreferencesRepository    (DataStore – pinned apps, theme, pill settings, taskbar settings)
  QuickControlsRepository  (Camera, AudioManager, Settings.System)

viewmodel/
  TaskbarViewModel         (@HiltViewModel – overlay, theme, pinned apps, pill/taskbar settings)
  AppMenuViewModel         (@HiltViewModel – app search, quick controls, menu state)
```
