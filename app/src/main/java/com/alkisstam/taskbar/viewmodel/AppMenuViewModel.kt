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
import com.alkisstam.taskbar.data.AppSortOrder
import com.alkisstam.taskbar.data.MediaRepository
import com.alkisstam.taskbar.data.MediaState
import com.alkisstam.taskbar.data.PreferencesRepository
import com.alkisstam.taskbar.data.QuickControlsChangeListener
import com.alkisstam.taskbar.data.QuickControlsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class QuickControlItemData(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val active: Boolean,
    val badge: String? = null
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
    val caffeineMinutes: Int = 0,
    val wifiEnabled: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val mobileDataEnabled: Boolean = false
)

private const val TAG = "AppMenuViewModel"

private fun sortApps(apps: List<AppInfo>, order: AppSortOrder, counts: Map<String, Int>): List<AppInfo> =
    when (order) {
        AppSortOrder.NAME -> apps.sortedBy { it.label.lowercase() }
        AppSortOrder.INSTALL_TIME -> apps.sortedWith(
            compareByDescending<AppInfo> { it.firstInstallTime }.thenBy { it.label.lowercase() }
        )
        AppSortOrder.USAGE -> apps.sortedWith(
            compareByDescending<AppInfo> { counts[it.packageName] ?: 0 }.thenBy { it.label.lowercase() }
        )
    }

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

    val appSortOrder: StateFlow<AppSortOrder> = prefsRepository.appSortOrder
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSortOrder.NAME)

    val appLaunchCounts: StateFlow<Map<String, Int>> = prefsRepository.appLaunchCounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setAppSortOrder(order: AppSortOrder) {
        viewModelScope.launch { prefsRepository.setAppSortOrder(order) }
    }

    val allApps: StateFlow<List<AppInfo>> = combine(
        appRepository.apps, appSortOrder, appLaunchCounts
    ) { apps, order, counts ->
        sortApps(apps, order, counts)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val fuzzySearchEnabled: StateFlow<Boolean> = prefsRepository.fuzzySearchEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val alwaysShowMusicPanel: StateFlow<Boolean> = prefsRepository.alwaysShowMusicPanel
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val showRecentApps: StateFlow<Boolean> = prefsRepository.showRecentApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val showRecentAppsRow: StateFlow<Boolean> = prefsRepository.showRecentAppsRow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val recentApps: StateFlow<List<AppInfo>> = combine(prefsRepository.recentOpenedApps, appRepository.apps) { recentPackages, apps ->
        val byPackage = apps.associateBy { it.packageName }
        recentPackages.mapNotNull { byPackage[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hiddenApps: StateFlow<Set<String>> = prefsRepository.hiddenApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredApps: StateFlow<List<AppInfo>> = combine(allApps, _searchQuery, fuzzySearchEnabled, prefsRepository.hiddenApps) { sorted, query, fuzzyEnabled, hidden ->
        val apps = sorted.filter { it.packageName !in hidden }
        val q = query.trim()
        when {
            q.isEmpty() -> apps
            fuzzyEnabled -> apps.mapNotNull { app -> fuzzyScore(app.label, q)?.let { app to it } }
                .sortedBy { it.second }
                .map { it.first }
            else -> apps.filter { it.label.contains(q, ignoreCase = true) }
                .sortedBy { it.label.lowercase() }
        }
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
    private var _musicWasVisibleBeforeOverlay = false

    private val _noMediaMessage = MutableStateFlow<String?>(null)
    val noMediaMessage: StateFlow<String?> = _noMediaMessage.asStateFlow()

    private val _clipboardPanelVisible = MutableStateFlow(false)
    val clipboardPanelVisible: StateFlow<Boolean> = _clipboardPanelVisible.asStateFlow()

    private val _calculatorPanelVisible = MutableStateFlow(false)
    val calculatorPanelVisible: StateFlow<Boolean> = _calculatorPanelVisible.asStateFlow()

    private val _notificationPanelVisible = MutableStateFlow(false)
    val notificationPanelVisible: StateFlow<Boolean> = _notificationPanelVisible.asStateFlow()

    private val _notesPanelVisible = MutableStateFlow(false)
    val notesPanelVisible: StateFlow<Boolean> = _notesPanelVisible.asStateFlow()

    val mediaState: StateFlow<MediaState> = mediaRepository.mediaState

    private val _brightnessLevel = MutableStateFlow(128)
    val brightnessLevel: StateFlow<Int> = _brightnessLevel.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val volumeMutex = Mutex()
    private var originalScreenTimeoutMs: Int = -1

    private val _volumeStreams = MutableStateFlow<List<VolumeStreamInfo>>(emptyList())
    val volumeStreams: StateFlow<List<VolumeStreamInfo>> = _volumeStreams.asStateFlow()

    private fun refreshVolumeStreams() {
        _volumeStreams.value = buildVolumeStreams(context, audioManager)
    }

    private fun anyOverlayPanelVisible(): Boolean =
        _volumePanelVisible.value || _brightnessPanelVisible.value || _calculatorPanelVisible.value

    private fun hideMusicForOverlay() {
        if (!anyOverlayPanelVisible()) _musicWasVisibleBeforeOverlay = _musicPanelVisible.value
        _musicPanelVisible.value = false
    }

    private fun restoreMusicAfterOverlay() {
        if (anyOverlayPanelVisible()) return
        if (_musicWasVisibleBeforeOverlay) _musicPanelVisible.value = true
        _musicWasVisibleBeforeOverlay = false
    }

    fun toggleVolumePanel() {
        val nowVisible = !_volumePanelVisible.value
        if (nowVisible) {
            refreshVolumeStreams()
            hideMusicForOverlay()
            _brightnessPanelVisible.value = false
            _calculatorPanelVisible.value = false
        }
        _volumePanelVisible.value = nowVisible
        if (!nowVisible) restoreMusicAfterOverlay()
    }

    fun dismissVolumePanel() {
        _volumePanelVisible.value = false
        restoreMusicAfterOverlay()
    }

    fun toggleBrightnessPanel() {
        val nowVisible = !_brightnessPanelVisible.value
        if (nowVisible) {
            _brightnessLevel.value = quickControls.getBrightness()
            hideMusicForOverlay()
            _volumePanelVisible.value = false
            _calculatorPanelVisible.value = false
        }
        _brightnessPanelVisible.value = nowVisible
        if (!nowVisible) restoreMusicAfterOverlay()
    }

    fun dismissBrightnessPanel() {
        _brightnessPanelVisible.value = false
        restoreMusicAfterOverlay()
    }

    fun toggleMusicPanel() {
        if (!_musicPanelVisible.value && !mediaState.value.hasSession && !alwaysShowMusicPanel.value) {
            _noMediaMessage.value = "No Media Playing"
            return
        }
        val newValue = !_musicPanelVisible.value
        if (newValue) _calculatorPanelVisible.value = false
        _musicPanelVisible.value = newValue
        _musicWasVisibleBeforeOverlay = false
        viewModelScope.launch { prefsRepository.setMusicPanelOpen(newValue) }
    }

    fun dismissMusicPanel() {
        _musicPanelVisible.value = false
        _musicWasVisibleBeforeOverlay = false
    }

    fun clearNoMediaMessage() {
        _noMediaMessage.value = null
    }

    private var clipboardDismissedForExternalOpen = false

    fun toggleClipboardPanel() {
        val newValue = !_clipboardPanelVisible.value
        if (newValue) {
            _calculatorPanelVisible.value = false
            _notificationPanelVisible.value = false
            _notesPanelVisible.value = false
        }
        _clipboardPanelVisible.value = newValue
    }

    fun dismissClipboardPanel() {
        _clipboardPanelVisible.value = false
    }

    fun toggleNotesPanel() {
        val newValue = !_notesPanelVisible.value
        if (newValue) {
            _calculatorPanelVisible.value = false
            _notificationPanelVisible.value = false
            _clipboardPanelVisible.value = false
        }
        _notesPanelVisible.value = newValue
    }

    fun dismissNotesPanel() {
        _notesPanelVisible.value = false
    }

    fun dismissNotesPanelForExternalOpen() {
        _notesPanelVisible.value = false
    }

    fun toggleCalculatorPanel() {
        val newValue = !_calculatorPanelVisible.value
        if (newValue) {
            hideMusicForOverlay()
            _volumePanelVisible.value = false
            _brightnessPanelVisible.value = false
            _clipboardPanelVisible.value = false
            _notificationPanelVisible.value = false
            _notesPanelVisible.value = false
            _menuVisible.value = false
            _isSearching.value = false
        }
        _calculatorPanelVisible.value = newValue
        if (!newValue) restoreMusicAfterOverlay()
    }

    fun dismissCalculatorPanel() {
        _calculatorPanelVisible.value = false
        restoreMusicAfterOverlay()
    }

    fun toggleNotificationPanel() {
        val newValue = !_notificationPanelVisible.value
        if (newValue) {
            _calculatorPanelVisible.value = false
            _clipboardPanelVisible.value = false
            _notesPanelVisible.value = false
        }
        _notificationPanelVisible.value = newValue
    }

    fun dismissNotificationPanel() {
        _notificationPanelVisible.value = false
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
        // Off the main thread: a large jump means up to hundreds of synchronous binder
        // calls. Mutex keeps rapid slider events from interleaving their adjust loops.
        viewModelScope.launch(Dispatchers.Default) {
            volumeMutex.withLock {
                try {
                    // Absolute setStreamVolume is blocked for the media stream on some OEM
                    // ROMs (e.g. ColorOS rejects it as "no setStreamVolume permission").
                    // Step toward the target with adjustStreamVolume — the same path the
                    // hardware volume keys use — which those ROMs do allow.
                    var guard = 0
                    while (guard < 400) {
                        val current = audioManager.getStreamVolume(streamType)
                        if (current == value) break
                        val dir = if (value > current) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                        audioManager.adjustStreamVolume(streamType, dir, 0)
                        val next = audioManager.getStreamVolume(streamType)
                        if (next == current) break // no movement: blocked or at min/max
                        // stop once we reach or pass the target (OEM step size may exceed 1)
                        if ((dir == AudioManager.ADJUST_RAISE && next >= value) ||
                            (dir == AudioManager.ADJUST_LOWER && next <= value)) break
                        guard++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set stream volume", e)
                }
                // Reflect the real resulting volume (may differ from target if the OEM
                // steps coarsely) so the slider settles on the actual position.
                val actual = audioManager.getStreamVolume(streamType)
                _volumeStreams.value = _volumeStreams.value.map {
                    if (it.streamType == streamType) it.copy(current = actual) else it
                }
            }
        }
    }

    fun openSearch() {
        _isSearching.value = true
        _menuVisible.value = false
        _calculatorPanelVisible.value = false
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
        viewModelScope.launch {
            // Leftover value means the process died while caffeine was active; put the
            // user's real screen timeout back.
            val storedTimeout = prefsRepository.caffeineOriginalTimeout.first()
            if (storedTimeout != null) {
                if (quickControls.canWriteSettings()) quickControls.setScreenTimeout(storedTimeout)
                prefsRepository.setCaffeineOriginalTimeout(null)
            }
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
        if (_menuVisible.value) {
            refreshQuickControls()
            _calculatorPanelVisible.value = false
        }
    }

    fun dismissMenu() {
        _menuVisible.value = false
        closeSearch()
    }

    fun launchApp(packageName: String) {
        appRepository.launchApp(packageName)
        viewModelScope.launch {
            prefsRepository.addRecentOpenedApp(packageName)
            prefsRepository.incrementLaunchCount(packageName)
        }
        _menuVisible.value = false
        closeSearch()
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch { prefsRepository.pinApp(packageName) }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch { prefsRepository.unpinApp(packageName) }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch { prefsRepository.hideApp(packageName) }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch { prefsRepository.unhideApp(packageName) }
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
            caffeineMinutes = _quickControlsState.value.caffeineMinutes,
            wifiEnabled = quickControls.isWifiEnabled(),
            bluetoothEnabled = quickControls.isBluetoothEnabled(),
            mobileDataEnabled = quickControls.isMobileDataEnabled()
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

    fun openWifiPanel() {
        quickControls.openWifiPanel()
    }

    fun openBluetoothPanel() {
        quickControls.openBluetoothPanel()
    }

    fun openMobileDataPanel() {
        quickControls.openMobileDataPanel()
    }

    fun openQuickShare() {
        quickControls.openQuickShare()
    }

    fun openDndSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open DND settings", e)
        }
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
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open write-settings screen", e)
                }
            }
            "brightness_slider" -> if (_quickControlsState.value.canWriteSettings) toggleBrightnessPanel() else {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open write-settings screen", e)
                }
            }
            "dnd" -> if (_quickControlsState.value.dndPermissionGranted) toggleDnd() else openDndSettings()
            "qr" -> openQrScanner()
            "power" -> showPowerMenu()
            "screenshot" -> viewModelScope.launch { kotlinx.coroutines.delay(700); takeScreenshot() }
            "lockscreen" -> lockScreen()
            "caffeine" -> cycleCaffeineTimeout()
            "clipboard" -> toggleClipboardPanel()
            "notes" -> toggleNotesPanel()
            "calculator" -> toggleCalculatorPanel()
            "wifi" -> openWifiPanel()
            "bluetooth" -> openBluetoothPanel()
            "mobile_data" -> openMobileDataPanel()
            "share" -> openQuickShare()
            "notif_history" -> toggleNotificationPanel()
        }
    }

    fun cycleCaffeineTimeout() {
        if (!_quickControlsState.value.canWriteSettings) return
        val current = _quickControlsState.value.caffeineMinutes
        when (current) {
            0 -> {
                originalScreenTimeoutMs = quickControls.getScreenTimeout()
                viewModelScope.launch { prefsRepository.setCaffeineOriginalTimeout(originalScreenTimeoutMs) }
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
                viewModelScope.launch { prefsRepository.setCaffeineOriginalTimeout(null) }
                _quickControlsState.value = _quickControlsState.value.copy(caffeineMinutes = 0)
            }
        }
    }

    fun deactivateCaffeine() {
        if (_quickControlsState.value.caffeineMinutes == 0) return
        if (originalScreenTimeoutMs > 0) quickControls.setScreenTimeout(originalScreenTimeoutMs)
        viewModelScope.launch { prefsRepository.setCaffeineOriginalTimeout(null) }
        _quickControlsState.value = _quickControlsState.value.copy(caffeineMinutes = 0)
    }
}
