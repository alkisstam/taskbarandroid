# Changelog

All notable changes to Floating Dock are documented here.

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
