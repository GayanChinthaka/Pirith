package com.pokerlanka.pirith.domain.model

data class Sutta(
    val id: Int,
    val titlePali: String,
    val titleSinhala: String,
    val category: String,
    val orderIndex: Int,
    val isFavorite: Boolean
)
