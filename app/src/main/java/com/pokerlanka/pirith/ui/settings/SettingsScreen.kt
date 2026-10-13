package com.pokerlanka.pirith.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.pirith.BuildConfig
import com.pokerlanka.pirith.domain.model.AppTheme
import com.pokerlanka.pirith.domain.model.ReaderMode
import com.pokerlanka.pirith.ui.component.Material3SettingsGroup
import com.pokerlanka.pirith.ui.component.Material3SettingsItem
import com.pokerlanka.pirith.ui.navigation.Screen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val preferences by viewModel.preferences.collectAsState()
    val scrollState = rememberScrollState()
    val showEnglish = preferences.showEnglishNames

    val currentThemeName = when (preferences.appTheme) {
        AppTheme.SYSTEM -> if (showEnglish) "පද්ධතිය (System)" else "පද්ධතිය"
        AppTheme.LIGHT -> if (showEnglish) "දිවා (Light)" else "දිවා"
        AppTheme.DARK -> if (showEnglish) "රාත්‍රී (Dark)" else "රාත්‍රී"
        AppTheme.SEPIA -> if (showEnglish) "සෙපියා (Sepia)" else "සෙපියා"
    }

    val currentReaderModeName = when (preferences.readerMode) {
        ReaderMode.PARALLEL -> "දෙබසම"
        ReaderMode.PALI_ONLY -> "පාලි"
        ReaderMode.SINHALA_ONLY -> "සිංහල"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showEnglish) "සැකසුම් (Settings)" else "සැකසුම්",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Settings Menu Items (Mixora styled Material3SettingsGroup)
            Material3SettingsGroup(
                title = "ප්‍රධාන සැකසුම්",
                items = listOf(
                    // 1. Appearance & Display
                    Material3SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = { Text(if (showEnglish) "පෙනුම සහ සංදර්ශනය (Appearance & Display)" else "පෙනුම සහ සංදර්ශනය") },
                        description = { Text("තේමාව: $currentThemeName • සංදර්ශන සැකසුම්") },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            navController.navigate(Screen.AppearanceSettings.route)
                        }
                    ),
                    // 2. Reader Defaults
                    Material3SettingsItem(
                        icon = Icons.Default.Tune,
                        title = { Text(if (showEnglish) "කියවීමේ පෙරනිමි (Reader Defaults)" else "කියවීමේ පෙරනිමි") },
                        description = { Text("$currentReaderModeName • අකුරු: ${(preferences.fontSizeScale * 100).toInt()}%") },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            navController.navigate(Screen.ReaderDefaultsSettings.route)
                        }
                    ),
                    // 3. Manage Collections / Playlists
                    Material3SettingsItem(
                        icon = Icons.Default.Bookmarks,
                        title = { Text(if (showEnglish) "පිරිත් එකතු (Collections / Playlists)" else "පිරිත් එකතු") },
                        description = { Text("එකතු කළමනාකරණය, නම් වෙනස් කිරීම සහ පෙළගැස්ම") },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            navController.navigate(Screen.CollectionsSettings.route)
                        }
                    )
                )
            )

            // General & Information Group
            Material3SettingsGroup(
                title = "තොරතුරු සහ ආධාරක",
                items = listOf(
                    // 3. Guide
                    Material3SettingsItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = { Text(if (showEnglish) "භාවිත මාර්ගෝපදේශය (Guide)" else "භාවිත මාර්ගෝපදේශය") },
                        description = { Text("100% නොබැඳි භාවිතය, සෙවීම සහ විශේෂාංග") },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            navController.navigate(Screen.GuideSettings.route)
                        }
                    ),
                    // 4. Updater
                    Material3SettingsItem(
                        icon = Icons.Default.SystemUpdate,
                        title = { Text(if (showEnglish) "යාවත්කාලීන (Updater)" else "යාවත්කාලීන") },
                        description = { Text("අනුවාදය v${BuildConfig.VERSION_NAME} • යාවත්කාලීන පරීක්ෂාව") },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            navController.navigate(Screen.UpdaterSettings.route)
                        }
                    ),
                    // 5. About
                    Material3SettingsItem(
                        icon = Icons.Default.Info,
                        title = { Text(if (showEnglish) "තොරතුරු (About)" else "තොරතුරු") },
                        description = { Text("සංවර්ධක, Pokerlanka, මූලාශ්‍ර සහ කෘතඥතාව") },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            navController.navigate(Screen.AboutSettings.route)
                        }
                    )
                )
            )

            Spacer(Modifier.height(16.dp))

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = (if (showEnglish) "පිරිත් (Pirith)" else "පිරිත්") + " v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "com.pokerlanka.pirith",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
