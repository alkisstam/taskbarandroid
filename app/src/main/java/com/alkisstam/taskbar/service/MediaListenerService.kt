package com.alkisstam.taskbar.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.alkisstam.taskbar.data.MediaRepository
import com.alkisstam.taskbar.data.NotificationHistoryRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "MediaListenerService"

@AndroidEntryPoint
class MediaListenerService : NotificationListenerService() {

    @Inject lateinit var mediaRepository: MediaRepository
    @Inject lateinit var notificationHistoryRepository: NotificationHistoryRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected")
        mediaRepository.onListenerConnected(
            ComponentName(this, MediaListenerService::class.java)
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        mediaRepository.onNotificationPosted(
            ComponentName(this, MediaListenerService::class.java)
        )
        sbn?.let { notificationHistoryRepository.record(it) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Listener disconnected")
        mediaRepository.onListenerDisconnected()
    }
}
