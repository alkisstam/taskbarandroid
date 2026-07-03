package com.alkisstam.taskbar.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MediaRepository"

data class MediaState(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val albumArt: Bitmap? = null,
    val hasSession: Boolean = false
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
            mediaSessionManager.getActiveSessions(componentName)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get active sessions", e)
            return
        }
        bindController(controllers)
    }

    private fun bindController(controllers: List<MediaController>?) {
        val preferred = controllers?.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers?.firstOrNull()

        if (preferred?.sessionToken != activeController?.sessionToken) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = preferred
            preferred?.registerCallback(controllerCallback)
        }
        updateState(preferred)
    }

    private fun updateState(controller: MediaController?) {
        if (controller == null) {
            _mediaState.value = MediaState()
            return
        }
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
            hasSession = true
        )
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
        if (ctrl.playbackState?.state == PlaybackState.STATE_PLAYING) {
            ctrl.transportControls.pause()
        } else {
            ctrl.transportControls.play()
        }
    }

    fun next() {
        activeController?.transportControls?.skipToNext()
    }

    fun prev() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun isNotificationAccessGranted(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val component = ComponentName(context, "com.alkisstam.taskbar.service.MediaListenerService")
            .flattenToString()
        return enabled.split(":").any { it.trim() == component }
    }
}
