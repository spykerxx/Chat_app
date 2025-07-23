package com.example.telegram.domain

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.telegram.data.model.ThemePreferenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    val darkModeFlow = ThemePreferenceManager.getDarkMode(context)
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreferenceManager.setDarkMode(context, enabled)
        }
    }
}
