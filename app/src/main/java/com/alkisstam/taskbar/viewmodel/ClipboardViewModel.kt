package com.alkisstam.taskbar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alkisstam.taskbar.data.ClipItem
import com.alkisstam.taskbar.data.ClipboardRepository
import com.alkisstam.taskbar.data.NoteItem
import com.alkisstam.taskbar.data.TodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class FavoriteEntry {
    data class Clip(val item: ClipItem) : FavoriteEntry()
    data class Note(val item: NoteItem) : FavoriteEntry()

    val timestamp: Long get() = when (this) {
        is Clip -> item.timestamp
        is Note -> item.timestamp
    }
}

@HiltViewModel
class ClipboardViewModel @Inject constructor(
    private val clipboardRepository: ClipboardRepository
) : ViewModel() {

    val clips: StateFlow<List<ClipItem>> = clipboardRepository.clips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntry>> = combine(
        clipboardRepository.clips,
        clipboardRepository.noteItems
    ) { clips, notes ->
        (clips.filter { it.isFavorite }.map { FavoriteEntry.Clip(it) } +
            notes.filter { it.isFavorite }.map { FavoriteEntry.Note(it) })
            .sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val noteItems: StateFlow<List<NoteItem>> = clipboardRepository.noteItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todoItems: StateFlow<List<TodoItem>> = clipboardRepository.todoItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shareHintDismissed: StateFlow<Boolean> = clipboardRepository.shareHintDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun dismissShareHint() {
        viewModelScope.launch { clipboardRepository.dismissShareHint() }
    }

    fun toggleFavorite(item: ClipItem) {
        viewModelScope.launch { clipboardRepository.updateClip(item.copy(isFavorite = !item.isFavorite)) }
    }

    fun toggleNoteFavorite(item: NoteItem) {
        viewModelScope.launch { clipboardRepository.updateNote(item.copy(isFavorite = !item.isFavorite)) }
    }

    fun togglePin(item: ClipItem) {
        viewModelScope.launch { clipboardRepository.updateClip(item.copy(isPinned = !item.isPinned)) }
    }

    fun removeClip(id: String) {
        viewModelScope.launch { clipboardRepository.removeClip(id) }
    }

    fun updateClip(item: ClipItem) {
        viewModelScope.launch { clipboardRepository.updateClip(item) }
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

    fun updateNote(item: NoteItem) {
        viewModelScope.launch { clipboardRepository.updateNote(item) }
    }

    fun addTodo(content: String) {
        if (content.isBlank()) return
        val item = TodoItem(id = UUID.randomUUID().toString(), content = content.trim(), timestamp = System.currentTimeMillis())
        viewModelScope.launch { clipboardRepository.addTodo(item) }
    }

    fun removeTodo(id: String) {
        viewModelScope.launch { clipboardRepository.removeTodo(id) }
    }

    fun toggleTodoDone(item: TodoItem) {
        viewModelScope.launch { clipboardRepository.updateTodo(item.copy(isDone = !item.isDone)) }
    }

    fun toggleTodoPin(item: TodoItem) {
        viewModelScope.launch { clipboardRepository.updateTodo(item.copy(isPinned = !item.isPinned)) }
    }

    fun updateTodo(item: TodoItem) {
        viewModelScope.launch { clipboardRepository.updateTodo(item) }
    }
}
