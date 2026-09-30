package com.pokerlanka.pirith.ui.reader

import com.pokerlanka.pirith.data.preferences.UserPreferences
import com.pokerlanka.pirith.domain.model.Sutta
import com.pokerlanka.pirith.domain.model.Verse

data class ReaderUiState(
    val sutta: Sutta? = null,
    val verses: List<Verse> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isAutoScrolling: Boolean = false,
    val autoScrollSpeed: Float = 0.5f,
    val isLoading: Boolean = true
)
