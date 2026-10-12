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
        "පට්ඨාන",
        "විශේෂ සූත්‍ර"
    )

    private val _selectedCategory = MutableStateFlow("සියල්ල")
    private val _searchQuery = MutableStateFlow("")
    private val _onlyFavorites = MutableStateFlow(false)

    private val _allSuttas = repository.getAllSuttas()
    private val _customCollections = repository.getCustomCollections()
    private val _tabOrder = repository.getTabOrder()

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
        _tabOrder,
        _filterFlow
    ) { suttas, customCollections, tabOrder, filter ->
        val trimmedQuery = filter.query.trim()
        val customCollection = customCollections.find { "coll_${it.id}" == filter.category || it.name == filter.category }

        // Build default collection items
        val defaultItems = standardCategories.map { catName ->
            val count = if (catName == "සියල්ල") suttas.size else suttas.count { it.category == catName }
            com.pokerlanka.pirith.domain.model.CollectionItem(
                key = catName,
                name = catName,
                isDefault = true,
                suttaCount = count
            )
        }

        // Build custom collection items
        val customItems = customCollections.map { coll ->
            com.pokerlanka.pirith.domain.model.CollectionItem(
                key = "coll_${coll.id}",
                name = coll.name,
                isDefault = false,
                suttaCount = coll.suttaIds.size,
                customCollection = coll
            )
        }

        // Combine and order according to tabOrder preferences
        val allItemsMap = (defaultItems + customItems).associateBy { it.key }
        val orderedKeys = if (tabOrder.isNotEmpty()) {
            val validSavedKeys = tabOrder.filter { allItemsMap.containsKey(it) }
            val missingKeys = allItemsMap.keys.filter { !validSavedKeys.contains(it) }
            validSavedKeys + missingKeys
        } else {
            allItemsMap.keys.toList()
        }
        val orderedCollectionItems = orderedKeys.mapNotNull { allItemsMap[it] }

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

        // When viewing a custom collection without search, preserve the exact custom order of suttaIds
        val sortedList = if (customCollection != null && trimmedQuery.isBlank()) {
            val orderMap = customCollection.suttaIds.withIndex().associate { it.value to it.index }
            filtered.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
        } else {
            filtered
        }

        HomeUiState(
            suttas = sortedList,
            categories = standardCategories,
            customCollections = customCollections,
            orderedCollections = orderedCollectionItems,
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

    fun createCollection(name: String, onResult: ((Boolean) -> Unit)? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            onResult?.invoke(false)
            return
        }
        viewModelScope.launch {
            val created = repository.createCustomCollection(trimmed)
            if (created != null) {
                _selectedCategory.value = "coll_${created.id}"
                onResult?.invoke(true)
            } else {
                onResult?.invoke(false)
            }
        }
    }

    fun renameCollection(collectionId: String, newName: String, onResult: ((Boolean) -> Unit)? = null) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            onResult?.invoke(false)
            return
        }
        viewModelScope.launch {
            val success = repository.renameCustomCollection(collectionId, trimmed)
            onResult?.invoke(success)
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
