package com.pokerlanka.pirith.data.repository

import com.pokerlanka.pirith.domain.model.Sutta
import com.pokerlanka.pirith.domain.model.Verse
import kotlinx.coroutines.flow.Flow

interface PirithRepository {
    fun getAllSuttas(): Flow<List<Sutta>>
    fun getSuttasByCategory(category: String): Flow<List<Sutta>>
    fun getFavoriteSuttas(): Flow<List<Sutta>>
    fun getSuttaById(id: Int): Flow<Sutta?>
    suspend fun toggleFavorite(id: Int, currentFavorite: Boolean)
    fun searchSuttas(query: String): Flow<List<Sutta>>
    fun getCategories(): Flow<List<String>>
    fun getVersesForSutta(suttaId: Int): Flow<List<Verse>>
    fun searchVerses(query: String): Flow<List<Verse>>
    fun searchSuttaIdsByVerseContent(query: String): Flow<List<Int>>

    fun getCustomCollections(): Flow<List<com.pokerlanka.pirith.domain.model.SuttaCollection>>
    suspend fun createCustomCollection(name: String): com.pokerlanka.pirith.domain.model.SuttaCollection?
    suspend fun renameCustomCollection(collectionId: String, newName: String): Boolean
    suspend fun updateCollectionsOrder(reorderedList: List<com.pokerlanka.pirith.domain.model.SuttaCollection>)
    suspend fun deleteCustomCollection(collectionId: String)
    suspend fun toggleSuttaInCollection(collectionId: String, suttaId: Int)
    suspend fun updateSuttasOrderInCollection(collectionId: String, newSuttaIds: List<Int>)

    fun getTabOrder(): Flow<List<String>>
    suspend fun updateTabOrder(tabKeys: List<String>)

    fun getUserPreferences(): Flow<com.pokerlanka.pirith.data.preferences.UserPreferences>
}

