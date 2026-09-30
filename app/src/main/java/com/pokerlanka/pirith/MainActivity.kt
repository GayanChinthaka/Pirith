package com.pokerlanka.pirith

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.pokerlanka.pirith.data.preferences.UserPreferences
import com.pokerlanka.pirith.data.preferences.UserPreferencesRepository
import com.pokerlanka.pirith.ui.navigation.MainNavHost
import com.pokerlanka.pirith.ui.theme.PirithTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val preferencesRepository: UserPreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences by preferencesRepository.userPreferencesFlow.collectAsState(
                initial = UserPreferences()
            )

            // Dynamically manage FLAG_KEEP_SCREEN_ON on the current window
            if (preferences.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            PirithTheme(appTheme = preferences.appTheme) {
                val navController = rememberNavController()
                MainNavHost(navController = navController)
            }
        }
    }
}
