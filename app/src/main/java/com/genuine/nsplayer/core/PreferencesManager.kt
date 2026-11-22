package com.genuine.nsplayer.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ns_player_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_THEME_STYLE = stringPreferencesKey("theme_style")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_HARDWARE_ACCEL = booleanPreferencesKey("hardware_accel")
        val KEY_BUFFER_SIZE = stringPreferencesKey("buffer_size")
        val KEY_DEFAULT_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val KEY_PARENTAL_PIN = stringPreferencesKey("parental_pin")
        val KEY_PARENTAL_ENABLED = booleanPreferencesKey("parental_enabled")
        val KEY_ACTIVE_PLAYLIST_ID = longPreferencesKey("active_playlist_id")
    }

    val themeStyle: Flow<String> = context.dataStore.data.map { it[KEY_THEME_STYLE] ?: "Obsidian" }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: true }
    val isHardwareAccel: Flow<Boolean> = context.dataStore.data.map { it[KEY_HARDWARE_ACCEL] ?: true }
    val bufferSize: Flow<String> = context.dataStore.data.map { it[KEY_BUFFER_SIZE] ?: "Normal" }
    val defaultAspectRatio: Flow<String> = context.dataStore.data.map { it[KEY_DEFAULT_ASPECT_RATIO] ?: "FIT" }
    val parentalPin: Flow<String?> = context.dataStore.data.map { it[KEY_PARENTAL_PIN] }
    val isParentalEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_PARENTAL_ENABLED] ?: false }
    val activePlaylistId: Flow<Long?> = context.dataStore.data.map { it[KEY_ACTIVE_PLAYLIST_ID] }

    suspend fun setThemeStyle(style: String) {
        context.dataStore.edit { it[KEY_THEME_STYLE] = style }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setHardwareAccel(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HARDWARE_ACCEL] = enabled }
    }

    suspend fun setBufferSize(size: String) {
        context.dataStore.edit { it[KEY_BUFFER_SIZE] = size }
    }

    suspend fun setDefaultAspectRatio(ratio: String) {
        context.dataStore.edit { it[KEY_DEFAULT_ASPECT_RATIO] = ratio }
    }

    suspend fun setParentalPin(pin: String?) {
        context.dataStore.edit {
            if (pin != null) {
                it[KEY_PARENTAL_PIN] = pin
                it[KEY_PARENTAL_ENABLED] = true
            } else {
                it.remove(KEY_PARENTAL_PIN)
                it[KEY_PARENTAL_ENABLED] = false
            }
        }
    }

    suspend fun setActivePlaylistId(id: Long) {
        context.dataStore.edit { it[KEY_ACTIVE_PLAYLIST_ID] = id }
    }
}
