package com.alkisstam.taskbar.service

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardShareActivityTest {

    private fun uri(scheme: String?, authority: String?): Uri = mockk {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.authority } returns authority
    }

    @Test
    fun `file uri is rejected`() {
        assertFalse(ClipboardShareActivity.isAllowedStream(uri("file", null), "com.alkisstam.taskbar"))
    }

    @Test
    fun `own fileprovider uri is rejected`() {
        assertFalse(
            ClipboardShareActivity.isAllowedStream(
                uri("content", "com.alkisstam.taskbar.fileprovider"), "com.alkisstam.taskbar"
            )
        )
    }

    @Test
    fun `other app content uri is allowed`() {
        assertTrue(
            ClipboardShareActivity.isAllowedStream(
                uri("content", "com.google.android.apps.photos.contentprovider"), "com.alkisstam.taskbar"
            )
        )
    }
}
