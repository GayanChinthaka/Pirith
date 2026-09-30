package com.pokerlanka.pirith.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.pirith.data.preferences.UserPreferencesRepository
import com.pokerlanka.pirith.data.repository.PirithRepository
import com.pokerlanka.pirith.domain.model.AppTheme
import com.pokerlanka.pirith.domain.model.ReaderMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val repository: PirithRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState

    init {
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    preferences = prefs,
                    autoScrollSpeed = prefs.autoScrollSpeed
                )
            }
        }
    }

    fun loadSutta(suttaId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val sutta = repository.getSuttaById(suttaId).firstOrNull()
            val verses = if (sutta != null) repository.getVersesForSutta(sutta.id).firstOrNull() ?: emptyList() else emptyList()
            _uiState.value = _uiState.value.copy(
                sutta = sutta,
                verses = verses,
                isLoading = false
            )
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

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.setAppTheme(theme)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setKeepScreenOn(enabled)
        }
    }

    fun toggleAutoScroll() {
        _uiState.value = _uiState.value.copy(
            isAutoScrolling = !_uiState.value.isAutoScrolling
        )
    }

    fun setAutoScrollSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(autoScrollSpeed = speed)
        viewModelScope.launch {
            preferencesRepository.setAutoScrollSpeed(speed)
        }
    }

    fun toggleFavorite() {
        val currentSutta = _uiState.value.sutta ?: return
        viewModelScope.launch {
            repository.toggleFavorite(currentSutta.id, currentSutta.isFavorite)
            _uiState.value = _uiState.value.copy(
                sutta = currentSutta.copy(isFavorite = !currentSutta.isFavorite)
            )
        }
    }
}
