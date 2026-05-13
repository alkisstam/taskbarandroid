package com.taskbar.app.service

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.taskbar.app.data.AppRepository
import com.taskbar.app.data.PreferencesRepository
import com.taskbar.app.data.QuickControlsRepository
import com.taskbar.app.viewmodel.AppMenuViewModel
import com.taskbar.app.viewmodel.TaskbarViewModel

class OverlayViewModelFactory(
    private val context: Context,
    private val appRepository: AppRepository,
    private val prefsRepository: PreferencesRepository,
    private val quickControlsRepository: QuickControlsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TaskbarViewModel::class.java) ->
                TaskbarViewModel(context.applicationContext, appRepository, prefsRepository) as T
            modelClass.isAssignableFrom(AppMenuViewModel::class.java) ->
                AppMenuViewModel(context.applicationContext, appRepository, prefsRepository, quickControlsRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
