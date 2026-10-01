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
import com.pokerlanka.pirith.domain.model.SuttaCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        val CUSTOM_COLLECTIONS = stringPreferencesKey("custom_collections_json")
        val TAB_ORDER = stringPreferencesKey("tab_order_json")
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

    val customCollectionsFlow: Flow<List<SuttaCollection>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val jsonStr = prefs[Keys.CUSTOM_COLLECTIONS] ?: return@map emptyList()
            try {
                Json.decodeFromString<List<SuttaCollection>>(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun createCollection(name: String): SuttaCollection? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        
        var created: SuttaCollection? = null
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[Keys.CUSTOM_COLLECTIONS]
            val currentList = if (!jsonStr.isNullOrEmpty()) {
                try {
                    Json.decodeFromString<List<SuttaCollection>>(jsonStr).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
            
            // Check if collection with same name already exists (case-insensitive)
            if (currentList.any { it.name.equals(trimmed, ignoreCase = true) }) {
                created = null
                return@edit
            }
            
            val newCollection = SuttaCollection(
                id = java.util.UUID.randomUUID().toString(),
                name = trimmed,
                suttaIds = emptyList()
            )
            currentList.add(newCollection)
            val listToSave: List<SuttaCollection> = currentList
            prefs[Keys.CUSTOM_COLLECTIONS] = Json.encodeToString(listToSave)
            created = newCollection
        }
        return created
    }

    suspend fun renameCollection(collectionId: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        
        var success = false
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[Keys.CUSTOM_COLLECTIONS] ?: return@edit
            try {
                val currentList = Json.decodeFromString<List<SuttaCollection>>(jsonStr)
                // Check if another collection already has this name
                if (currentList.any { it.id != collectionId && it.name.equals(trimmed, ignoreCase = true) }) {
                    success = false
                    return@edit
                }
                val updated = currentList.map { coll ->
                    if (coll.id == collectionId) coll.copy(name = trimmed) else coll
                }
                prefs[Keys.CUSTOM_COLLECTIONS] = Json.encodeToString(updated)
                success = true
            } catch (_: Exception) {}
        }
        return success
    }

    suspend fun updateCollectionsOrder(reorderedList: List<SuttaCollection>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_COLLECTIONS] = Json.encodeToString(reorderedList)
        }
    }

    suspend fun deleteCollection(collectionId: String) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[Keys.CUSTOM_COLLECTIONS] ?: return@edit
            try {
                val currentList = Json.decodeFromString<List<SuttaCollection>>(jsonStr)
                val updated: List<SuttaCollection> = currentList.filter { it.id != collectionId }
                prefs[Keys.CUSTOM_COLLECTIONS] = Json.encodeToString(updated)
            } catch (_: Exception) {}
        }
    }

    suspend fun toggleSuttaInCollection(collectionId: String, suttaId: Int) {
        context.dataStore.edit { prefs ->
            val jsonStr = prefs[Keys.CUSTOM_COLLECTIONS] ?: return@edit
            try {
                val currentList = Json.decodeFromString<List<SuttaCollection>>(jsonStr)
                val updated: List<SuttaCollection> = currentList.map { coll ->
                    if (coll.id == collectionId) {
                        val ids = if (coll.suttaIds.contains(suttaId)) {
                            coll.suttaIds - suttaId
                        } else {
                            coll.suttaIds + suttaId
                        }
                        coll.copy(suttaIds = ids)
                    } else {
                        coll
                    }
                }
                prefs[Keys.CUSTOM_COLLECTIONS] = Json.encodeToString(updated)
            } catch (_: Exception) {}
        }
    }

    val tabOrderFlow: Flow<List<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val jsonStr = prefs[Keys.TAB_ORDER] ?: return@map emptyList()
            try {
                Json.decodeFromString<List<String>>(jsonStr)
            } catch (_: Exception) {
                emptyList()
            }
        }

    suspend fun updateTabOrder(tabKeys: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TAB_ORDER] = Json.encodeToString(tabKeys)
        }
    }
}

