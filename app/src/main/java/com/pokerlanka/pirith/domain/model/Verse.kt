package com.pokerlanka.pirith.domain.model

data class Verse(
    val id: Int,
    val suttaId: Int,
    val verseNumber: Int,
    val paliText: String,
    val sinhalaMeaning: String
)
