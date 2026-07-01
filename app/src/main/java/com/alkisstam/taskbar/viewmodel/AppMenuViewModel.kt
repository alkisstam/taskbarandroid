package com.alkisstam.taskbar.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.alkisstam.taskbar.ui.appmenu.VolumeStreamInfo
import com.alkisstam.taskbar.ui.appmenu.buildVolumeStreams
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alkisstam.taskbar.data.AppInfo
import com.alkisstam.taskbar.data.AppRepository
import com.alkisstam.taskbar.data.MediaRepository
import com.alkisstam.taskbar.data.MediaState
import com.alkisstam.taskbar.data.PreferencesRepository
import com.alkisstam.taskbar.data.QuickControlsChangeListener
import com.alkisstam.taskbar.data.QuickControlsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val canShowPowerMenu: Boolean = false,
    val canTakeScreenshot: Boolean = false,
    val canLockScreen: Boolean = false,
    val caffeineMinutes: Int = 0
)

private const val TAG = "AppMenuViewModel"

private fun fuzzyScore(label: String, query: String): Int? {
    val l = label.lowercase()
    val q = query.trim().lowercase()
    if (q.isEmpty()) return 0
    val idx = l.indexOf(q)
    if (idx >= 0) return idx
    var qi = 0
    var score = 1000
    var lastMatch = -1
    for (li in l.indices) {
        if (qi < q.length && l[li] == q[qi]) {
            score += if (lastMatch == li - 1) 0 else 2
            lastMatch = li
            qi++
        }
    }
    return if (qi == q.length) score else null
}

@HiltViewModel
class AppMenuViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val prefsRepository: PreferencesRepository,
    private val quickControls: QuickControlsRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val allApps: StateFlow<List<AppInfo>> = appRepository.apps

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(appRepository.apps, _searchQuery) { apps, query ->
        val q = query.trim()
        if (q.isEmpty()) apps
        else apps.mapNotNull { app -> fuzzyScore(app.label, q)?.let { app to it } }
            .sortedBy { it.second }
            .map { it.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quickControlsState = MutableStateFlow(QuickControlsState())
    val quickControlsState: StateFlow<QuickControlsState> = _quickControlsState.asStateFlow()

    val pinnedPackages: StateFlow<List<String>> = prefsRepository.pinnedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _menuVisible = MutableStateFlow(false)
    val menuVisible: StateFlow<Boolean> = _menuVisible.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _volumePanelVisible = MutableStateFlow(false)
    val volumePanelVisible: StateFlow<Boolean> = _volumePanelVisible.asStateFlow()

    private val _brightnessPanelVisible = MutableStateFlow(false)
    val brightnessPanelVisible: StateFlow<Boolean> = _brightnessPanelVisible.asStateFlow()

    private val _musicPanelVisible = MutableStateFlow(false)
    val musicPanelVisible: StateFlow<Boolean> = _musicPanelVisible.asStateFlow()
    private var _musicWasVisibleBeforeSlider = false

    private val _clipboardPanelVisible = MutableStateFlow(false)
    val clipboardPanelVisible: StateFlow<Boolean> = _clipboardPanelVisible.asStateFlow()

    val mediaState: StateFlow<MediaState> = mediaRepository.mediaState

    private val _brightnessLevel = MutableStateFlow(128)
    val brightnessLevel: StateFlow<Int> = _brightnessLevel.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalScreenTimeoutMs: Int = -1

    private val _volumeStreams = MutableStateFlow<List<VolumeStreamInfo>>(emptyList())
    val volumeStreams: StateFlow<List<VolumeStreamInfo>> = _volumeStreams.asStateFlow()

    private fun refreshVolumeStreams() {
        _volumeStreams.value = buildVolumeStreams(audioManager)
    }

    fun toggleVolumePanel() {
        val nowVisible = !_volumePanelVisible.value
        if (nowVisible) {
            refreshVolumeStreams()
            _brightnessPanelVisible.value = false
            _musicWasVisibleBeforeSlider = _musicPanelVisible.value
            _musicPanelVisible.value = false
        } else if (_musicWasVisibleBeforeSlider) {
            _musicPanelVisible.value = true
            _musicWasVisibleBeforeSlider = false
        }
        _volumePanelVisible.value = nowVisible
    }

    fun dismissVolumePanel() {
        _volumePanelVisible.value = false
        if (_musicWasVisibleBeforeSlider) {
            _musicPanelVisible.value = true
            _musicWasVisibleBeforeSlider = false
        }
    }

    fun toggleBrightnessPanel() {
        val nowVisible = !_brightnessPanelVisible.value
        if (nowVisible) {
            _brightnessLevel.value = quickControls.getBrightness()
            _volumePanelVisible.value = false
            _musicWasVisibleBeforeSlider = _musicPanelVisible.value
            _musicPanelVisible.value = false
        } else if (_musicWasVisibleBeforeSlider) {
            _musicPanelVisible.value = true
            _musicWasVisibleBeforeSlider = false
        }
        _brightnessPanelVisible.value = nowVisible
    }

    fun dismissBrightnessPanel() {
        _brightnessPanelVisible.value = false
        if (_musicWasVisibleBeforeSlider) {
            _musicPanelVisible.value = true
            _musicWasVisibleBeforeSlider = false
        }
    }

    fun toggleMusicPanel() {
        val newValue = !_musicPanelVisible.value
        _musicPanelVisible.value = newValue
        viewModelScope.launch { prefsRepository.setMusicPanelOpen(newValue) }
    }

    fun dismissMusicPanel() {
        _musicPanelVisible.value = false
    }

    private var clipboardDismissedForExternalOpen = false

    fun toggleClipboardPanel() {
        _clipboardPanelVisible.value = !_clipboardPanelVisible.value
    }

    fun dismissClipboardPanel() {
        _clipboardPanelVisible.value = false
    }

    fun dismissClipboardPanelForExternalOpen() {
        clipboardDismissedForExternalOpen = true
        _clipboardPanelVisible.value = false
    }

    fun consumeClipboardDismissedForExternalOpen(): Boolean {
        val result = clipboardDismissedForExternalOpen
        clipboardDismissedForExternalOpen = false
        return result
    }

    fun playPause() { mediaRepository.playPause() }
    fun nextTrack() { mediaRepository.next() }
    fun prevTrack() { mediaRepository.prev() }

    fun setBrightnessLevel(value: Int) {
        try {
            quickControls.setBrightness(value)
            _brightnessLevel.value = value
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set brightness", e)
        }
    }

    fun setStreamVolume(streamType: Int, value: Int) {
        try {
            audioManager.setStreamVolume(streamType, value, 0)
            refreshVolumeStreams()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set stream volume", e)
        }
    }

    fun openSearch() {
        _isSearching.value = true
        _menuVisible.value = false
    }

    fun closeSearch() {
        _isSearching.value = false
        _searchQuery.value = ""
    }

    private val quickControlsChangeListener = object : QuickControlsChangeListener {
        override fun onQuickControlsChanged() { refreshQuickControls() }
    }

    init {
        viewModelScope.launch {
            _musicPanelVisible.value = prefsRepository.musicPanelOpen.first()
        }
        refreshQuickControls()
        quickControls.addChangeListener(quickControlsChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        quickControls.removeChangeListener(quickControlsChangeListener)
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
            canShowPowerMenu = quickControls.canShowPowerMenu(),
            canTakeScreenshot = quickControls.canTakeScreenshot(),
            canLockScreen = quickControls.canLockScreen(),
            caffeineMinutes = _quickControlsState.value.caffeineMinutes
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

    fun takeScreenshot() {
        quickControls.takeScreenshot()
    }

    fun lockScreen() {
        quickControls.lockScreen()
    }

    fun openDndSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun handleQuickControlAction(id: String) {
        when (id) {
            "volume" -> toggleVolumePanel()
            "torch" -> toggleTorch()
            "ringer" -> cycleRingerMode()
            "rotate" -> if (_quickControlsState.value.canWriteSettings) toggleAutoRotate() else {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            "brightness_slider" -> if (_quickControlsState.value.canWriteSettings) toggleBrightnessPanel() else {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            "dnd" -> if (_quickControlsState.value.dndPermissionGranted) toggleDnd() else openDndSettings()
            "qr" -> openQrScanner()
            "power" -> showPowerMenu()
            "screenshot" -> viewModelScope.launch { kotlinx.coroutines.delay(700); takeScreenshot() }
            "lockscreen" -> lockScreen()
            "caffeine" -> cycleCaffeineTimeout()
            "clipboard" -> toggleClipboardPanel()
        }
    }

    fun cycleCaffeineTimeout() {
        if (!_quickControlsState.value.canWriteSettings) return
        val current = _quickControlsState.value.caffeineMinutes
        when (current) {
            0 -> {
                originalScreenTimeoutMs = quickControls.getScreenTimeout()
                quickControls.setScreenTimeout(3 * 60 * 1000)
                _quickControlsState.value = _quickControlsState.value.copy(caffeineMinutes = 3)
            }
            3 -> {
                quickControls.setScreenTimeout(5 * 60 * 1000)
                _quickControlsState.value = _quickControlsState.value.copy(caffeineMinutes = 5)
            }
            5 -> {
                quickControls.setScreenTimeout(10 * 60 * 1000)
                _quickControlsState.value = _quickControlsState.value.copy(caffeineMinutes = 10)
            }
            else -> {
                if (originalScreenTimeoutMs > 0) quickControls.setScreenTimeout(originalScreenTimeoutMs)
                _quickControlsState.value = _quickControlsState.value.copy(caffeineMinutes = 0)
            }
        }
    }
}
