package com.alkisstam.taskbar.viewmodel

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

    fun remove(id: String) {
        viewModelScope.launch { repository.remove(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
