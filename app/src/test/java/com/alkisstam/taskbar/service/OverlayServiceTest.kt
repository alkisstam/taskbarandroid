package com.alkisstam.taskbar.service

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayServiceTest {

    @Test
    fun `pinned dock hides under keyboard or shade and returns when they close`() {
        assertEquals(true, OverlayService.systemUiAutoHideAction(pinned = true, obscured = true, forceHidden = false))
        assertEquals(false, OverlayService.systemUiAutoHideAction(pinned = true, obscured = false, forceHidden = true))
    }

    @Test
    fun `dock unpinned while force-hidden is not brought back`() {
        assertEquals(null, OverlayService.systemUiAutoHideAction(pinned = false, obscured = false, forceHidden = true))
    }

    @Test
    fun `no change means no action`() {
        assertEquals(null, OverlayService.systemUiAutoHideAction(pinned = true, obscured = true, forceHidden = true))
        assertEquals(null, OverlayService.systemUiAutoHideAction(pinned = false, obscured = true, forceHidden = false))
    }
}
