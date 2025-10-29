// PreferencesManager.kt
package ru.sonic.zabbix.zbx

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
    }

    suspend fun saveTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = theme.ordinal
        }
    }

    fun getAppSettings(): Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val themeOrdinal = preferences[PreferencesKeys.THEME_MODE] ?: AppTheme.SYSTEM.ordinal
        val language = preferences[PreferencesKeys.LANGUAGE] ?: "English"

        AppSettings(
            theme = AppTheme.values()[themeOrdinal],
            language = language
        )
    }
}