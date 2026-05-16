package com.taskbar.app.viewmodel

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskbar.app.data.AppInfo
import com.taskbar.app.data.AppRepository
import com.taskbar.app.data.PillSettings
import com.taskbar.app.data.PreferencesRepository
import com.taskbar.app.data.TaskbarSettings
import com.taskbar.app.data.ThemeMode
import com.taskbar.app.service.OverlayService
import com.taskbar.app.service.TaskBarAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoHideInLandscape: StateFlow<Boolean> = prefsRepository.autoHideInLandscape
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val quickControlsStripEnabled: StateFlow<Boolean> = prefsRepository.quickControlsStripEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val surfaceTintColor: StateFlow<Long> = prefsRepository.surfaceTintColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val onboardingComplete: StateFlow<Boolean?> = prefsRepository.onboardingComplete
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun completeOnboarding() {
        viewModelScope.launch { prefsRepository.setOnboardingComplete() }
    }

    fun setSurfaceTintColor(color: Long) {
        viewModelScope.launch { prefsRepository.setSurfaceTintColor(color) }
    }

    private val _isTaskbarVisible = MutableStateFlow(true)
    val isTaskbarVisible: StateFlow<Boolean> = _isTaskbarVisible.asStateFlow()

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

    fun showTaskbar() { _isTaskbarVisible.value = true }
    fun hideTaskbar() {
        if (!_isSettingsOpen.value) _isTaskbarVisible.value = false
    }

    init {
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
        val intent = appRepository.getLaunchIntent(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
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
        val intent = Intent(context, OverlayService::class.java)
        context.startForegroundService(intent)
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

    fun setQuickControlsStripEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setQuickControlsStripEnabled(enabled)
        }
    }

    fun stopOverlay() {
        viewModelScope.launch {
            prefsRepository.setOverlayEnabled(false)
        }
        val intent = Intent(context, OverlayService::class.java)
        context.stopService(intent)
    }

}
