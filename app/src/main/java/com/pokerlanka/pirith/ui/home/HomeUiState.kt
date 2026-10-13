package com.pokerlanka.pirith.ui.home

import com.pokerlanka.pirith.domain.model.CollectionItem
import com.pokerlanka.pirith.domain.model.Sutta
import com.pokerlanka.pirith.domain.model.SuttaCollection

data class HomeUiState(
    val suttas: List<Sutta> = emptyList(),
    val categories: List<String> = emptyList(),
    val customCollections: List<SuttaCollection> = emptyList(),
    val orderedCollections: List<CollectionItem> = emptyList(),
    val selectedCategory: String = "සියල්ල",
    val searchQuery: String = "",
    val onlyFavorites: Boolean = false,
    val isLoading: Boolean = true,
    val showEnglishNames: Boolean = false
)
