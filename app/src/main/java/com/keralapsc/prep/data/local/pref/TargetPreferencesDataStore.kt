package com.keralapsc.prep.data.local.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.keralapsc.prep.domain.model.target.ExamTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.targetDataStore: DataStore<Preferences> by preferencesDataStore(name = "psc_target_preferences")

class TargetPreferencesDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences> = context.targetDataStore

    val activeTargetFlow: Flow<ExamTarget> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val levelId = preferences[KEY_LEVEL_ID]?.takeIf { it.isNotBlank() }
            val postId = preferences[KEY_POST_ID]?.takeIf { it.isNotBlank() }
            ExamTarget(levelId = levelId, postId = postId)
        }

    suspend fun saveActiveTarget(target: ExamTarget) {
        dataStore.edit { preferences ->
            if (target.levelId != null) {
                preferences[KEY_LEVEL_ID] = target.levelId
            } else {
                preferences.remove(KEY_LEVEL_ID)
            }
            if (target.postId != null) {
                preferences[KEY_POST_ID] = target.postId
            } else {
                preferences.remove(KEY_POST_ID)
            }
        }
    }

    companion object {
        val KEY_LEVEL_ID = stringPreferencesKey("active_exam_level_id")
        val KEY_POST_ID = stringPreferencesKey("active_exam_post_id")
    }
}
