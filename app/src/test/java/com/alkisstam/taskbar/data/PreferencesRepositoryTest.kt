package com.alkisstam.taskbar.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesRepositoryTest {

    @Test
    fun `pinned apps JSON round-trip`() {
        val pkgs = listOf("com.a", "com.b", "com.c")
        assertEquals(pkgs, PreferencesRepository.deserializePinnedApps(PreferencesRepository.serializePinnedApps(pkgs)))
    }

    @Test
    fun `pinned apps legacy delimiter format migrates`() {
        assertEquals(listOf("com.a", "com.b"), PreferencesRepository.deserializePinnedApps("com.a||com.b"))
    }

    @Test
    fun `string list round-trip`() {
        val ids = listOf("torch", "ringer", "share")
        assertEquals(ids, PreferencesRepository.deserializeStringList(PreferencesRepository.serializeStringList(ids)))
    }

    @Test
    fun `corrupt string list yields empty`() {
        assertEquals(emptyList<String>(), PreferencesRepository.deserializeStringList("garbage"))
    }
}
