package com.pokerlanka.pirith.domain.model

data class CollectionItem(
    val key: String,         // e.g. "default_all", "default_wandana", "custom_<uuid>"
    val name: String,        // Display name: "සියල්ල", "වන්දනා", or custom name
    val isDefault: Boolean,  // true if standard category, false if custom
    val suttaCount: Int,     // number of suttas in this collection
    val customCollection: SuttaCollection? = null // non-null for custom collections
)
