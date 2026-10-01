package com.pokerlanka.pirith.data.repository

import com.pokerlanka.pirith.data.local.dao.SuttaDao
import com.pokerlanka.pirith.data.local.dao.VerseDao
import com.pokerlanka.pirith.data.preferences.UserPreferencesRepository
import com.pokerlanka.pirith.domain.model.Sutta
import com.pokerlanka.pirith.domain.model.SuttaCollection
import com.pokerlanka.pirith.domain.model.Verse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PirithRepositoryImpl(
    private val suttaDao: SuttaDao,
    private val verseDao: VerseDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : PirithRepository {

    override fun getAllSuttas(): Flow<List<Sutta>> =
        suttaDao.getAllSuttas().map { list -> list.map { it.toDomain() } }

    override fun getSuttasByCategory(category: String): Flow<List<Sutta>> =
        suttaDao.getSuttasByCategory(category).map { list -> list.map { it.toDomain() } }

    override fun getFavoriteSuttas(): Flow<List<Sutta>> =
        suttaDao.getFavoriteSuttas().map { list -> list.map { it.toDomain() } }

    override fun getSuttaById(id: Int): Flow<Sutta?> =
        suttaDao.getSuttaById(id).map { it?.toDomain() }

    override suspend fun toggleFavorite(id: Int, currentFavorite: Boolean) {
        val newStatus = if (currentFavorite) 0 else 1
        suttaDao.setFavorite(id, newStatus)
    }

    override fun searchSuttas(query: String): Flow<List<Sutta>> =
        suttaDao.searchSuttas(query).map { list -> list.map { it.toDomain() } }

    override fun getCategories(): Flow<List<String>> =
        suttaDao.getCategories()

    override fun getVersesForSutta(suttaId: Int): Flow<List<Verse>> =
        verseDao.getVersesBySuttaId(suttaId).map { list -> list.map { it.toDomain() } }

    override fun searchVerses(query: String): Flow<List<Verse>> =
        verseDao.searchVerses(query).map { list -> list.map { it.toDomain() } }

    override fun searchSuttaIdsByVerseContent(query: String): Flow<List<Int>> =
        verseDao.searchSuttaIdsByVerseContent(query)

    override fun getCustomCollections(): Flow<List<SuttaCollection>> =
        userPreferencesRepository.customCollectionsFlow

    override suspend fun createCustomCollection(name: String): SuttaCollection? =
        userPreferencesRepository.createCollection(name)

    override suspend fun renameCustomCollection(collectionId: String, newName: String): Boolean =
        userPreferencesRepository.renameCollection(collectionId, newName)

    override suspend fun updateCollectionsOrder(reorderedList: List<SuttaCollection>) =
        userPreferencesRepository.updateCollectionsOrder(reorderedList)

    override suspend fun deleteCustomCollection(collectionId: String) =
        userPreferencesRepository.deleteCollection(collectionId)

    override suspend fun toggleSuttaInCollection(collectionId: String, suttaId: Int) =
        userPreferencesRepository.toggleSuttaInCollection(collectionId, suttaId)
}

