package com.taskbar.app.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskbar.app.data.AppInfo
import com.taskbar.app.data.AppRepository
import com.taskbar.app.data.PreferencesRepository
import com.taskbar.app.data.QuickControlsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class QuickControlItemData(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val active: Boolean
)

data class QuickControlsState(
    val torchOn: Boolean = false,
    val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
    val autoRotate: Boolean = false,
    val autoBrightness: Boolean = false,
    val canWriteSettings: Boolean = false,
    val hasTorch: Boolean = false,
    val canSetSilent: Boolean = false,
    val dndEnabled: Boolean = false,
    val dndPermissionGranted: Boolean = false,
    val canShowPowerMenu: Boolean = false
)

private const val TAG = "AppMenuViewModel"

@HiltViewModel
class AppMenuViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val prefsRepository: PreferencesRepository,
    private val quickControls: QuickControlsRepository
) : ViewModel() {

    val allApps: StateFlow<List<AppInfo>> = appRepository.apps

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(appRepository.apps, _searchQuery) { apps, query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quickControlsState = MutableStateFlow(QuickControlsState())
    val quickControlsState: StateFlow<QuickControlsState> = _quickControlsState.asStateFlow()

    val pinnedPackages: StateFlow<List<String>> = prefsRepository.pinnedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickControlsStripEnabled: StateFlow<Boolean> = prefsRepository.quickControlsStripEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _menuVisible = MutableStateFlow(false)
    val menuVisible: StateFlow<Boolean> = _menuVisible.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    fun openSearch() {
        _isSearching.value = true
        _menuVisible.value = false
    }

    fun closeSearch() {
        _isSearching.value = false
        _searchQuery.value = ""
    }

    init {
        refreshQuickControls()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleMenu() {
        _menuVisible.value = !_menuVisible.value
        if (_menuVisible.value) refreshQuickControls()
    }

    fun dismissMenu() {
        _menuVisible.value = false
        closeSearch()
    }

    fun launchApp(packageName: String) {
        val intent = appRepository.getLaunchIntent(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        _menuVisible.value = false
        closeSearch()
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch { prefsRepository.pinApp(packageName) }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch { prefsRepository.unpinApp(packageName) }
    }

    fun refreshQuickControls() {
        _quickControlsState.value = QuickControlsState(
            torchOn = quickControls.getTorchState(),
            ringerMode = quickControls.getRingerMode(),
            autoRotate = quickControls.isAutoRotateEnabled(),
            autoBrightness = quickControls.isAutoBrightnessEnabled(),
            canWriteSettings = quickControls.canWriteSettings(),
            hasTorch = quickControls.hasTorch(),
            canSetSilent = quickControls.canSetSilent(),
            dndEnabled = quickControls.isDndEnabled(),
            dndPermissionGranted = quickControls.isDndPermissionGranted(),
            canShowPowerMenu = quickControls.canShowPowerMenu()
        )
    }

    fun toggleTorch() {
        val newState = !_quickControlsState.value.torchOn
        try {
            quickControls.setTorch(newState)
            _quickControlsState.value = _quickControlsState.value.copy(torchOn = newState)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle torch", e)
        }
    }

    fun cycleRingerMode() {
        val current = _quickControlsState.value.ringerMode
        val canSilent = _quickControlsState.value.canSetSilent
        val next = when (current) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> if (canSilent) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        quickControls.setRingerMode(next)
        _quickControlsState.value = _quickControlsState.value.copy(ringerMode = next)
    }

    fun toggleAutoRotate() {
        if (!_quickControlsState.value.canWriteSettings) return
        val newState = !_quickControlsState.value.autoRotate
        quickControls.setAutoRotate(newState)
        _quickControlsState.value = _quickControlsState.value.copy(autoRotate = newState)
    }

    fun toggleAutoBrightness() {
        if (!_quickControlsState.value.canWriteSettings) return
        val newState = !_quickControlsState.value.autoBrightness
        quickControls.setAutoBrightness(newState)
        _quickControlsState.value = _quickControlsState.value.copy(autoBrightness = newState)
    }

    fun toggleDnd() {
        quickControls.toggleDnd()
        _quickControlsState.value = _quickControlsState.value.copy(
            dndEnabled = quickControls.isDndEnabled()
        )
    }

    fun openQrScanner() {
        quickControls.openQrScanner()
        _menuVisible.value = false
    }

    fun showPowerMenu() {
        quickControls.showPowerMenu()
        _menuVisible.value = false
    }

    fun handleQuickControlAction(id: String) {
        when (id) {
            "torch" -> toggleTorch()
            "ringer" -> cycleRingerMode()
            "rotate" -> toggleAutoRotate()
            "brightness" -> toggleAutoBrightness()
            "dnd" -> toggleDnd()
            "qr" -> openQrScanner()
            "power" -> showPowerMenu()
        }
    }
}
