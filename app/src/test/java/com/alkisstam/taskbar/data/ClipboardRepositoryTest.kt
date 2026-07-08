package com.alkisstam.taskbar.data

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardRepositoryTest {

    private val repo = ClipboardRepository(mockk(relaxed = true))

    @Test
    fun `clips round-trip through JSON`() {
        val clips = listOf(
            ClipItem("id1", ClipType.TEXT, "hello", "AppA", 111L, isPinned = true),
            ClipItem("id2", ClipType.PDF, "/path/f.pdf", "AppB", 222L, isFavorite = true, fileName = "f.pdf"),
            ClipItem("id3", ClipType.URL, "https://x.y", "AppC", 333L)
        )
        assertEquals(clips, repo.deserializeClips(repo.serializeClips(clips)))
    }

    @Test
    fun `notes round-trip through JSON`() {
        val notes = listOf(
            NoteItem("n1", "note one", 1L, isPinned = true),
            NoteItem("n2", "note two", 2L, isFavorite = true)
        )
        assertEquals(notes, repo.deserializeNotes(repo.serializeNotes(notes)))
    }

    @Test
    fun `todos round-trip through JSON`() {
        val todos = listOf(
            TodoItem("t1", "buy milk", 1L, isDone = true),
            TodoItem("t2", "ship app", 2L)
        )
        assertEquals(todos, repo.deserializeTodos(repo.serializeTodos(todos)))
    }

    @Test
    fun `corrupt clip record skipped, valid ones kept`() {
        val json = """[{"id":"broken"},{"id":"ok","type":"TEXT","content":"c","sourceApp":"s","timestamp":5}]"""
        val result = repo.deserializeClips(json)
        assertEquals(1, result.size)
        assertEquals("ok", result[0].id)
    }

    @Test
    fun `unknown clip type skipped instead of crashing`() {
        val json = """[{"id":"x","type":"HOLOGRAM","content":"c","sourceApp":"s","timestamp":5}]"""
        assertTrue(repo.deserializeClips(json).isEmpty())
    }

    @Test
    fun `garbage input yields empty lists`() {
        assertTrue(repo.deserializeClips("not json").isEmpty())
        assertTrue(repo.deserializeNotes("not json").isEmpty())
        assertTrue(repo.deserializeTodos("not json").isEmpty())
    }
}
