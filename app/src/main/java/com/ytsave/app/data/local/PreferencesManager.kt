package com.ytsave.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val defaultFormat: Flow<String> = dataStore.data.map { preferences ->
        preferences[DEFAULT_FORMAT] ?: "video+audio"
    }


    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }

    suspend fun setDefaultFormat(format: String) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_FORMAT] = format
        }
    }


    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    companion object {
        val DEFAULT_FORMAT = stringPreferencesKey("default_format")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
