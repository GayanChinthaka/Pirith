package com.pokerlanka.pirith.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pokerlanka.pirith.data.local.entity.SuttaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuttaDao {

    @Query("SELECT * FROM suttas ORDER BY orderIndex ASC, id ASC")
    fun getAllSuttas(): Flow<List<SuttaEntity>>

    @Query("SELECT * FROM suttas WHERE category = :category ORDER BY orderIndex ASC, id ASC")
    fun getSuttasByCategory(category: String): Flow<List<SuttaEntity>>

    @Query("SELECT * FROM suttas WHERE isFavorite = 1 ORDER BY orderIndex ASC, id ASC")
    fun getFavoriteSuttas(): Flow<List<SuttaEntity>>

    @Query("SELECT * FROM suttas WHERE id = :id LIMIT 1")
    fun getSuttaById(id: Int): Flow<SuttaEntity?>

    @Query("UPDATE suttas SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Int)

    @Query("""
        SELECT * FROM suttas 
        WHERE titleSinhala LIKE '%' || :query || '%' 
           OR titlePali LIKE '%' || :query || '%'
        ORDER BY orderIndex ASC
    """)
    fun searchSuttas(query: String): Flow<List<SuttaEntity>>

    @Query("SELECT DISTINCT category FROM suttas ORDER BY id ASC")
    fun getCategories(): Flow<List<String>>
}
