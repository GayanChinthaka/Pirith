package com.pokerlanka.pirith.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Reader : Screen("reader/{suttaId}") {
        fun createRoute(suttaId: Int): String = "reader/$suttaId"
    }
    data object Settings : Screen("settings")
    data object AppearanceSettings : Screen("settings/appearance")
    data object ReaderDefaultsSettings : Screen("settings/reader_defaults")
    data object GuideSettings : Screen("settings/guide")
    data object UpdaterSettings : Screen("settings/updater")
    data object AboutSettings : Screen("settings/about")
}
