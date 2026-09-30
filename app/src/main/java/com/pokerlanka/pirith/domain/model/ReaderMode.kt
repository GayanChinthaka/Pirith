package com.pokerlanka.pirith.domain.model

enum class ReaderMode(val displayNameSinhala: String, val displayNameEnglish: String) {
    PARALLEL("පාලි සහ සිංහල", "Parallel"),
    PALI_ONLY("පාලි පමණි", "Pali Only"),
    SINHALA_ONLY("සිංහල පමණි", "Sinhala Only")
}
