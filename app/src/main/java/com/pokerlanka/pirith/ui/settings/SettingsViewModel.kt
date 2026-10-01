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

    val customCollections: StateFlow<List<com.pokerlanka.pirith.domain.model.SuttaCollection>> = preferencesRepository.customCollectionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createCollection(name: String, onResult: ((Boolean) -> Unit)? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            onResult?.invoke(false)
            return
        }
        viewModelScope.launch {
            val created = preferencesRepository.createCollection(trimmed)
            onResult?.invoke(created != null)
        }
    }

    fun renameCollection(collectionId: String, newName: String, onResult: ((Boolean) -> Unit)? = null) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            onResult?.invoke(false)
            return
        }
        viewModelScope.launch {
            val success = preferencesRepository.renameCollection(collectionId, trimmed)
            onResult?.invoke(success)
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            preferencesRepository.deleteCollection(collectionId)
        }
    }

    fun moveCollection(fromIndex: Int, toIndex: Int) {
        val current = customCollections.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            viewModelScope.launch {
                preferencesRepository.updateCollectionsOrder(current)
            }
        }
    }

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
