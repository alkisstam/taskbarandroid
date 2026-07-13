package com.alkisstam.taskbar.viewmodel

import android.app.Notification
import android.app.PendingIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alkisstam.taskbar.data.NotificationEntry
import com.alkisstam.taskbar.data.NotificationHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationHistoryViewModel @Inject constructor(
    private val repository: NotificationHistoryRepository
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntry>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun actionsFor(id: String): List<Notification.Action> = repository.actionsFor(id)

    fun sendAction(action: Notification.Action) {
        try {
            action.actionIntent?.send()
        } catch (e: PendingIntent.CanceledException) {
            // App cancelled the intent since the notification was captured; nothing to do.
        }
    }

    fun remove(id: String) {
        viewModelScope.launch { repository.remove(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
