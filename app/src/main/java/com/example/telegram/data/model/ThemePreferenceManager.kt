package com.example.telegram.data.model

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ThemePreferenceManager {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    fun getDarkMode(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .map { preferences -> preferences[DARK_MODE_KEY] ?: false }
    }
}
