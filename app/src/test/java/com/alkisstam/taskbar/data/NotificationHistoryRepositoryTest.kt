package com.alkisstam.taskbar.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHistoryRepositoryTest {

    @Test
    fun `records only when quick controls are on and the history control is enabled`() {
        assertTrue(NotificationHistoryRepository.isRecordingAllowed(true, emptySet()))
        assertTrue(NotificationHistoryRepository.isRecordingAllowed(true, setOf("torch")))
        assertFalse(NotificationHistoryRepository.isRecordingAllowed(true, setOf("notif_history")))
        assertFalse(NotificationHistoryRepository.isRecordingAllowed(false, emptySet()))
    }
}
