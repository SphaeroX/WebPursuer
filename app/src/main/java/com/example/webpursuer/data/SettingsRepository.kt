package com.example.webpursuer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
    }

    val apiKey: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[OPENROUTER_API_KEY]
        }

    val model: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[OPENROUTER_MODEL] ?: "google/gemini-pro" // Default model
        }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[OPENROUTER_API_KEY] = key
        }
    }

    suspend fun saveModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[OPENROUTER_MODEL] = model
        }
    }
}
