package com.keralapsc.prep

import android.app.Application
import android.util.Log
import com.keralapsc.prep.data.local.asset.DatabasePrepopulationCallback
import com.keralapsc.prep.di.AppContainer
import com.keralapsc.prep.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PSCApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Ensure database is initialized, validated, and seeded on IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            container.initializeDatabase()
        }
    }
}
