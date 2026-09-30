package com.pokerlanka.pirith.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pokerlanka.pirith.domain.model.Verse

@Entity(
    tableName = "verses",
    foreignKeys = [
        ForeignKey(
            entity = SuttaEntity::class,
            parentColumns = ["id"],
            childColumns = ["suttaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["suttaId"])]
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val suttaId: Int,
    val verseNumber: Int,
    val paliText: String,
    val sinhalaMeaning: String
) {
    fun toDomain(): Verse = Verse(
        id = id,
        suttaId = suttaId,
        verseNumber = verseNumber,
        paliText = paliText,
        sinhalaMeaning = sinhalaMeaning
    )
}
