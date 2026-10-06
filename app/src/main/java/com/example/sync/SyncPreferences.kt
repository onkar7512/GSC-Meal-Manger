package com.example.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "google_sheet_sync_preferences")

object SyncPreferences {
    private val KEY_WEBHOOK_URL = stringPreferencesKey("google_sheet_webhook_url")
    private val KEY_AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
    private val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    private val KEY_LAST_SYNC_STATUS = stringPreferencesKey("last_sync_status")
    private val KEY_PENDING_RECORDS_COUNT = longPreferencesKey("pending_records_count")
    private val KEY_DELETED_STUDENT_ROLLS = stringSetPreferencesKey("deleted_student_roll_numbers")

    suspend fun addDeletedRollNumber(context: Context, rollNo: String) {
        if (rollNo.isBlank()) return
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_DELETED_STUDENT_ROLLS] ?: emptySet()
            preferences[KEY_DELETED_STUDENT_ROLLS] = current + rollNo.trim().lowercase()
        }
    }

    suspend fun getDeletedRollNumbers(context: Context): Set<String> {
        return context.dataStore.data.first()[KEY_DELETED_STUDENT_ROLLS] ?: emptySet()
    }

    suspend fun clearDeletedRollNumbers(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_DELETED_STUDENT_ROLLS)
        }
    }

    fun getWebhookUrl(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_WEBHOOK_URL] ?: ""
        }
    }

    suspend fun getWebhookUrlOnce(context: Context): String {
        return context.dataStore.data.first()[KEY_WEBHOOK_URL] ?: ""
    }

    suspend fun setWebhookUrl(context: Context, url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WEBHOOK_URL] = url.trim()
        }
    }

    fun isAutoSyncEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_AUTO_SYNC_ENABLED] ?: true // Default enabled
        }
    }

    suspend fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_SYNC_ENABLED] = enabled
        }
    }

    fun getLastSyncTime(context: Context): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_LAST_SYNC_TIME] ?: 0L
        }
    }

    fun getLastSyncStatus(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_LAST_SYNC_STATUS] ?: "Not synced yet"
        }
    }

    suspend fun updateSyncSuccess(context: Context, recordCount: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_TIME] = System.currentTimeMillis()
            preferences[KEY_LAST_SYNC_STATUS] = "Successfully synced $recordCount records"
        }
    }

    suspend fun updateSyncFailure(context: Context, errorMessage: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_STATUS] = "Failed: $errorMessage"
        }
    }
}
