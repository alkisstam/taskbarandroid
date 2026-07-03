package com.alkisstam.taskbar.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.AppRepository
import com.alkisstam.taskbar.data.MediaRepository
import com.alkisstam.taskbar.data.MediaState
import com.alkisstam.taskbar.data.PreferencesRepository
import com.alkisstam.taskbar.data.QuickControlsRepository
import com.alkisstam.taskbar.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppMenuViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var audioManager: AudioManager
    private lateinit var appRepo: AppRepository
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var quickControls: QuickControlsRepository
    private lateinit var mediaRepo: MediaRepository

    @Before
    fun setUp() {
        audioManager = mockk<AudioManager>(relaxed = true) {
            every { ringerMode } returns AudioManager.RINGER_MODE_NORMAL
        }
        context = mockk<Context>(relaxed = true) {
            every { getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        }
        appRepo = mockk<AppRepository>(relaxed = true) {
            every { apps } returns MutableStateFlow(emptyList())
        }
        prefsRepo = mockk<PreferencesRepository>(relaxed = true) {
            every { pinnedApps } returns flowOf(emptyList())
            every { musicPanelOpen } returns flowOf(false)
        }
        quickControls = mockk<QuickControlsRepository>(relaxed = true) {
            every { getRingerMode() } returns AudioManager.RINGER_MODE_NORMAL
            every { canSetSilent() } returns true
        }
        mediaRepo = mockk<MediaRepository>(relaxed = true) {
            every { mediaState } returns MutableStateFlow(MediaState())
        }
    }

    private fun createViewModel() = AppMenuViewModel(context, appRepo, prefsRepo, quickControls, mediaRepo)

    // region Volume / brightness panel mutual exclusion

    @Test
    fun `toggleVolumePanel shows volume panel`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        vm.toggleVolumePanel()
        assertTrue(vm.volumePanelVisible.value)
    }

    @Test
    fun `toggleVolumePanel twice hides the panel`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        vm.toggleVolumePanel()
        vm.toggleVolumePanel()
        assertFalse(vm.volumePanelVisible.value)
    }

    @Test
    fun `toggleVolumePanel dismisses brightness panel`() = runTest(mainDispatcherRule.dispatcher) {
        every { quickControls.getBrightness() } returns 128
        val vm = createViewModel()
        vm.toggleBrightnessPanel()
        assertTrue(vm.brightnessPanelVisible.value)
        vm.toggleVolumePanel()
        assertFalse(vm.brightnessPanelVisible.value)
        assertTrue(vm.volumePanelVisible.value)
    }

    @Test
    fun `toggleBrightnessPanel shows brightness panel`() = runTest(mainDispatcherRule.dispatcher) {
        every { quickControls.getBrightness() } returns 128
        val vm = createViewModel()
        vm.toggleBrightnessPanel()
        assertTrue(vm.brightnessPanelVisible.value)
    }

    @Test
    fun `toggleBrightnessPanel dismisses volume panel`() = runTest(mainDispatcherRule.dispatcher) {
        every { quickControls.getBrightness() } returns 128
        val vm = createViewModel()
        vm.toggleVolumePanel()
        assertTrue(vm.volumePanelVisible.value)
        vm.toggleBrightnessPanel()
        assertFalse(vm.volumePanelVisible.value)
        assertTrue(vm.brightnessPanelVisible.value)
    }

    @Test
    fun `dismissVolumePanel hides the panel`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        vm.toggleVolumePanel()
        vm.dismissVolumePanel()
        assertFalse(vm.volumePanelVisible.value)
    }

    @Test
    fun `dismissBrightnessPanel hides the panel`() = runTest(mainDispatcherRule.dispatcher) {
        every { quickControls.getBrightness() } returns 128
        val vm = createViewModel()
        vm.toggleBrightnessPanel()
        vm.dismissBrightnessPanel()
        assertFalse(vm.brightnessPanelVisible.value)
    }

    // endregion

    // region Menu and search

    @Test
    fun `dismissMenu also closes search`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        vm.openSearch()
        assertTrue(vm.isSearching.value)
        vm.dismissMenu()
        assertFalse(vm.isSearching.value)
        assertFalse(vm.menuVisible.value)
    }

    @Test
    fun `openSearch sets isSearching and closes menu`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        vm.toggleMenu()
        assertTrue(vm.menuVisible.value)
        vm.openSearch()
        assertTrue(vm.isSearching.value)
        assertFalse(vm.menuVisible.value)
    }

    @Test
    fun `closeSearch clears query and stops searching`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        vm.openSearch()
        vm.setSearchQuery("test")
        assertEquals("test", vm.searchQuery.value)
        vm.closeSearch()
        assertEquals("", vm.searchQuery.value)
        assertFalse(vm.isSearching.value)
    }

    // endregion

    // region cycleRingerMode

    @Test
    fun `cycleRingerMode goes NORMAL to VIBRATE`() = runTest(mainDispatcherRule.dispatcher) {
        every { quickControls.getRingerMode() } returns AudioManager.RINGER_MODE_NORMAL
        val vm = createViewModel()
        vm.refreshQuickControls()

        vm.cycleRingerMode()

        assertEquals(AudioManager.RINGER_MODE_VIBRATE, vm.quickControlsState.value.ringerMode)
        verify { quickControls.setRingerMode(AudioManager.RINGER_MODE_VIBRATE) }
    }

    @Test
    fun `cycleRingerMode goes VIBRATE to SILENT when canSetSilent`() = runTest(mainDispatcherRule.dispatcher) {
        every { quickControls.getRingerMode() } returns AudioManager.RINGER_MODE_VIBRATE
        every { quickControls.canSetSilent() } returns true
        val vm = createViewModel()
        vm.refreshQuickControls()

        vm.cycleRingerMode()

        assertEquals(AudioManager.RINGER_MODE_SILENT, vm.quickControlsState.value.ringerMode)
        verify { quickControls.setRingerMode(AudioManager.RINGER_MODE_SILENT) }
    }

    @Test
    fun `cycleRingerMode goes VIBRATE to NORMAL when cannot set silent`() = runTest(mainDispatcherRule.dispatcher) {
        every { quickControls.getRingerMode() } returns AudioManager.RINGER_MODE_VIBRATE
        every { quickControls.canSetSilent() } returns false
        val vm = createViewModel()
        vm.refreshQuickControls()

        vm.cycleRingerMode()

        assertEquals(AudioManager.RINGER_MODE_NORMAL, vm.quickControlsState.value.ringerMode)
        verify { quickControls.setRingerMode(AudioManager.RINGER_MODE_NORMAL) }
    }

    @Test
    fun `cycleRingerMode goes SILENT to NORMAL`() = runTest(mainDispatcherRule.dispatcher) {
        every { quickControls.getRingerMode() } returns AudioManager.RINGER_MODE_SILENT
        val vm = createViewModel()
        vm.refreshQuickControls()

        vm.cycleRingerMode()

        assertEquals(AudioManager.RINGER_MODE_NORMAL, vm.quickControlsState.value.ringerMode)
        verify { quickControls.setRingerMode(AudioManager.RINGER_MODE_NORMAL) }
    }

    // endregion

    // region filteredApps

    @Test
    fun `filteredApps returns all apps when query is empty`() = runTest(mainDispatcherRule.dispatcher) {
        val icon = mockk<Bitmap>()
        every { appRepo.apps } returns MutableStateFlow(listOf(
            AppInfo("com.a", "Alpha", icon),
            AppInfo("com.b", "Beta", icon),
        ))

        val vm = createViewModel()
        val collected = mutableListOf<List<AppInfo>>()
        val job = launch { vm.filteredApps.collect { collected.add(it) } }
        advanceUntilIdle()

        assertEquals(2, collected.last().size)
        job.cancel()
    }

    @Test
    fun `filteredApps filters by label case-insensitively`() = runTest(mainDispatcherRule.dispatcher) {
        val icon = mockk<Bitmap>()
        every { appRepo.apps } returns MutableStateFlow(listOf(
            AppInfo("com.a", "Alpha", icon),
            AppInfo("com.b", "Beta", icon),
            AppInfo("com.c", "ALPHABET", icon),
        ))

        val vm = createViewModel()
        val collected = mutableListOf<List<AppInfo>>()
        val job = launch { vm.filteredApps.collect { collected.add(it) } }
        advanceUntilIdle()

        vm.setSearchQuery("alph")
        advanceUntilIdle()

        val result = collected.last()
        assertEquals(2, result.size)
        assertTrue(result.any { it.packageName == "com.a" })
        assertTrue(result.any { it.packageName == "com.c" })
        job.cancel()
    }

    @Test
    fun `filteredApps returns empty list when no match`() = runTest(mainDispatcherRule.dispatcher) {
        val icon = mockk<Bitmap>()
        every { appRepo.apps } returns MutableStateFlow(listOf(AppInfo("com.a", "Alpha", icon)))

        val vm = createViewModel()
        val collected = mutableListOf<List<AppInfo>>()
        val job = launch { vm.filteredApps.collect { collected.add(it) } }
        advanceUntilIdle()

        vm.setSearchQuery("zzz")
        advanceUntilIdle()

        assertTrue(collected.last().isEmpty())
        job.cancel()
    }

    // endregion
}
