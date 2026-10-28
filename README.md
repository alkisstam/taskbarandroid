# Floating Dock

A system-overlay dock for Android that renders above all other apps — including the system navigation bar when the Accessibility Service is enabled.

**Current version:** 1.5.13 (versionCode 68) — see [CHANGELOG.md](CHANGELOG.md)

## Features

### Dock

- **Overlay dock** at the bottom of the screen with your pinned apps and the All Apps button
- **Drag-to-reorder** pinned apps directly in the dock row (long-press to start dragging)
- **Status bar strip**: time & date (left), battery level + icon (right)
- **Icons pill** above the dock, on the All Apps side: Quick Controls, app panels, Music and Pin Dock. Tap an icon to slide its panel in; tap it again, swipe down or tap outside to close. The last open panel comes back the next time the dock opens
- **App panels** — up to 3 extra rows of apps, each with a name, one of 17 preset icons and an on/off switch
- **Pin Dock** — keep the dock on screen; only the dock takes touches, so back gestures and the keyboard still reach the app underneath
- **Floating windows** (opt-in) — open apps in a floating window from the long-press menu, or on tap. Uses built-in OEM support, or Shizuku elsewhere (Shizuku also remembers each app's window size and position)

### Trigger Pill

- Small pill at the bottom, left, right or both edges that reveals the dock
- **Configurable gestures** — Double-Tap, Swipe Up, Swipe Down and (side pills) Swipe In each run an action: show dock, App Menu, Search Apps, notification/quick tiles panel, power menu, screenshot, flashlight, lock screen, or Disabled
- **Hold and Drag** — hold the pill, then drag to change brightness or media volume, with an on-screen level indicator
- **Swipe Sensitivity** (4–40dp) and **Restrict Trigger to Pill** (off = whole edge is the trigger area)
- Side pills block the Back gesture only while Swipe In has an action; capped at 200dp tall (Android's exclusion limit)
- Configurable size, opacity, tint and position along the edge

### App Menu

- **Fullscreen card** with the search pill and sort menu at the bottom; tap outside or press Back to close
- App order by Name, Install time or Usage; recent apps row
- **Icon Size** setting (32–120dp)
- Long-press any app to pin/unpin, or open in a floating window

### Quick Controls

- Controls: Torch, Ringer, Rotate, Brightness, DND, QR Code, Power, Volume, Screenshot, Lock Screen, Caffeine, Clipboard, Notes, Calculator, Wifi, Bluetooth, Share, Notification History, plus optional Quick Settings
- Reorder and enable/disable individually in the **Controls** settings tab
- **Auto Reveal Controls** — opens the Quick Controls panel whenever the dock appears
- **Music Panel** — player beside the icons pill with album art and prev/play-pause/next; tap the art to open the playing app
- **Volume Panel** — per-stream vertical sliders (Media, Ring, Alarm)
- **Brightness Panel** — slider with auto-brightness toggle
- **Clipboard Panel** — Clips / Favorites tabs; capture via the Android share sheet, open URLs/images/PDFs/files in place, edit text clips
- **Notes Panel** — notes and to-dos in their own full-screen panel
- **Calculator Panel** — live running total; swipe up on the handle for scientific functions
- **Notification History Panel** — up to 100 recent notifications grouped by app; tap to open, swipe to remove, or Clear all
- **Quick Settings Panel** — dock and pill settings inline in the overlay

### Theming

- **Material You** dynamic color (Android 12+), fallback palette on older devices
- **Dark / Light / System** theme
- **Surface tint** presets with adjustable tint strength
- **Transparency, grain and blur** — dock-only frosted glass (Android 12+, Samsung One UI via SemBlur), glass on panels, optional tint over blur
- **Icon packs** (ADW/Nova/GO format) and **Icon Shape** (Default, Square, Squircle, Circle)

### Behaviour

- **Auto-hide in Fullscreen / Landscape** — hides dock and pill
- **Hides under the keyboard and notification shade** (needs the accessibility service)
- **Hide on screen-off / lockscreen**, restored on unlock; optional **Disable on Lock Screen**
- **Boot autostart**
- **Vibrate Feedback** — haptic tick on long-press, drag-reorder and slider drags
- **Quick Settings tile** to toggle the dock off and on

### Settings

Four-tab settings screen: **General**, **Apps**, **Controls**, **Design**

- **General**: overlay status, theme, language override, behaviour, floating windows, permissions overview, per-manufacturer battery usage guide, backup/restore
- **Apps**: pinned apps (+ picker), app panels, App Grid (order, icon size)
- **Controls**: Quick Controls toggles, reorder Active Controls
- **Design**: pill size, appearance and gestures; dock size and appearance; blur, grain, tint; icon pack and shape

UI translated into 17 languages.

## Requirements

- Android Studio with AGP 9.4
- compileSdk / targetSdk 36, minSdk 26
- Kotlin 2.2, JVM target 21

## Build

```bash
./gradlew installRelease   # signed release on connected device (needs keystore.properties)
./gradlew bundleRelease    # AAB for Play
./gradlew test             # unit tests
```

1. Run on a physical device (overlay doesn't work on emulators without extra setup)
2. On first launch, grant **"Draw over other apps"** (the only required permission)
3. Optionally enable **Floating Dock Overlay** in *Settings → Accessibility* to draw above the navigation bar, use the bottom pill position, and get keyboard/shade/home detection — the app works without it

## Permissions

| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the dock above all other apps |
| `QUERY_ALL_PACKAGES` | List all installed apps |
| `FOREGROUND_SERVICE` | Keep the overlay service alive in the background |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Special-use foreground service type (Android 14+) |
| `POST_NOTIFICATIONS` | Persistent foreground service notification |
| `MODIFY_AUDIO_SETTINGS` | Ringer cycle and volume panel |
| `WRITE_SETTINGS` | Auto-rotate and brightness |
| `ACCESS_NOTIFICATION_POLICY` | Do Not Disturb toggle |
| `RECEIVE_BOOT_COMPLETED` | Re-enable overlay after reboot |
| `ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE` | Wifi control state |
| `BLUETOOTH_CONNECT` | Bluetooth control state |
| `moe.shizuku.manager.permission.API_V23` | Floating windows via Shizuku (optional) |
| `BIND_ACCESSIBILITY_SERVICE` | Draw above the navigation bar via `TYPE_ACCESSIBILITY_OVERLAY`, global actions |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Media session and notification history |

## Architecture

```
TaskBarApplication           (Hilt entry point)
MainActivity                 (Onboarding + settings: General / Apps / Controls / Design)
TaskBarAccessibilityService  (TYPE_ACCESSIBILITY_OVERLAY, global actions, home/IME/shade detection)
MediaListenerService         (NotificationListenerService – media sessions, notification history)
OverlayTileService           (Quick Settings tile – toggle the dock)
BootReceiver                 (autostart)
ClipboardShareActivity       (ACTION_SEND target – saves clip, opens panel)
OverlayService               (ForegroundService – WindowManager window host)
  ├── overlayView            (full-screen touch interceptor + App Menu)
  ├── taskbarView            (dock row, icons pill, app/controls panels)
  ├── pillView / pillView2   (trigger pill; second one for Both edges)
  ├── searchView             (app search)
  ├── volumePanelView, brightnessPanelView, musicPanelView, volumeScrimView
  ├── clipboardPanelView, notesPanelView, calculatorPanelView
  ├── notificationPanelView, quickSettingsPanelView
  ├── clipboardBlurView      (full-screen blur behind full-screen panels)
  └── levelIndicatorView     (brightness/volume indicator for Hold and Drag)

data/
  AppRepository                  (installed apps + icons; refreshes on package changes)
  IconPackRepository             (ADW/Nova/GO icon packs)
  MediaRepository                (active MediaSession state and controls)
  NotificationHistoryRepository  (recent notifications)
  PreferencesRepository          (DataStore – all settings, backup/restore)
  QuickControlsRepository        (control order/state; Camera, AudioManager, Settings.System)
  ClipboardRepository            (DataStore – clips + notes; files in filesDir/clipboard/)

viewmodel/
  TaskbarViewModel               (dock/pill state, pinned apps, panels, theme, auto-hide)
  AppMenuViewModel               (app menu, search, quick controls, media, panel visibility)
  ClipboardViewModel             (clips, favorites, notes CRUD)
  NotificationHistoryViewModel   (notification history panel)
```
