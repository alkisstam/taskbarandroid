package com.alkisstam.taskbar.data

import android.content.Context
import com.alkisstam.taskbar.ui.clipboard.readTextPrefix
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.io.InputStream

class ClipboardRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private val repo = ClipboardRepository(mockk(relaxed = true))

    private fun repoWithFiles() = ClipboardRepository(mockk<Context> { every { filesDir } returns tmp.root })

    @Test
    fun `oversized share is rejected and its partial file deleted`() {
        val repo = repoWithFiles()
        var left = 50L * 1024 * 1024 + 1
        val huge = object : InputStream() {
            override fun read(): Int = if (left-- > 0) 0 else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (left <= 0) return -1
                val n = minOf(len.toLong(), left).toInt()
                left -= n
                return n
            }
        }
        try {
            repo.copyStreamToStorage(huge, "txt", "big")
            fail("expected IOException")
        } catch (e: IOException) {
            assertFalse(tmp.root.resolve("clipboard/big.txt").exists())
        }
    }

    @Test
    fun `file clips without their file are hidden, text clips kept`() {
        val present = tmp.newFile("a.pdf").absolutePath
        assertTrue(repo.hasContent(ClipItem("1", ClipType.PDF, present, "A", 1L)))
        assertFalse(repo.hasContent(ClipItem("2", ClipType.IMAGE, "/missing/x.png", "A", 1L)))
        assertTrue(repo.hasContent(ClipItem("3", ClipType.TEXT, "hello", "A", 1L)))
        assertTrue(repo.hasContent(ClipItem("4", ClipType.URL, "https://x.y", "A", 1L)))
    }

    @Test
    fun `text prefix reads only the requested chars`() {
        val f = tmp.newFile("t.txt").apply { writeText("x".repeat(100_000)) }
        assertEquals(300, readTextPrefix(f.path, 300).length)
        assertEquals("hi", readTextPrefix(tmp.newFile("s.txt").apply { writeText("hi") }.path, 300))
    }

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
    fun `pruneFile deletes stored file for every file-backed clip type`() {
        ClipType.values().filter { it != ClipType.TEXT && it != ClipType.URL }.forEach { type ->
            val file = java.io.File.createTempFile("clip", ".bin")
            repo.pruneFile(ClipItem("id", type, file.absolutePath, "App", 1L))
            assertFalse("$type file not deleted", file.exists())
        }
    }

    @Test
    fun `pruneFile leaves inline clips alone`() {
        val file = java.io.File.createTempFile("clip", ".bin")
        repo.pruneFile(ClipItem("id", ClipType.TEXT, file.absolutePath, "App", 1L))
        assertTrue(file.exists())
        file.delete()
    }

    @Test
    fun `garbage input yields empty lists`() {
        assertTrue(repo.deserializeClips("not json").isEmpty())
        assertTrue(repo.deserializeNotes("not json").isEmpty())
        assertTrue(repo.deserializeTodos("not json").isEmpty())
    }
}
