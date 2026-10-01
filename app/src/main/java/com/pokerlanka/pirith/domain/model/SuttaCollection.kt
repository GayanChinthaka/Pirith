package com.pokerlanka.pirith.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SuttaCollection(
    val id: String,
    val name: String,
    val suttaIds: List<Int> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
