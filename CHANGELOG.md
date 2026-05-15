# Changelog

All notable changes to TaskBar are documented here.

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
