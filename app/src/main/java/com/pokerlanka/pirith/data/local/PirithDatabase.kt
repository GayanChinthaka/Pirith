package com.pokerlanka.pirith.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pokerlanka.pirith.data.local.dao.SuttaDao
import com.pokerlanka.pirith.data.local.dao.VerseDao
import com.pokerlanka.pirith.data.local.entity.SuttaEntity
import com.pokerlanka.pirith.data.local.entity.VerseEntity

@Database(
    entities = [SuttaEntity::class, VerseEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PirithDatabase : RoomDatabase() {
    abstract fun suttaDao(): SuttaDao
    abstract fun verseDao(): VerseDao

    companion object {
        const val DATABASE_NAME = "pirith.db"
        const val ASSET_PATH = "databases/pirith.db"
    }
}
