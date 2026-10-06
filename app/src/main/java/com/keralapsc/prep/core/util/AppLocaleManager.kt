package com.keralapsc.prep.core.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.keralapsc.prep.data.local.pref.AppPreferencesDataStore
import java.util.Locale

object AppLocaleManager {

    fun applyLocale(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (languageCode == AppPreferencesDataStore.LANGUAGE_SYSTEM) {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList.forLanguageTags(languageCode)
            }
        } else {
            val targetLocale = when (languageCode) {
                AppPreferencesDataStore.LANGUAGE_MALAYALAM -> Locale("ml")
                AppPreferencesDataStore.LANGUAGE_ENGLISH -> Locale("en")
                else -> Locale.getDefault()
            }
            Locale.setDefault(targetLocale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(targetLocale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}
