package com.pokerlanka.pirith.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pokerlanka.pirith.domain.model.AppTheme
import com.pokerlanka.pirith.domain.model.ReaderMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val fontSizeScale: Float = 1.0f,
    val lineHeightMultiplier: Float = 1.4f,
    val readerMode: ReaderMode = ReaderMode.PARALLEL,
    val autoScrollSpeed: Float = 0.5f,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val keepScreenOn: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val FONT_SIZE_SCALE = floatPreferencesKey("font_size_scale")
        val LINE_HEIGHT_MULTIPLIER = floatPreferencesKey("line_height_multiplier")
        val READER_MODE = stringPreferencesKey("reader_mode")
        val AUTO_SCROLL_SPEED = floatPreferencesKey("auto_scroll_speed")
        val APP_THEME = stringPreferencesKey("app_theme")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val fontSize = prefs[Keys.FONT_SIZE_SCALE] ?: 1.0f
            val lineHeight = prefs[Keys.LINE_HEIGHT_MULTIPLIER] ?: 1.4f
            val modeStr = prefs[Keys.READER_MODE] ?: ReaderMode.PARALLEL.name
            val mode = runCatching { ReaderMode.valueOf(modeStr) }.getOrDefault(ReaderMode.PARALLEL)
            val rawSpeed = prefs[Keys.AUTO_SCROLL_SPEED] ?: 0.5f
            val speed = if (rawSpeed > 1.0f || rawSpeed < 0.25f) 0.5f else rawSpeed
            val themeStr = prefs[Keys.APP_THEME] ?: AppTheme.SYSTEM.name
            val theme = runCatching { AppTheme.valueOf(themeStr) }.getOrDefault(AppTheme.SYSTEM)
            val keepScreen = prefs[Keys.KEEP_SCREEN_ON] ?: true

            UserPreferences(
                fontSizeScale = fontSize,
                lineHeightMultiplier = lineHeight,
                readerMode = mode,
                autoScrollSpeed = speed,
                appTheme = theme,
                keepScreenOn = keepScreen
            )
        }

    suspend fun setFontSizeScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SIZE_SCALE] = scale }
    }

    suspend fun setLineHeightMultiplier(multiplier: Float) {
        context.dataStore.edit { it[Keys.LINE_HEIGHT_MULTIPLIER] = multiplier }
    }

    suspend fun setReaderMode(mode: ReaderMode) {
        context.dataStore.edit { it[Keys.READER_MODE] = mode.name }
    }

    suspend fun setAutoScrollSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.AUTO_SCROLL_SPEED] = speed }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }
}
