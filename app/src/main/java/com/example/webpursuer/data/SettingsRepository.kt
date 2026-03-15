package com.murmli.webpursuer.data

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
        @Deprecated("Use OPENROUTER_REPORT_MODEL or OPENROUTER_MONITOR_MODEL")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val OPENROUTER_REPORT_MODEL = stringPreferencesKey("openrouter_report_model")
        val OPENROUTER_MONITOR_MODEL = stringPreferencesKey("openrouter_monitor_model")
        val OPENROUTER_SEARCH_MODEL = stringPreferencesKey("openrouter_search_model")

        val NOTIFICATIONS_ENABLED =
                androidx.datastore.preferences.core.booleanPreferencesKey("notifications_enabled")
        val REPORT_ENABLED =
                androidx.datastore.preferences.core.booleanPreferencesKey("report_enabled")
        val REPORT_TIME_HOUR =
                androidx.datastore.preferences.core.intPreferencesKey("report_time_hour")
        val LAST_REPORT_TIME =
                androidx.datastore.preferences.core.longPreferencesKey("last_report_time")
        val REPORT_MONITOR_SELECTION =
                androidx.datastore.preferences.core.stringSetPreferencesKey(
                        "report_monitor_selection"
                )
        val DIFF_FILTER_MODE = stringPreferencesKey("diff_filter_mode")
        val DIFF_VIEW_MODE = stringPreferencesKey("diff_view_mode") // "DIFF" or "RENDERED"
        
        val RECENT_CHANGES_PAGE_SIZE = androidx.datastore.preferences.core.intPreferencesKey("recent_changes_page_size")
        val RECENT_CHANGES_SORT_ORDER = stringPreferencesKey("recent_changes_sort_order") // "DESC" or "ASC"
        
        val WORKER_QUIET_START_HOUR = androidx.datastore.preferences.core.intPreferencesKey("worker_quiet_start_hour")
        val WORKER_QUIET_END_HOUR = androidx.datastore.preferences.core.intPreferencesKey("worker_quiet_end_hour")
        val WORKER_QUIET_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("worker_quiet_enabled")
        
        val NOTIFICATION_QUIET_START_HOUR = androidx.datastore.preferences.core.intPreferencesKey("notification_quiet_start_hour")
        val NOTIFICATION_QUIET_END_HOUR = androidx.datastore.preferences.core.intPreferencesKey("notification_quiet_end_hour")
        val NOTIFICATION_QUIET_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("notification_quiet_enabled")
    }

    val apiKey: Flow<String?> =
            context.dataStore.data.map { preferences -> preferences[OPENROUTER_API_KEY] }

    val model: Flow<String> =
            context.dataStore.data.map { preferences ->
                preferences[OPENROUTER_MODEL] ?: "google/gemini-3-flash-preview" // Default model
            }

    val reportModel: Flow<String> =
            context.dataStore.data.map { preferences ->
                preferences[OPENROUTER_REPORT_MODEL]
                        ?: preferences[OPENROUTER_MODEL] // Fallback to old setting if exists
                         ?: "google/gemini-3-flash-preview"
            }

    val monitorModel: Flow<String> =
            context.dataStore.data.map { preferences ->
                preferences[OPENROUTER_MONITOR_MODEL] ?: "google/gemini-3-flash-preview"
            }

    val searchModel: Flow<String> =
            context.dataStore.data.map { preferences ->
                preferences[OPENROUTER_SEARCH_MODEL] ?: "google/gemini-3-flash-preview"
            }

    val notificationsEnabled: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[NOTIFICATIONS_ENABLED] ?: true }

    val reportEnabled: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[REPORT_ENABLED] ?: false }

    val reportTimeHour: Flow<Int> =
            context.dataStore.data.map { preferences ->
                preferences[REPORT_TIME_HOUR] ?: 8 // Default 8 AM
            }

    val lastReportTime: Flow<Long> =
            context.dataStore.data.map { preferences -> preferences[LAST_REPORT_TIME] ?: 0L }

    val reportMonitorSelection: Flow<Set<String>> =
            context.dataStore.data.map { preferences ->
                preferences[REPORT_MONITOR_SELECTION] ?: emptySet()
            }

    val diffFilterMode: Flow<String> =
            context.dataStore.data.map { preferences -> preferences[DIFF_FILTER_MODE] ?: "CHANGES" }

    val diffViewMode: Flow<String> =
            context.dataStore.data.map { preferences -> preferences[DIFF_VIEW_MODE] ?: "DIFF" }
            
    val recentChangesPageSize: Flow<Int> =
            context.dataStore.data.map { preferences -> preferences[RECENT_CHANGES_PAGE_SIZE] ?: 10 }
            
    val recentChangesSortOrder: Flow<String> =
            context.dataStore.data.map { preferences -> preferences[RECENT_CHANGES_SORT_ORDER] ?: "DESC" }

    val workerQuietStartHour: Flow<Int> =
            context.dataStore.data.map { preferences -> preferences[WORKER_QUIET_START_HOUR] ?: 22 } // Default 10 PM
            
    val workerQuietEndHour: Flow<Int> =
            context.dataStore.data.map { preferences -> preferences[WORKER_QUIET_END_HOUR] ?: 6 } // Default 6 AM
            
    val workerQuietEnabled: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[WORKER_QUIET_ENABLED] ?: false }
            
    val notificationQuietStartHour: Flow<Int> =
            context.dataStore.data.map { preferences -> preferences[NOTIFICATION_QUIET_START_HOUR] ?: 22 }
            
    val notificationQuietEndHour: Flow<Int> =
            context.dataStore.data.map { preferences -> preferences[NOTIFICATION_QUIET_END_HOUR] ?: 6 }
            
    val notificationQuietEnabled: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[NOTIFICATION_QUIET_ENABLED] ?: false }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences -> preferences[OPENROUTER_API_KEY] = key }
    }

    suspend fun saveModel(model: String) {
        context.dataStore.edit { preferences -> preferences[OPENROUTER_MODEL] = model }
    }

    suspend fun saveReportModel(model: String) {
        context.dataStore.edit { preferences -> preferences[OPENROUTER_REPORT_MODEL] = model }
    }

    suspend fun saveMonitorModel(model: String) {
        context.dataStore.edit { preferences -> preferences[OPENROUTER_MONITOR_MODEL] = model }
    }

    suspend fun saveSearchModel(model: String) {
        context.dataStore.edit { preferences -> preferences[OPENROUTER_SEARCH_MODEL] = model }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun saveReportEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[REPORT_ENABLED] = enabled }
    }

    suspend fun saveReportTimeHour(hour: Int) {
        context.dataStore.edit { preferences -> preferences[REPORT_TIME_HOUR] = hour }
    }

    suspend fun saveLastReportTime(time: Long) {
        context.dataStore.edit { preferences -> preferences[LAST_REPORT_TIME] = time }
    }

    suspend fun saveReportMonitorSelection(selection: Set<String>) {
        context.dataStore.edit { preferences -> preferences[REPORT_MONITOR_SELECTION] = selection }
    }

    suspend fun saveDiffFilterMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[DIFF_FILTER_MODE] = mode }
    }

    suspend fun saveDiffViewMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[DIFF_VIEW_MODE] = mode }
    }
    
    suspend fun saveRecentChangesPageSize(size: Int) {
        context.dataStore.edit { preferences -> preferences[RECENT_CHANGES_PAGE_SIZE] = size }
    }
    
    suspend fun saveRecentChangesSortOrder(order: String) {
        context.dataStore.edit { preferences -> preferences[RECENT_CHANGES_SORT_ORDER] = order }
    }

    suspend fun saveWorkerQuietStartHour(hour: Int) {
        context.dataStore.edit { preferences -> preferences[WORKER_QUIET_START_HOUR] = hour }
    }
    
    suspend fun saveWorkerQuietEndHour(hour: Int) {
        context.dataStore.edit { preferences -> preferences[WORKER_QUIET_END_HOUR] = hour }
    }
    
    suspend fun saveWorkerQuietEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[WORKER_QUIET_ENABLED] = enabled }
    }
    
    suspend fun saveNotificationQuietStartHour(hour: Int) {
        context.dataStore.edit { preferences -> preferences[NOTIFICATION_QUIET_START_HOUR] = hour }
    }
    
    suspend fun saveNotificationQuietEndHour(hour: Int) {
        context.dataStore.edit { preferences -> preferences[NOTIFICATION_QUIET_END_HOUR] = hour }
    }
    
    suspend fun saveNotificationQuietEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[NOTIFICATION_QUIET_ENABLED] = enabled }
    }

    fun isQuietTime(startHour: Int, endHour: Int): Boolean {
        val now = java.util.Calendar.getInstance()
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        
        return if (startHour < endHour) {
            currentHour in startHour until endHour
        } else {
            // Over midnight case (e.g., 22 to 06)
            currentHour >= startHour || currentHour < endHour
        }
    }
}
