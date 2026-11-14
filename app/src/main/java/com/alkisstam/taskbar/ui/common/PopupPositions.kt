package com.alkisstam.taskbar.ui.common

import android.view.View
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

// Bottom corner sits on the anchor's top-center, opening toward the roomier side so the pressed icon stays visible.
object AboveAnchorPopupPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val centerX = anchorBounds.center.x
        val x = if (centerX > windowSize.width / 2) centerX - popupContentSize.width else centerX
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
        )
    }
}

// Centers on the host view's root rather than windowSize: overlay windows report a visible frame
// larger than the window, which pushed centered popups to the bottom-right.
class CenteredPopupPosition(private val host: View) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val root = host.rootView
        return IntOffset(
            ((root.width - popupContentSize.width) / 2).coerceAtLeast(0),
            ((root.height - popupContentSize.height) / 2).coerceAtLeast(0)
        )
    }
}
