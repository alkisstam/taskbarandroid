# Changelog

All notable changes to TaskBar are documented here.

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
