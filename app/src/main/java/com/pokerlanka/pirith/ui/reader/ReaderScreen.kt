package com.pokerlanka.pirith.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pokerlanka.pirith.domain.model.AppTheme
import com.pokerlanka.pirith.domain.model.ReaderMode
import com.pokerlanka.pirith.domain.model.Verse
import com.pokerlanka.pirith.ui.theme.PirithTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    suttaId: Int,
    navController: NavController,
    viewModel: ReaderViewModel = koinViewModel()
) {
    LaunchedEffect(suttaId) {
        viewModel.loadSutta(suttaId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Auto-scroll loop
    LaunchedEffect(uiState.isAutoScrolling, uiState.autoScrollSpeed) {
        if (uiState.isAutoScrolling) {
            val pixelsPerStep = 2f * uiState.autoScrollSpeed
            while (isActive && uiState.isAutoScrolling) {
                listState.scrollBy(pixelsPerStep)
                delay(16L) // ~60 FPS smooth scroll
            }
        }
    }

    PirithTheme(appTheme = uiState.preferences.appTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.sutta?.titleSinhala ?: "පිරිත් සූත්‍රය",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            uiState.sutta?.let {
                                Text(
                                    text = it.titlePali,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Auto Scroll Toggle
                        IconButton(onClick = { viewModel.toggleAutoScroll() }) {
                            Icon(
                                imageVector = if (uiState.isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isAutoScrolling) "Pause Auto-Scroll" else "Start Auto-Scroll",
                                tint = if (uiState.isAutoScrolling) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Favorite Toggle
                        uiState.sutta?.let { sutta ->
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    imageVector = if (sutta.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Toggle favorite",
                                    tint = if (sutta.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Reader Settings Sheet Button
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Reader Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (uiState.sutta == null) {
                    Text(
                        text = "සූත්‍රය හමු නොවීය",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Segmented Language / Mode Switcher Bar
                        ReaderModeSegmentedControl(
                            currentMode = uiState.preferences.readerMode,
                            onModeSelected = { mode -> viewModel.setReaderMode(mode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        )

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.verses,
                                key = { _, item -> item.id }
                            ) { index, verse ->
                                VerseCard(
                                    index = index + 1,
                                    verse = verse,
                                    readerMode = uiState.preferences.readerMode,
                                    fontScale = uiState.preferences.fontSizeScale,
                                    lineHeightMultiplier = uiState.preferences.lineHeightMultiplier
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(72.dp))
                            }
                        }
                    }

                    // Floating Auto-Scroll Speed Adjuster Capsule
                    AnimatedVisibility(
                        visible = uiState.isAutoScrolling,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                    ) {
                        FloatingAutoScrollControls(
                            currentSpeed = uiState.autoScrollSpeed,
                            onSpeedSelected = { speed -> viewModel.setAutoScrollSpeed(speed) },
                            onPauseClick = { viewModel.toggleAutoScroll() }
                        )
                    }
                }
            }

            // Reader Settings BottomSheet
            if (showSettingsSheet) {
                ReaderSettingsBottomSheet(
                    preferences = uiState.preferences,
                    onDismiss = { showSettingsSheet = false },
                    onFontScaleChange = { scale -> viewModel.setFontSizeScale(scale) },
                    onLineHeightChange = { mult -> viewModel.setLineHeightMultiplier(mult) },
                    onThemeChange = { theme -> viewModel.setAppTheme(theme) },
                    onKeepScreenOnChange = { enabled -> viewModel.setKeepScreenOn(enabled) },
                    onReaderModeChange = { mode -> viewModel.setReaderMode(mode) }
                )
            }
        }
    }
}

@Composable
fun ReaderModeSegmentedControl(
    currentMode: ReaderMode,
    onModeSelected: (ReaderMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        Triple(ReaderMode.PARALLEL, "දෙබසම", Icons.Default.Translate),
        Triple(ReaderMode.PALI_ONLY, "පාළි", Icons.AutoMirrored.Filled.MenuBook),
        Triple(ReaderMode.SINHALA_ONLY, "සිංහල", Icons.Default.AutoStories)
    )

    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEach { (mode, label, icon) ->
                val isSelected = currentMode == mode
                val targetBgColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
                val targetContentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                val bgColor by animateColorAsState(
                    targetValue = targetBgColor,
                    label = "segmentBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = targetContentColor,
                    label = "segmentContent"
                )

                Surface(
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(19.dp),
                    color = bgColor,
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)) else null,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerseCard(
    index: Int,
    verse: Verse,
    readerMode: ReaderMode,
    fontScale: Float,
    lineHeightMultiplier: Float
) {
    val paliFontSize = (17 * fontScale).sp
    val paliLineHeight = (26 * fontScale * lineHeightMultiplier).sp
    val sinhalaFontSize = (15 * fontScale).sp
    val sinhalaLineHeight = (23 * fontScale * lineHeightMultiplier).sp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Verse Number Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "ගාථා අංක $index",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pali Verse
            if (readerMode == ReaderMode.PARALLEL || readerMode == ReaderMode.PALI_ONLY) {
                Text(
                    text = verse.paliText,
                    fontSize = paliFontSize,
                    lineHeight = paliLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Divider in parallel mode
            if (readerMode == ReaderMode.PARALLEL && verse.sinhalaMeaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Sinhala Meaning
            if (readerMode == ReaderMode.PARALLEL || readerMode == ReaderMode.SINHALA_ONLY) {
                Text(
                    text = verse.sinhalaMeaning,
                    fontSize = sinhalaFontSize,
                    lineHeight = sinhalaLineHeight,
                    fontWeight = FontWeight.Normal,
                    color = if (readerMode == ReaderMode.PARALLEL)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun FloatingAutoScrollControls(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onPauseClick: () -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f)

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            speeds.forEach { speed ->
                val isSelected = kotlin.math.abs(currentSpeed - speed) < 0.05f
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable { onSpeedSelected(speed) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (speed == 1.0f) "1.0x" else "${speed}x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsBottomSheet(
    preferences: com.pokerlanka.pirith.data.preferences.UserPreferences,
    onDismiss: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onReaderModeChange: (ReaderMode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "කියවීමේ සැකසුම් (Reader Settings)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Display Mode
            Text(
                text = "පෙන්වන ආකාරය (Display Mode)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = preferences.readerMode == ReaderMode.PARALLEL,
                    onClick = { onReaderModeChange(ReaderMode.PARALLEL) },
                    label = { Text("සමාන්තර") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = preferences.readerMode == ReaderMode.PALI_ONLY,
                    onClick = { onReaderModeChange(ReaderMode.PALI_ONLY) },
                    label = { Text("පාලි පමණි") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = preferences.readerMode == ReaderMode.SINHALA_ONLY,
                    onClick = { onReaderModeChange(ReaderMode.SINHALA_ONLY) },
                    label = { Text("සිංහල පමණි") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reading Theme Switcher
            Text(
                text = "තේමාව (Theme)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    val isSelected = preferences.appTheme == theme
                    FilterChip(
                        selected = isSelected,
                        onClick = { onThemeChange(theme) },
                        label = {
                            Text(
                                when (theme) {
                                    AppTheme.SYSTEM -> "පද්ධතිය (System)"
                                    AppTheme.LIGHT -> "දිවා (Light)"
                                    AppTheme.DARK -> "රාත්‍රී (Dark)"
                                    AppTheme.SEPIA -> "සෙපියා (Sepia)"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Font Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "අකුරු ප්‍රමාණය (Font Size)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(preferences.fontSizeScale * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = preferences.fontSizeScale,
                onValueChange = onFontScaleChange,
                valueRange = 0.8f..1.8f,
                steps = 9
            )

            // Live Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "නමෝ තස්ස භගවතෝ අරහතෝ සම්මාසම්බුද්ධස්ස",
                    fontSize = (15 * preferences.fontSizeScale).sp,
                    lineHeight = (22 * preferences.fontSizeScale * preferences.lineHeightMultiplier).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Line Spacing Multiplier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "පේළි අතර පරතරය (Line Spacing)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.1fx", preferences.lineHeightMultiplier),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = preferences.lineHeightMultiplier,
                onValueChange = onLineHeightChange,
                valueRange = 1.0f..2.0f,
                steps = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Keep Screen On Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "තිරය නොනිවා තබාගන්න",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "කියවන අතරතුර තිරය ක්‍රියා විරහිත නොවේ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = preferences.keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange
                )
            }
        }
    }
}
