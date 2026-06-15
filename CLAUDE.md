# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Install

```bash
# Build and install signed release APK on connected device (preferred)
./gradlew installRelease

# Build release AAB for Play Store upload
./gradlew bundleRelease
# Output: app/release/app-release.aab

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.alkisstam.taskbar.viewmodel.TaskbarViewModelTest"
```

Release signing uses `keystore.properties` (not committed). The local signing key differs from the Play Store key — uninstall the Play Store build before sideloading.

When bumping a release: update `versionCode` and `versionName` in `app/build.gradle.kts`, then update `CHANGELOG.md`.

## Architecture

The app is a system overlay dock. It has no main UI beyond settings — the dock itself renders as floating `WindowManager` views above all other apps.

### Service layer (`service/`)

**`OverlayService`** is the core foreground service. It manages a set of independent `WindowManager` views (not a single window):
- `overlayView` — transparent full-screen touch interceptor
- `taskbarView` — the dock row (pinned apps + quick controls)
- `pillView` / `pillView2` — trigger pill(s) at the screen edge
- `searchView` — full-screen search overlay
- `volumePanelView`, `brightnessPanelView`, `musicPanelView`, `volumeScrimView` — floating panels

Each view is a `ComposeView` with its own Lifecycle/ViewModelStore/SavedStateRegistry wiring (all implemented on `OverlayService` itself). Views are added on `onStartCommand` and toggled via `View.VISIBLE`/`View.GONE`, not added/removed dynamically.

**`TaskBarAccessibilityService`** — a singleton (`instance`) that provides two things: (1) `TYPE_ACCESSIBILITY_OVERLAY` window type (allows drawing over system chrome including nav bar), and (2) `performGlobalAction()` for notifications, quick settings, back, lock, screenshot. `OverlayWindowParams.kt` picks `TYPE_ACCESSIBILITY_OVERLAY` when the a11y service is running, else falls back to `TYPE_APPLICATION_OVERLAY`.

`OverlayService` broadcasts are the IPC mechanism between `MainActivity`/settings and the overlay: `ACTION_SETTINGS_OPEN`, `ACTION_SETTINGS_CLOSE`, `ACTION_DISMISS_ALL`, `ACTION_ACCESSIBILITY_CHANGED`.

**`OverlayComposables.kt`** — all `@Composable` functions rendered inside the overlay views. `OverlayContent` is the main dock composable; `BrightnessPanelContent`, `VolumePanelContent`, `MusicPanelContent`, `SearchOverlayContent` are the panel composables.

### ViewModel layer (`viewmodel/`)

Two ViewModels, both Hilt-injected and created via `OverlayViewModelFactory` inside `OverlayService`:

- **`TaskbarViewModel`** — dock state (visibility, expansion, settings, pinned apps, theme, pill config). Also handles the `isSettingsOpen` flag and fullscreen/landscape auto-hide.
- **`AppMenuViewModel`** — app menu, search, quick controls, volume/brightness/music panel visibility, media playback, all quick control actions (torch, ringer, rotate, DND, etc.).

### Data layer (`data/`)

- **`PreferencesRepository`** — single source of truth for all persisted settings, backed by Jetpack DataStore. Exposes `Flow`s for each setting. Also handles backup/restore (JSON serialisation of all preferences).
- **`AppRepository`** — queries `PackageManager` for installed apps; caches as `StateFlow<List<AppInfo>>`.
- **`QuickControlsRepository`** — ordered list of enabled/disabled quick control tiles, persisted in DataStore.
- **`MediaRepository`** — wraps `MediaController` from `MediaListenerService` for playback state and controls.
- **`RecentAppsRepository`** — recent app history (usage stats or fallback).

### UI layer (`ui/`)

- `ui/taskbar/` — `TaskbarView` (dock row), `TriggerPillView`, `PinnedAppItem`, `AppMenuButton`
- `ui/appmenu/` — `AppMenuPanel`, `AppGrid`, `QuickControls`, `VolumePanel`, `MusicPanel`, `FloatingSearchBar`
- `ui/settings/` — `SettingsScreen` (4-tab: General, Apps, Controls, Design), `PillSettingsScreen`, `PinnedAppsManager`
- `ui/onboarding/` — permission onboarding flow

### Key data structures

`TaskbarSettings` and `PillSettings` are plain data classes in `PreferencesRepository.kt` (not separate files). `GestureAction` and `PillEdgePosition` enums live there too.

`QuickControlItemData` is in `QuickControls.kt`; the canonical default tile map (id → label/icon) lives in `QuickControlsRepository`.

### Overlay window flag rules

- Dock and overlayView use `MATCH_PARENT` × `MATCH_PARENT` so touch events can be intercepted across the full screen, then selectively dispatched.
- `FLAG_NOT_TOUCHABLE` / `FLAG_NOT_FOCUSABLE` are toggled dynamically as panels open/close (see `setOverlayFlags`, `setTaskbarFlags` in `OverlayService`).
- The pill uses narrow fixed dimensions; `BOTH` edge position spawns a second `pillView2`.

### DI

Hilt with `@Singleton` repositories. `OverlayService` is `@AndroidEntryPoint`. ViewModels inside the service are created via `OverlayViewModelFactory` (not `ViewModelProvider` default factory) because the service is its own `ViewModelStoreOwner`.
