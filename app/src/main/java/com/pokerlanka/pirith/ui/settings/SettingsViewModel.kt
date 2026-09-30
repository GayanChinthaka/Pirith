package com.pokerlanka.pirith.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.pirith.data.preferences.UserPreferences
import com.pokerlanka.pirith.data.preferences.UserPreferencesRepository
import com.pokerlanka.pirith.domain.model.AppTheme
import com.pokerlanka.pirith.domain.model.ReaderMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.setAppTheme(theme)
        }
    }

    fun setReaderMode(mode: ReaderMode) {
        viewModelScope.launch {
            preferencesRepository.setReaderMode(mode)
        }
    }

    fun setFontSizeScale(scale: Float) {
        viewModelScope.launch {
            preferencesRepository.setFontSizeScale(scale)
        }
    }

    fun setLineHeightMultiplier(multiplier: Float) {
        viewModelScope.launch {
            preferencesRepository.setLineHeightMultiplier(multiplier)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setKeepScreenOn(enabled)
        }
    }
}
