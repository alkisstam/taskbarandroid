package com.alkisstam.taskbar.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MediaRepository"
private const val MAX_REFRESH_RETRIES = 3

data class MediaState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val albumArt: Bitmap? = null,
    val hasSession: Boolean = false,
    val packageName: String = ""
)

@Singleton
class MediaRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private var listenerComponentName: ComponentName? = null
    private var activeController: MediaController? = null
    private var refreshRetryCount = 0
    // Main dispatcher: activeController/refreshRetryCount are also mutated by the
    // session-manager and controller callbacks, which run on the main looper. Keeping
    // the retry path on the same thread avoids racing those mutations.
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindController(controllers)
        }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateState(activeController)
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateState(activeController)
        }
        override fun onSessionDestroyed() {
            activeController?.unregisterCallback(this)
            activeController = null
            _mediaState.value = MediaState()
            listenerComponentName?.let { refreshSessions(it) }
        }
    }

    fun onListenerConnected(componentName: ComponentName) {
        listenerComponentName = componentName
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener, componentName
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register sessions listener", e)
        }
        refreshSessions(componentName)
    }

    fun onListenerDisconnected() {
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister sessions listener", e)
        }
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        listenerComponentName = null
        _mediaState.value = MediaState()
    }

    private fun refreshSessions(componentName: ComponentName) {
        val controllers = try {
            val result = mediaSessionManager.getActiveSessions(componentName)
            refreshRetryCount = 0
            result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get active sessions (attempt ${refreshRetryCount + 1})", e)
            scheduleRefreshRetry(componentName)
            return
        }
        bindController(controllers)
    }

    private fun scheduleRefreshRetry(componentName: ComponentName) {
        if (refreshRetryCount >= MAX_REFRESH_RETRIES) {
            Log.w(TAG, "Max session-fetch retries reached, forcing listener rebind")
            refreshRetryCount = 0
            forceListenerRebind(componentName)
            return
        }
        refreshRetryCount++
        val delayMs = 1000L shl (refreshRetryCount - 1)
        repoScope.launch {
            delay(delayMs)
            refreshSessions(componentName)
        }
    }

    /** requestRebind alone is a silent no-op on ColorOS/Realme UI when the binding has
     * gone stale; toggling the component's enabled state forces the system to re-evaluate
     * enabled listeners and rebind. */
    private fun forceListenerRebind(componentName: ComponentName) {
        try {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle listener component", e)
        }
        try {
            NotificationListenerService.requestRebind(componentName)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request listener rebind", e)
        }
    }

    /** Fallback trigger for OEMs (e.g. Samsung One UI) where the sessions-changed
     * listener can miss the initial media session; any posted notification re-checks.
     * ColorOS/Realme UI can also rebind the listener without redelivering
     * onListenerConnected — a posted notification proves we are connected, so a null
     * componentName here means that callback was skipped and we recover it. */
    fun onNotificationPosted(componentName: ComponentName) {
        if (listenerComponentName == null) {
            Log.w(TAG, "Notification posted before onListenerConnected; recovering connection")
            onListenerConnected(componentName)
            return
        }
        if (activeController == null) {
            refreshSessions(componentName)
        }
    }

    private fun bindController(controllers: List<MediaController>?) {
        try {
            val preferred = controllers?.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: controllers?.firstOrNull()

            if (preferred?.sessionToken != activeController?.sessionToken) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = preferred
                preferred?.registerCallback(controllerCallback)
            }
            updateState(preferred)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to bind media controller", e)
        }
    }

    private fun updateState(controller: MediaController?) {
        if (controller == null) {
            _mediaState.value = MediaState()
            return
        }
        try {
            val pbState = controller.playbackState
            val metadata = controller.metadata
            _mediaState.value = MediaState(
                isPlaying = pbState?.state == PlaybackState.STATE_PLAYING,
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
                albumArt = safeCopy(
                    metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ),
                hasSession = true,
                packageName = controller.packageName
            )
        } catch (e: Exception) {
            // playbackState/metadata are binder calls into the media app; they throw if
            // its session died between the callback firing and us reading it.
            Log.w(TAG, "Failed to read media session state", e)
            _mediaState.value = MediaState()
        }
    }

    private fun safeCopy(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        return try {
            if (bitmap.isRecycled) null else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy album art bitmap from external session", e)
            null
        }
    }

    fun playPause() {
        val ctrl = activeController ?: return
        try {
            if (ctrl.playbackState?.state == PlaybackState.STATE_PLAYING) {
                ctrl.transportControls.pause()
            } else {
                ctrl.transportControls.play()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play/pause", e)
        }
    }

    fun next() {
        try {
            activeController?.transportControls?.skipToNext()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to skip to next", e)
        }
    }

    fun prev() {
        try {
            activeController?.transportControls?.skipToPrevious()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to skip to previous", e)
        }
    }

    fun isNotificationAccessGranted(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        // Settings may store either the full or the short flattened form ("pkg/.service.X")
        // depending on which UI wrote it; unflatten each entry so both forms match.
        val component = ComponentName(context, "com.alkisstam.taskbar.service.MediaListenerService")
        return enabled.split(":").any { ComponentName.unflattenFromString(it.trim()) == component }
    }
}
