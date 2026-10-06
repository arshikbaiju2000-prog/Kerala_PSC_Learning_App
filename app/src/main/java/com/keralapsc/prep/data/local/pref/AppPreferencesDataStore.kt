package com.keralapsc.prep.data.local.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "psc_app_preferences")

/**
 * DataStore dedicated exclusively to application and UI preferences.
 *
 * CRITICAL ARCHITECTURE INVARIANT:
 * Learning domain state (e.g. DailyGoalEntity, UserProgressEntity, UserQuestionProgressEntity)
 * must NEVER be duplicated or stored in this DataStore.
 */
class AppPreferencesDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences> = context.appPreferencesDataStore

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

    companion object {
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

        const val LANGUAGE_SYSTEM = "SYSTEM"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_MALAYALAM = "ml"
    }
}
