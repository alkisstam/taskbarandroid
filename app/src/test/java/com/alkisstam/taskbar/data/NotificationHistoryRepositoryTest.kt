package com.alkisstam.taskbar.data

import org.junit.Assert.assertEquals
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

    @Test
    fun `corrupt history is detected so it can be kept before the next write`() {
        val good = """{"id":"a","packageName":"p","appLabel":"P","title":"t","text":"x","timestamp":1}"""
        assertFalse(NotificationHistoryRepository.hasCorruptRecords("[$good]"))
        assertTrue(NotificationHistoryRepository.hasCorruptRecords("""[$good,{"id":"b"}]"""))
        assertTrue(NotificationHistoryRepository.hasCorruptRecords("not json"))
        assertFalse(NotificationHistoryRepository.hasCorruptRecords("[]"))
    }

    @Test
    fun `expanded text is used when the short text is blank`() {
        assertEquals("long", NotificationHistoryRepository.notificationText("", "long"))
        assertEquals("short", NotificationHistoryRepository.notificationText("short", "long"))
        assertEquals("", NotificationHistoryRepository.notificationText(null, null))
    }
}
