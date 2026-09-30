package com.pokerlanka.pirith.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Reader : Screen("reader/{suttaId}") {
        fun createRoute(suttaId: Int): String = "reader/$suttaId"
    }
    data object Settings : Screen("settings")
}
