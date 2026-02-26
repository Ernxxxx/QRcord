package com.example.qrcord.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val AUTO_OPEN_URL = booleanPreferencesKey("auto_open_url")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val VIBRATE_ON_SCAN = booleanPreferencesKey("vibrate_on_scan")
        val CONTINUOUS_SCAN = booleanPreferencesKey("continuous_scan")
        val PRIVACY_NOTICE_SHOWN = booleanPreferencesKey("privacy_notice_shown")
        val SCAN_COUNT = intPreferencesKey("scan_count")
        val RATING_SHOWN = booleanPreferencesKey("rating_shown")
    }

    // Flow for auto open URL setting
    val autoOpenUrlFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_OPEN_URL] ?: false
        }

    // Flow for save history setting
    val saveHistoryFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SAVE_HISTORY] ?: true
        }

    // Flow for vibrate on scan setting
    val vibrateOnScanFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.VIBRATE_ON_SCAN] ?: true
        }

    // Flow for continuous scan setting
    val continuousScanFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CONTINUOUS_SCAN] ?: false
        }

    // Flow for scan count (for rating prompt)
    val scanCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SCAN_COUNT] ?: 0
        }

    // Flow for rating shown
    val ratingShownFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.RATING_SHOWN] ?: false
        }

    // Flow for privacy notice shown setting
    val privacyNoticeShownFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PRIVACY_NOTICE_SHOWN] ?: false
        }

    // Update auto open URL setting
    suspend fun updateAutoOpenUrl(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_OPEN_URL] = enabled
        }
    }

    // Update save history setting
    suspend fun updateSaveHistory(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVE_HISTORY] = enabled
        }
    }

    // Update vibrate on scan setting
    suspend fun updateVibrateOnScan(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATE_ON_SCAN] = enabled
        }
    }

    // Update continuous scan setting
    suspend fun updateContinuousScan(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTINUOUS_SCAN] = enabled
        }
    }

    // Increment scan count
    suspend fun incrementScanCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SCAN_COUNT] ?: 0
            preferences[PreferencesKeys.SCAN_COUNT] = current + 1
        }
    }

    // Update rating shown
    suspend fun updateRatingShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RATING_SHOWN] = shown
        }
    }

    // Update privacy notice shown setting
    suspend fun updatePrivacyNoticeShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIVACY_NOTICE_SHOWN] = shown
        }
    }
}
