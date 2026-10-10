package com.keralapsc.prep.data.local.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.keralapsc.prep.domain.model.content.ContentUpdatePreferences
import com.keralapsc.prep.domain.model.content.PendingUpdateSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "psc_app_preferences")

/**
 * DataStore dedicated exclusively to application and UI preferences.
 *
 * CRITICAL ARCHITECTURE INVARIANT:
 * Learning domain state (e.g. DailyGoalEntity, UserProgressEntity, UserQuestionProgressEntity)
 * must NEVER be duplicated or stored in this DataStore.
 */
class AppPreferencesDataStore(
    private val context: Context,
    customDataStore: DataStore<Preferences>? = null
) {

    private val dataStore: DataStore<Preferences> = customDataStore ?: context.appPreferencesDataStore
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val appLanguageFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_APP_LANGUAGE] ?: LANGUAGE_SYSTEM
        }

    suspend fun setAppLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = languageCode
        }
    }

    val contentUpdatePreferencesFlow: Flow<ContentUpdatePreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val pendingJson = preferences[KEY_PENDING_UPDATES_JSON] ?: "[]"
            val pendingUpdates = try {
                json.decodeFromString<List<PendingUpdateSummary>>(pendingJson)
            } catch (e: Exception) {
                emptyList()
            }
            ContentUpdatePreferences(
                autoCheckEnabled = preferences[KEY_AUTO_CHECK_ENABLED] ?: true,
                autoDownloadUnmeteredEnabled = preferences[KEY_AUTO_DOWNLOAD_UNMETERED_ENABLED] ?: true,
                allowMeteredDownload = preferences[KEY_ALLOW_METERED_DOWNLOAD] ?: false,
                lastCatalogCheckAttemptTimestamp = preferences[KEY_LAST_CATALOG_CHECK_ATTEMPT_TIMESTAMP] ?: 0L,
                lastSuccessfulCatalogCheckTimestamp = preferences[KEY_LAST_SUCCESSFUL_CATALOG_CHECK_TIMESTAMP] ?: 0L,
                lastSuccessfulInstallationTimestamp = preferences[KEY_LAST_SUCCESSFUL_INSTALLATION_TIMESTAMP] ?: 0L,
                lastCheckStatusMessage = preferences[KEY_LAST_CHECK_STATUS_MESSAGE],
                pendingUpdates = pendingUpdates
            )
        }

    suspend fun setAutoCheckUpdatesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_CHECK_ENABLED] = enabled
        }
    }

    suspend fun setAutoDownloadUnmeteredEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_DOWNLOAD_UNMETERED_ENABLED] = enabled
        }
    }

    suspend fun setAllowMeteredDownload(allowed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ALLOW_METERED_DOWNLOAD] = allowed
        }
    }

    suspend fun recordCatalogCheckAttempt(timestamp: Long, statusMessage: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_CATALOG_CHECK_ATTEMPT_TIMESTAMP] = timestamp
            preferences[KEY_LAST_CHECK_STATUS_MESSAGE] = statusMessage
        }
    }

    suspend fun recordSuccessfulCatalogCheck(
        timestamp: Long,
        pendingUpdates: List<PendingUpdateSummary>,
        statusMessage: String
    ) {
        val serialized = json.encodeToString(pendingUpdates)
        dataStore.edit { preferences ->
            preferences[KEY_LAST_CATALOG_CHECK_ATTEMPT_TIMESTAMP] = timestamp
            preferences[KEY_LAST_SUCCESSFUL_CATALOG_CHECK_TIMESTAMP] = timestamp
            preferences[KEY_LAST_CHECK_STATUS_MESSAGE] = statusMessage
            preferences[KEY_PENDING_UPDATES_JSON] = serialized
        }
    }

    suspend fun recordSuccessfulInstallation(
        timestamp: Long,
        statusMessage: String,
        installedPackId: String? = null,
        installedPackVersion: Int? = null
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_SUCCESSFUL_INSTALLATION_TIMESTAMP] = timestamp
            preferences[KEY_LAST_CHECK_STATUS_MESSAGE] = statusMessage
            if (installedPackId != null && installedPackVersion != null) {
                val currentJson = preferences[KEY_PENDING_UPDATES_JSON] ?: "[]"
                val currentList = try {
                    json.decodeFromString<List<PendingUpdateSummary>>(currentJson)
                } catch (e: Exception) {
                    emptyList()
                }
                val updated = currentList.filterNot { it.packId == installedPackId && it.packVersion == installedPackVersion }
                preferences[KEY_PENDING_UPDATES_JSON] = json.encodeToString(updated)
            }
        }
    }

    suspend fun removePendingUpdate(packId: String, packVersion: Int) {
        dataStore.edit { preferences ->
            val currentJson = preferences[KEY_PENDING_UPDATES_JSON] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<PendingUpdateSummary>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updated = currentList.filterNot { it.packId == packId && it.packVersion == packVersion }
            preferences[KEY_PENDING_UPDATES_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun clearPendingUpdates() {
        dataStore.edit { preferences ->
            preferences[KEY_PENDING_UPDATES_JSON] = "[]"
        }
    }

    companion object {
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_AUTO_CHECK_ENABLED = booleanPreferencesKey("auto_check_updates_enabled")
        val KEY_AUTO_DOWNLOAD_UNMETERED_ENABLED = booleanPreferencesKey("auto_download_unmetered_enabled")
        val KEY_ALLOW_METERED_DOWNLOAD = booleanPreferencesKey("allow_metered_download")
        val KEY_LAST_CATALOG_CHECK_ATTEMPT_TIMESTAMP = longPreferencesKey("last_catalog_check_attempt_timestamp")
        val KEY_LAST_SUCCESSFUL_CATALOG_CHECK_TIMESTAMP = longPreferencesKey("last_successful_catalog_check_timestamp")
        val KEY_LAST_SUCCESSFUL_INSTALLATION_TIMESTAMP = longPreferencesKey("last_successful_installation_timestamp")
        val KEY_LAST_CHECK_STATUS_MESSAGE = stringPreferencesKey("last_check_status_message")
        val KEY_PENDING_UPDATES_JSON = stringPreferencesKey("pending_updates_json")

        const val LANGUAGE_SYSTEM = "SYSTEM"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_MALAYALAM = "ml"
    }
}
