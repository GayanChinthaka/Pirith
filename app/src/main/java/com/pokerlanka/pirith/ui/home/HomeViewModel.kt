package com.pokerlanka.pirith.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.pirith.data.repository.PirithRepository
import com.pokerlanka.pirith.domain.model.Sutta
import com.pokerlanka.pirith.domain.model.SuttaCollection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class SearchFilter(
    val category: String,
    val query: String,
    val onlyFavorites: Boolean,
    val matchingVerseSuttaIds: List<Int>
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: PirithRepository
) : ViewModel() {

    private val standardCategories = listOf(
        "සියල්ල",
        "වන්දනා",
        "බෝධි පූජා",
        "මහා පිරිත්",
        "සෙත් පිරිත්",
        "විශේෂ සූත්‍ර"
    )

    private val _selectedCategory = MutableStateFlow("සියල්ල")
    private val _searchQuery = MutableStateFlow("")
    private val _onlyFavorites = MutableStateFlow(false)

    private val _allSuttas = repository.getAllSuttas()
    private val _customCollections = repository.getCustomCollections()

    private val _matchingVerseSuttaIds = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) flowOf(emptyList()) else repository.searchSuttaIdsByVerseContent(query.trim())
    }

    private val _filterFlow = combine(
        _selectedCategory,
        _searchQuery,
        _onlyFavorites,
        _matchingVerseSuttaIds
    ) { category, query, onlyFavorites, verseSuttaIds ->
        SearchFilter(category, query, onlyFavorites, verseSuttaIds)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _allSuttas,
        _customCollections,
        _filterFlow
    ) { suttas, customCollections, filter ->
        val trimmedQuery = filter.query.trim()
        val customCollection = customCollections.find { "coll_${it.id}" == filter.category || it.name == filter.category }

        val filtered = suttas.filter { sutta ->
            val matchesCategory = when {
                filter.category == "සියල්ල" -> true
                filter.category.startsWith("coll_") -> customCollection?.suttaIds?.contains(sutta.id) == true
                customCollections.any { it.name == filter.category } && !standardCategories.contains(filter.category) ->
                    customCollection?.suttaIds?.contains(sutta.id) == true
                else -> sutta.category == filter.category
            }
            val matchesFavorite = (!filter.onlyFavorites || sutta.isFavorite)
            val matchesSearch = if (trimmedQuery.isBlank()) {
                true
            } else {
                sutta.titleSinhala.contains(trimmedQuery, ignoreCase = true) ||
                sutta.titlePali.contains(trimmedQuery, ignoreCase = true) ||
                filter.matchingVerseSuttaIds.contains(sutta.id)
            }
            matchesCategory && matchesFavorite && matchesSearch
        }

        HomeUiState(
            suttas = filtered,
            categories = standardCategories,
            customCollections = customCollections,
            selectedCategory = filter.category,
            searchQuery = filter.query,
            onlyFavorites = filter.onlyFavorites,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onToggleFavoritesOnly() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun onToggleSuttaFavorite(sutta: Sutta) {
        viewModelScope.launch {
            repository.toggleFavorite(sutta.id, sutta.isFavorite)
        }
    }

    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val created = repository.createCustomCollection(name.trim())
            _selectedCategory.value = "coll_${created.id}"
        }
    }

    fun deleteCollection(collection: SuttaCollection) {
        viewModelScope.launch {
            repository.deleteCustomCollection(collection.id)
            if (_selectedCategory.value == "coll_${collection.id}" || _selectedCategory.value == collection.name) {
                _selectedCategory.value = "සියල්ල"
            }
        }
    }

    fun toggleSuttaInCollection(collectionId: String, suttaId: Int) {
        viewModelScope.launch {
            repository.toggleSuttaInCollection(collectionId, suttaId)
        }
    }
}
