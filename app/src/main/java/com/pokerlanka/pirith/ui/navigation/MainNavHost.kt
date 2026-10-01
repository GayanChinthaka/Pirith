package com.pokerlanka.pirith.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pokerlanka.pirith.ui.home.HomeScreen
import com.pokerlanka.pirith.ui.home.HomeViewModel
import com.pokerlanka.pirith.ui.reader.ReaderScreen
import com.pokerlanka.pirith.ui.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainNavHost(navController: NavHostController) {
    val homeViewModel: HomeViewModel = koinViewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                uiState = homeUiState,
                onCategorySelected = { category -> homeViewModel.onCategorySelected(category) },
                onSearchQueryChanged = { query -> homeViewModel.onSearchQueryChanged(query) },
                onToggleFavoritesOnly = { homeViewModel.onToggleFavoritesOnly() },
                onToggleFavorite = { sutta -> homeViewModel.onToggleSuttaFavorite(sutta) },
                onSuttaClick = { suttaId -> navController.navigate(Screen.Reader.createRoute(suttaId)) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Reader.route) { backStackEntry ->
            val suttaId = backStackEntry.arguments?.getString("suttaId")?.toIntOrNull() ?: return@composable
            ReaderScreen(suttaId = suttaId, navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.AppearanceSettings.route) {
            com.pokerlanka.pirith.ui.settings.AppearanceSettingsScreen(navController = navController)
        }
        composable(Screen.ReaderDefaultsSettings.route) {
            com.pokerlanka.pirith.ui.settings.ReaderDefaultsSettingsScreen(navController = navController)
        }
        composable(Screen.GuideSettings.route) {
            com.pokerlanka.pirith.ui.settings.GuideSettingsScreen(navController = navController)
        }
        composable(Screen.UpdaterSettings.route) {
            com.pokerlanka.pirith.ui.settings.UpdaterSettingsScreen(navController = navController)
        }
        composable(Screen.AboutSettings.route) {
            com.pokerlanka.pirith.ui.settings.AboutSettingsScreen(navController = navController)
        }
    }
}
