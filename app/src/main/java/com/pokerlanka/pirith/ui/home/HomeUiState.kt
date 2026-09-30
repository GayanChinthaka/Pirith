package com.pokerlanka.pirith.ui.home

import com.pokerlanka.pirith.domain.model.Sutta

data class HomeUiState(
    val suttas: List<Sutta> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "සියල්ල",
    val searchQuery: String = "",
    val onlyFavorites: Boolean = false,
    val isLoading: Boolean = true
)
