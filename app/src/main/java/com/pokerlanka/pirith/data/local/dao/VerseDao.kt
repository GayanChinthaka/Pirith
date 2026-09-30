package com.pokerlanka.pirith.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.pokerlanka.pirith.data.local.entity.VerseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {

    @Query("SELECT * FROM verses WHERE suttaId = :suttaId ORDER BY verseNumber ASC, id ASC")
    fun getVersesBySuttaId(suttaId: Int): Flow<List<VerseEntity>>

    @Query("""
        SELECT * FROM verses 
        WHERE paliText LIKE '%' || :query || '%' 
           OR sinhalaMeaning LIKE '%' || :query || '%'
        ORDER BY suttaId ASC, verseNumber ASC
    """)
    fun searchVerses(query: String): Flow<List<VerseEntity>>

    @Query("""
        SELECT DISTINCT suttaId FROM verses 
        WHERE paliText LIKE '%' || :query || '%' 
           OR sinhalaMeaning LIKE '%' || :query || '%'
    """)
    fun searchSuttaIdsByVerseContent(query: String): Flow<List<Int>>
}
