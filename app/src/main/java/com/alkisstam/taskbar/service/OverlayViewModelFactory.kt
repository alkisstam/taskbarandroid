package com.alkisstam.taskbar.service

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alkisstam.taskbar.data.AppRepository
import com.alkisstam.taskbar.data.MediaRepository
import com.alkisstam.taskbar.data.PreferencesRepository
import com.alkisstam.taskbar.data.QuickControlsRepository
import com.alkisstam.taskbar.data.RecentAppsRepository
import com.alkisstam.taskbar.viewmodel.AppMenuViewModel
import com.alkisstam.taskbar.viewmodel.TaskbarViewModel

class OverlayViewModelFactory(
    private val context: Context,
    private val appRepository: AppRepository,
    private val prefsRepository: PreferencesRepository,
    private val quickControlsRepository: QuickControlsRepository,
    private val mediaRepository: MediaRepository,
    private val recentAppsRepository: RecentAppsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TaskbarViewModel::class.java) ->
                TaskbarViewModel(context.applicationContext, appRepository, prefsRepository, recentAppsRepository) as T
            modelClass.isAssignableFrom(AppMenuViewModel::class.java) ->
                AppMenuViewModel(context.applicationContext, appRepository, prefsRepository, quickControlsRepository, mediaRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
