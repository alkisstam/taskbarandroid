package com.alkisstam.taskbar.data

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaRepositoryTest {

    private lateinit var sessionManager: MediaSessionManager
    private lateinit var repo: MediaRepository
    private val component = ComponentName("com.alkisstam.taskbar", "MediaListenerService")

    @Before
    fun setUp() {
        sessionManager = mockk(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every { getSystemService(Context.MEDIA_SESSION_SERVICE) } returns sessionManager
        }
        repo = MediaRepository(context)
    }

    private fun controller(playing: Boolean, pkg: String, metadataMock: MediaMetadata? = null): MediaController =
        mockk(relaxed = true) {
            every { sessionToken } returns mockk()
            every { playbackState } returns mockk(relaxed = true) {
                every { state } returns if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            }
            every { metadata } returns metadataMock
            every { packageName } returns pkg
        }

    @Test
    fun `binds first controller when none playing`() {
        every { sessionManager.getActiveSessions(any()) } returns listOf(
            controller(playing = false, pkg = "first"),
            controller(playing = false, pkg = "second")
        )
        repo.onListenerConnected(component)
        assertTrue(repo.mediaState.value.hasSession)
        assertEquals("first", repo.mediaState.value.packageName)
        assertFalse(repo.mediaState.value.isPlaying)
    }

    @Test
    fun `prefers playing controller over first`() {
        every { sessionManager.getActiveSessions(any()) } returns listOf(
            controller(playing = false, pkg = "paused"),
            controller(playing = true, pkg = "playing")
        )
        repo.onListenerConnected(component)
        assertEquals("playing", repo.mediaState.value.packageName)
        assertTrue(repo.mediaState.value.isPlaying)
    }

    @Test
    fun `reads title and artist from metadata`() {
        val meta = mockk<MediaMetadata> {
            every { getString(MediaMetadata.METADATA_KEY_TITLE) } returns "Song"
            every { getString(MediaMetadata.METADATA_KEY_ARTIST) } returns "Artist"
            every { getBitmap(any()) } returns null
        }
        every { sessionManager.getActiveSessions(any()) } returns listOf(
            controller(playing = true, pkg = "p", metadataMock = meta)
        )
        repo.onListenerConnected(component)
        assertEquals("Song", repo.mediaState.value.title)
        assertEquals("Artist", repo.mediaState.value.artist)
    }

    @Test
    fun `session dying mid-bind resets state instead of crashing`() {
        val dead = mockk<MediaController>(relaxed = true) {
            every { sessionToken } returns mockk()
            every { playbackState } returns mockk(relaxed = true) {
                every { state } returns PlaybackState.STATE_PLAYING
            }
            every { metadata } throws IllegalStateException("session destroyed")
            every { packageName } returns "dead"
        }
        every { sessionManager.getActiveSessions(any()) } returns listOf(dead)
        repo.onListenerConnected(component)
        assertFalse(repo.mediaState.value.hasSession)
    }

    @Test
    fun `empty session list yields no-session state`() {
        every { sessionManager.getActiveSessions(any()) } returns emptyList()
        repo.onListenerConnected(component)
        assertFalse(repo.mediaState.value.hasSession)
    }

    @Test
    fun `disconnect clears state and unregisters listener`() {
        every { sessionManager.getActiveSessions(any()) } returns listOf(
            controller(playing = true, pkg = "p")
        )
        repo.onListenerConnected(component)
        assertTrue(repo.mediaState.value.hasSession)
        repo.onListenerDisconnected()
        assertFalse(repo.mediaState.value.hasSession)
        verify { sessionManager.removeOnActiveSessionsChangedListener(any()) }
    }
}
