package com.alkisstam.taskbar.viewmodel

import android.content.Context
import android.graphics.Bitmap
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.AppRepository
import com.alkisstam.taskbar.data.PillSettings
import com.alkisstam.taskbar.data.PreferencesRepository
import com.alkisstam.taskbar.data.TaskbarSettings
import com.alkisstam.taskbar.data.ThemeMode
import com.alkisstam.taskbar.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
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
class TaskbarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var appRepo: AppRepository
    private lateinit var prefsRepo: PreferencesRepository

    @Before
    fun setUp() {
        context = mockk<Context>(relaxed = true)
        appRepo = mockk<AppRepository>(relaxed = true) {
            every { apps } returns MutableStateFlow(emptyList())
        }
        prefsRepo = mockk<PreferencesRepository>(relaxed = true) {
            every { pinnedApps } returns flowOf(emptyList())
            every { themeMode } returns flowOf(ThemeMode.SYSTEM)
            every { overlayEnabled } returns flowOf(false)
            every { pillSettings } returns flowOf(PillSettings())
            every { taskbarSettings } returns flowOf(TaskbarSettings())
            every { autoHideInFullscreen } returns flowOf(false)
            every { autoHideInLandscape } returns flowOf(false)
            every { quickControlsEnabled } returns flowOf(true)
            every { controlsOrder } returns flowOf(PreferencesRepository.ALL_CONTROL_IDS)
            every { controlsDisabledIds } returns flowOf(emptySet())
            every { surfaceTintColor } returns flowOf(0L)
            every { onboardingComplete } returns flowOf(true)
            every { musicPanelEnabled } returns flowOf(false)
            every { taskbarVisible } returns flowOf(true)
        }
    }

    private fun createViewModel() = TaskbarViewModel(context, appRepo, prefsRepo)

    @Test
    fun `showTaskbar sets isTaskbarVisible to true`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        vm.hideTaskbar()
        assertFalse(vm.isTaskbarVisible.value)
        vm.showTaskbar()
        assertTrue(vm.isTaskbarVisible.value)
    }

    @Test
    fun `hideTaskbar sets isTaskbarVisible to false when settings not open`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        assertTrue(vm.isTaskbarVisible.value)
        vm.hideTaskbar()
        assertFalse(vm.isTaskbarVisible.value)
    }

    @Test
    fun `hideTaskbar hides dock even when settings open`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        vm.setSettingsOpen(true)
        vm.hideTaskbar()
        assertFalse(vm.isTaskbarVisible.value)
    }

    @Test
    fun `setSettingsOpen updates isSettingsOpen`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = createViewModel()
        assertFalse(vm.isSettingsOpen.value)
        vm.setSettingsOpen(true)
        assertTrue(vm.isSettingsOpen.value)
        vm.setSettingsOpen(false)
        assertFalse(vm.isSettingsOpen.value)
    }

    @Test
    fun `pinnedApps preserves pinned order and drops unknown packages`() = runTest(mainDispatcherRule.dispatcher) {
        val icon = mockk<Bitmap>()
        val apps = listOf(
            AppInfo("com.a", "App A", icon),
            AppInfo("com.b", "App B", icon),
        )
        every { appRepo.apps } returns MutableStateFlow(apps)
        every { prefsRepo.pinnedApps } returns flowOf(listOf("com.b", "com.a", "com.c"))

        val vm = createViewModel()

        val collected = mutableListOf<List<AppInfo>>()
        val job = launch { vm.pinnedApps.collect { collected.add(it) } }
        advanceUntilIdle()

        assertEquals(listOf("com.b", "com.a"), collected.last().map { it.packageName })
        job.cancel()
    }

    @Test
    fun `pinnedApps is empty when no apps are installed`() = runTest(mainDispatcherRule.dispatcher) {
        every { appRepo.apps } returns MutableStateFlow(emptyList())
        every { prefsRepo.pinnedApps } returns flowOf(listOf("com.a", "com.b"))

        val vm = createViewModel()

        val collected = mutableListOf<List<AppInfo>>()
        val job = launch { vm.pinnedApps.collect { collected.add(it) } }
        advanceUntilIdle()

        assertTrue(collected.last().isEmpty())
        job.cancel()
    }
}
