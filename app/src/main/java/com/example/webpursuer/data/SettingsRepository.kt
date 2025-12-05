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
        val NOTIFICATIONS_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("notifications_enabled")
        val REPORT_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("report_enabled")
        val REPORT_TIME_HOUR = androidx.datastore.preferences.core.intPreferencesKey("report_time_hour")
        val LAST_REPORT_TIME = androidx.datastore.preferences.core.longPreferencesKey("last_report_time")
        val REPORT_MONITOR_SELECTION = androidx.datastore.preferences.core.stringSetPreferencesKey("report_monitor_selection")
    }

    val apiKey: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[OPENROUTER_API_KEY]
        }

    val model: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[OPENROUTER_MODEL] ?: "google/gemini-2.5-flash-lite" // Default model
        }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }

    val reportEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[REPORT_ENABLED] ?: false
        }

    val reportTimeHour: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[REPORT_TIME_HOUR] ?: 8 // Default 8 AM
        }

    val lastReportTime: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_REPORT_TIME] ?: 0L
        }

    val reportMonitorSelection: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[REPORT_MONITOR_SELECTION] ?: emptySet()
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

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveReportEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REPORT_ENABLED] = enabled
        }
    }

    suspend fun saveReportTimeHour(hour: Int) {
        context.dataStore.edit { preferences ->
            preferences[REPORT_TIME_HOUR] = hour
        }
    }

    suspend fun saveLastReportTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_REPORT_TIME] = time
        }
    }

    suspend fun saveReportMonitorSelection(selection: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[REPORT_MONITOR_SELECTION] = selection
        }
    }
}
