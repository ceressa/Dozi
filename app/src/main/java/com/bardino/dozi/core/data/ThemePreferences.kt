package com.bardino.dozi.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 🎨 Tema Tercih Yönetimi (DataStore)
 *
 * Kullanıcının tema tercihi (light/dark/system) lokal olarak saklanır
 * ve gerçek zamanlı olarak uygulamada kullanılır.
 */

// DataStore Extension
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

object ThemePreferences {

    private val THEME_KEY = stringPreferencesKey("theme_mode")

    /**
     * Tema tercihini kaydet
     * @param theme "light", "dark" veya "system"
     */
    suspend fun saveTheme(context: Context, theme: String) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    /**
     * Tema tercihini oku (Flow)
     * Default: "system" (sistem ayarını takip et)
     */
    fun getThemeFlow(context: Context): Flow<String> {
        return context.themeDataStore.data.map { preferences ->
            preferences[THEME_KEY] ?: "system"
        }
    }

    /**
     * Tema tercihini senkron oku
     * Not: Bu suspend function, coroutine içinde çağrılmalı
     */
    suspend fun getTheme(context: Context): String {
        var theme = "system"
        context.themeDataStore.data.collect { preferences ->
            theme = preferences[THEME_KEY] ?: "system"
        }
        return theme
    }
}
