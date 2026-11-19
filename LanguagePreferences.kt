package com.example.tiptime

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class AppPreferences(private val context: Context) {

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
    }

    val languageFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[LANGUAGE_KEY] ?: "en" }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language
        }
    }

    val darkThemeFlow: Flow<Boolean?> = context.dataStore.data
        .map { prefs -> prefs[DARK_THEME_KEY] }

    suspend fun saveDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_THEME_KEY] = isDark
        }
    }
}