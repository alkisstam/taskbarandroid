package com.alkisstam.taskbar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alkisstam.taskbar.data.ClipItem
import com.alkisstam.taskbar.data.ClipboardRepository
import com.alkisstam.taskbar.data.NoteItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ClipboardViewModel @Inject constructor(
    private val clipboardRepository: ClipboardRepository
) : ViewModel() {

    val clips: StateFlow<List<ClipItem>> = clipboardRepository.clips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<ClipItem>> = clipboardRepository.clips
        .map { list -> list.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val noteItems: StateFlow<List<NoteItem>> = clipboardRepository.noteItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(item: ClipItem) {
        viewModelScope.launch { clipboardRepository.updateClip(item.copy(isFavorite = !item.isFavorite)) }
    }

    fun togglePin(item: ClipItem) {
        viewModelScope.launch { clipboardRepository.updateClip(item.copy(isPinned = !item.isPinned)) }
    }

    fun removeClip(id: String) {
        viewModelScope.launch { clipboardRepository.removeClip(id) }
    }

    fun addNote(content: String) {
        if (content.isBlank()) return
        val item = NoteItem(id = UUID.randomUUID().toString(), content = content.trim(), timestamp = System.currentTimeMillis())
        viewModelScope.launch { clipboardRepository.addNote(item) }
    }

    fun removeNote(id: String) {
        viewModelScope.launch { clipboardRepository.removeNote(id) }
    }

    fun toggleNotePin(item: NoteItem) {
        viewModelScope.launch { clipboardRepository.updateNote(item.copy(isPinned = !item.isPinned)) }
    }
}
