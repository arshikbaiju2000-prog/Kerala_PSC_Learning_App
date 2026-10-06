package com.keralapsc.prep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.keralapsc.prep.core.util.AppLocaleManager
import com.keralapsc.prep.presentation.error.DatabaseErrorScreen
import com.keralapsc.prep.presentation.navigation.PSCApp
import com.keralapsc.prep.presentation.theme.PSCTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PSCApplication).container
        lifecycleScope.launch {
            val languageCode = container.appPreferencesDataStore.appLanguageFlow.first()
            AppLocaleManager.applyLocale(this@MainActivity, languageCode)
        }
        enableEdgeToEdge()
        setContent {
            PSCTheme {
                val dbError by container.databaseInitializationError.collectAsState()
                if (dbError != null) {
                    DatabaseErrorScreen(
                        error = dbError,
                        onRetry = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                container.initializeDatabase()
                            }
                        }
                    )
                } else {
                    PSCApp()
                }
            }
        }
    }
}
