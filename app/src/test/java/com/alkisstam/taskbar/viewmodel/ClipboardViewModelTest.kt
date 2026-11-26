package com.alkisstam.taskbar.viewmodel

import com.alkisstam.taskbar.data.ClipboardRepository
import com.alkisstam.taskbar.data.NoteItem
import com.alkisstam.taskbar.data.TodoItem
import com.alkisstam.taskbar.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClipboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = mockk<ClipboardRepository>(relaxed = true)

    @Test
    fun `blank edits are ignored and others are trimmed`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = ClipboardViewModel(repo)
        vm.updateNote(NoteItem("n", "   ", 1L))
        vm.updateTodo(TodoItem("t", "", 1L))
        vm.updateNote(NoteItem("n2", "  hi  ", 1L))
        advanceUntilIdle()
        coVerify(exactly = 0) { repo.updateNote(match { it.id == "n" }) }
        coVerify(exactly = 0) { repo.updateTodo(any()) }
        coVerify { repo.updateNote(NoteItem("n2", "hi", 1L)) }
    }
}
