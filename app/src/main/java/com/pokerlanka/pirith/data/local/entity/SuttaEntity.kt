package com.pokerlanka.pirith.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pokerlanka.pirith.domain.model.Sutta

@Entity(tableName = "suttas")
data class SuttaEntity(
    @PrimaryKey
    val id: Int,
    val titlePali: String,
    val titleSinhala: String,
    val category: String,
    val orderIndex: Int,
    val isFavorite: Int = 0
) {
    fun toDomain(): Sutta = Sutta(
        id = id,
        titlePali = titlePali,
        titleSinhala = titleSinhala,
        category = category,
        orderIndex = orderIndex,
        isFavorite = isFavorite == 1
    )
}
