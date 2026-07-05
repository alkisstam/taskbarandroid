package com.alkisstam.taskbar.viewmodel

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.AppRepository
import com.alkisstam.taskbar.data.LaunchMode
import com.alkisstam.taskbar.data.PillSettings
import com.alkisstam.taskbar.data.PreferencesRepository
import com.alkisstam.taskbar.data.TaskbarSettings
import com.alkisstam.taskbar.data.ThemeMode
import com.alkisstam.taskbar.service.OverlayService
import com.alkisstam.taskbar.service.TaskBarAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskbarViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val prefsRepository: PreferencesRepository
) : ViewModel() {

    val allApps: StateFlow<List<AppInfo>> = appRepository.apps

    val pinnedApps: StateFlow<List<AppInfo>> = combine(
        prefsRepository.pinnedApps,
        appRepository.apps
    ) { pinnedPackages, apps ->
        val appMap = apps.associateBy { it.packageName }
        pinnedPackages.mapNotNull { pkg -> appMap[pkg] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<ThemeMode> = prefsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val overlayEnabled: StateFlow<Boolean> = prefsRepository.overlayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val pillSettings: StateFlow<PillSettings> = prefsRepository.pillSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PillSettings())

    val taskbarSettings: StateFlow<TaskbarSettings> = prefsRepository.taskbarSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskbarSettings())

    val autoHideInFullscreen: StateFlow<Boolean> = prefsRepository.autoHideInFullscreen
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoHideInLandscape: StateFlow<Boolean> = prefsRepository.autoHideInLandscape
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val quickControlsEnabled: StateFlow<Boolean> = prefsRepository.quickControlsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val controlsOrder: StateFlow<List<String>> = prefsRepository.controlsOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesRepository.ALL_CONTROL_IDS)

    val controlsDisabledIds: StateFlow<Set<String>> = prefsRepository.controlsDisabledIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val surfaceTintColor: StateFlow<Long> = prefsRepository.surfaceTintColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val onboardingComplete: StateFlow<Boolean?> = prefsRepository.onboardingComplete
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastSeenVersionCode: StateFlow<Int?> = prefsRepository.lastSeenVersionCode
        .map { it as Int? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val musicPanelEnabled: StateFlow<Boolean> = prefsRepository.musicPanelEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val hapticFeedbackEnabled: StateFlow<Boolean> = prefsRepository.hapticFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val panelOutlineEnabled: StateFlow<Boolean> = prefsRepository.panelOutlineEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val translucentMode: StateFlow<Boolean> = prefsRepository.translucentMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val translucentAlpha: StateFlow<Float> = prefsRepository.translucentAlpha
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.80f)

    val appGridColumns: StateFlow<Int> = prefsRepository.appGridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val appGridRows: StateFlow<Int> = prefsRepository.appGridRows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setHapticFeedbackEnabled(enabled)
        }
    }

    fun setPanelOutlineEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setPanelOutlineEnabled(enabled) }
    }

    fun setTranslucentMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setTranslucentMode(enabled)
            if (enabled) prefsRepository.setPanelOutlineEnabled(false)
        }
    }

    fun setTranslucentAlpha(value: Float) {
        viewModelScope.launch { prefsRepository.setTranslucentAlpha(value) }
    }

    fun setAppGridColumns(value: Int) {
        viewModelScope.launch { prefsRepository.setAppGridColumns(value) }
    }

    fun setAppGridRows(value: Int) {
        viewModelScope.launch { prefsRepository.setAppGridRows(value) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefsRepository.setOnboardingComplete() }
    }

    fun markVersionSeen(code: Int) {
        viewModelScope.launch { prefsRepository.setLastSeenVersionCode(code) }
    }

    fun setSurfaceTintColor(color: Long) {
        viewModelScope.launch { prefsRepository.setSurfaceTintColor(color) }
    }

    private val _isTaskbarVisible = MutableStateFlow(false)
    val isTaskbarVisible: StateFlow<Boolean> = _isTaskbarVisible.asStateFlow()

    private val _isDockExpanded = MutableStateFlow(false)
    val isDockExpanded: StateFlow<Boolean> = _isDockExpanded.asStateFlow()

    fun toggleDockExpanded() {
        val next = !_isDockExpanded.value
        _isDockExpanded.value = next
        _dockExpandProgress.value = if (next) 1f else 0f
    }
    fun collapseDock() {
        _isDockExpanded.value = false
        _dockExpandProgress.value = 0f
    }

    private val _dockExpandProgress = MutableStateFlow(0f)
    val dockExpandProgress: StateFlow<Float> = _dockExpandProgress.asStateFlow()

    fun setExpandProgress(p: Float) { _dockExpandProgress.value = p.coerceIn(0f, 1f) }
    fun cancelExpandReveal() { _dockExpandProgress.value = 0f }
    fun cancelCollapseReveal() { _dockExpandProgress.value = 1f }

    private val _dockRevealProgress = MutableStateFlow(0f)
    val dockRevealProgress: StateFlow<Float> = _dockRevealProgress.asStateFlow()

    fun setRevealProgress(p: Float) { _dockRevealProgress.value = p.coerceIn(0f, 1f) }
    fun cancelReveal() { _dockRevealProgress.value = 0f }

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    fun setSettingsOpen(open: Boolean) { _isSettingsOpen.value = open }

    private val _isAccessibilityEnabled = MutableStateFlow(checkAccessibilityEnabled())
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val accessibilityObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            _isAccessibilityEnabled.value = checkAccessibilityEnabled()
        }
    }

    fun showTaskbar() {
        _isTaskbarVisible.value = true
        viewModelScope.launch { prefsRepository.setTaskbarVisible(true) }
    }
    fun hideTaskbar() {
        _isDockExpanded.value = false
        _dockExpandProgress.value = 0f
        _isTaskbarVisible.value = false
        _dockRevealProgress.value = 0f
        viewModelScope.launch { prefsRepository.setTaskbarVisible(false) }
    }

    init {
        viewModelScope.launch {
            _isTaskbarVisible.value = prefsRepository.taskbarVisible.first()
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            accessibilityObserver
        )
    }

    override fun onCleared() {
        super.onCleared()
        context.contentResolver.unregisterContentObserver(accessibilityObserver)
    }

    private fun checkAccessibilityEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val componentName = "${context.packageName}/${TaskBarAccessibilityService::class.java.name}"
        return flat.split(':').any { TextUtils.equals(it.trim(), componentName) }
    }

    fun launchApp(packageName: String) {
        appRepository.launchApp(packageName, LaunchMode.NORMAL)
    }

    fun launchAppSplit(packageName: String) {
        appRepository.launchApp(packageName, LaunchMode.SPLIT_SCREEN)
    }

    fun launchAppFloating(packageName: String) {
        appRepository.launchApp(packageName, LaunchMode.FLOATING)
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch {
            prefsRepository.pinApp(packageName)
        }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch {
            prefsRepository.unpinApp(packageName)
        }
    }

    fun reorderPinnedApps(newOrder: List<String>) {
        viewModelScope.launch {
            prefsRepository.savePinnedApps(newOrder)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepository.setThemeMode(mode)
        }
    }

    fun startOverlay() {
        viewModelScope.launch {
            prefsRepository.setOverlayEnabled(true)
        }
        try {
            context.startForegroundService(Intent(context, OverlayService::class.java))
        } catch (e: Exception) {
            Log.w("TaskbarViewModel", "Failed to start overlay service", e)
        }
    }

    fun savePillSettings(settings: PillSettings) {
        viewModelScope.launch {
            prefsRepository.savePillSettings(settings)
        }
    }

    fun saveTaskbarSettings(settings: TaskbarSettings) {
        viewModelScope.launch {
            prefsRepository.saveTaskbarSettings(settings)
        }
    }

    fun setAutoHideInFullscreen(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setAutoHideInFullscreen(enabled)
        }
    }

    fun setAutoHideInLandscape(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setAutoHideInLandscape(enabled)
        }
    }

    fun setQuickControlsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setQuickControlsEnabled(enabled)
        }
    }

    fun saveControlsOrder(order: List<String>) {
        viewModelScope.launch {
            prefsRepository.saveControlsOrder(order)
        }
    }

    fun saveControlsDisabledIds(ids: Set<String>) {
        viewModelScope.launch {
            prefsRepository.saveControlsDisabledIds(ids)
        }
    }

    fun setMusicPanelEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setMusicPanelEnabled(enabled)
        }
    }

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    fun updateBattery(level: Int, charging: Boolean) {
        _batteryLevel.value = level
        _isCharging.value = charging
    }

    fun stopOverlay() {
        viewModelScope.launch {
            prefsRepository.setOverlayEnabled(false)
        }
        val intent = Intent(context, OverlayService::class.java)
        context.stopService(intent)
    }

    private val _backupError = MutableStateFlow<String?>(null)
    val backupError: StateFlow<String?> = _backupError.asStateFlow()

    fun clearBackupError() {
        _backupError.value = null
    }

    fun exportBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val json = prefsRepository.exportToJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }
            } catch (e: Exception) {
                Log.w("TaskbarViewModel", "Failed to export backup", e)
                _backupError.value = "Failed to export backup"
            }
        }
    }

    fun importBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                } ?: return@launch
                prefsRepository.importFromJson(json)
            } catch (e: Exception) {
                Log.w("TaskbarViewModel", "Failed to import backup", e)
                _backupError.value = "Failed to restore backup: invalid file"
            }
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            prefsRepository.resetAllSettings()
        }
    }

}
