package com.ytsave.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytsave.app.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val defaultFormat: StateFlow<String> = preferencesManager.defaultFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "video+audio")


    val themeMode: StateFlow<String> = preferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun updateDefaultFormat(format: String) {
        viewModelScope.launch {
            preferencesManager.setDefaultFormat(format)
        }
    }


    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }
}
