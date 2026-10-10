package com.keralapsc.prep

import android.app.Application
import android.util.Log
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.di.AppContainer
import com.keralapsc.prep.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PSCApplication : Application() {

    lateinit var container: AppContainer
        internal set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Ensure database is initialized, validated, and seeded on IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            container.initializeDatabase()
            val prefs = container.contentUpdatePreferencesRepository.getPreferences().first()
            container.contentUpdateScheduler.schedulePeriodicUpdate(prefs)
        }
    }
}
