package com.pokerlanka.pirith.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.pirith.data.preferences.UserPreferences
import com.pokerlanka.pirith.data.preferences.UserPreferencesRepository
import com.pokerlanka.pirith.data.repository.PirithRepository
import com.pokerlanka.pirith.domain.model.AppTheme
import com.pokerlanka.pirith.domain.model.CollectionItem
import com.pokerlanka.pirith.domain.model.ReaderMode
import com.pokerlanka.pirith.domain.model.SuttaCollection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val pirithRepository: PirithRepository
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

    val preferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val customCollections: StateFlow<List<SuttaCollection>> = preferencesRepository.customCollectionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSuttas: StateFlow<List<com.pokerlanka.pirith.domain.model.Sutta>> = pirithRepository.getAllSuttas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCollections: StateFlow<List<CollectionItem>> = combine(
        pirithRepository.getAllSuttas(),
        preferencesRepository.customCollectionsFlow,
        preferencesRepository.tabOrderFlow
    ) { suttas, customList, tabOrder ->
        val defaultItems = standardCategories.map { catName ->
            val count = if (catName == "සියල්ල") suttas.size else suttas.count { it.category == catName }
            CollectionItem(
                key = catName,
                name = catName,
                isDefault = true,
                suttaCount = count
            )
        }

        val customItems = customList.map { coll ->
            CollectionItem(
                key = "coll_${coll.id}",
                name = coll.name,
                isDefault = false,
                suttaCount = coll.suttaIds.size,
                customCollection = coll
            )
        }

        val allItemsMap = (defaultItems + customItems).associateBy { it.key }
        val orderedKeys = if (tabOrder.isNotEmpty()) {
            val validSavedKeys = tabOrder.filter { allItemsMap.containsKey(it) }
            val missingKeys = allItemsMap.keys.filter { !validSavedKeys.contains(it) }
            validSavedKeys + missingKeys
        } else {
            allItemsMap.keys.toList()
        }

        orderedKeys.mapNotNull { allItemsMap[it] }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createCollection(name: String, onResult: ((Boolean) -> Unit)? = null) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || standardCategories.any { it.equals(trimmed, ignoreCase = true) }) {
            onResult?.invoke(false)
            return
        }
        viewModelScope.launch {
            val created = preferencesRepository.createCollection(trimmed)
            if (created != null) {
                // Append new collection key to tabOrder
                val currentKeys = allCollections.value.map { it.key }.toMutableList()
                currentKeys.add("coll_${created.id}")
                preferencesRepository.updateTabOrder(currentKeys)
                onResult?.invoke(true)
            } else {
                onResult?.invoke(false)
            }
        }
    }

    fun renameCollection(collectionId: String, newName: String, onResult: ((Boolean) -> Unit)? = null) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || standardCategories.any { it.equals(trimmed, ignoreCase = true) }) {
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
            val updatedKeys = allCollections.value.filter { it.key != "coll_$collectionId" }.map { it.key }
            preferencesRepository.updateTabOrder(updatedKeys)
        }
    }

    fun moveCollection(fromIndex: Int, toIndex: Int) {
        val current = allCollections.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            val newKeys = current.map { it.key }
            viewModelScope.launch {
                preferencesRepository.updateTabOrder(newKeys)
            }
        }
    }

    fun moveSuttaInCollection(collectionId: String, fromIndex: Int, toIndex: Int) {
        val coll = customCollections.value.find { it.id == collectionId } ?: return
        val currentSuttaIds = coll.suttaIds.toMutableList()
        if (fromIndex in currentSuttaIds.indices && toIndex in currentSuttaIds.indices && fromIndex != toIndex) {
            val moved = currentSuttaIds.removeAt(fromIndex)
            currentSuttaIds.add(toIndex, moved)
            viewModelScope.launch {
                pirithRepository.updateSuttasOrderInCollection(collectionId, currentSuttaIds)
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

    fun setShowEnglishNames(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowEnglishNames(enabled)
        }
    }
}
