package com.keralapsc.prep.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.keralapsc.prep.domain.model.content.ContentUpdatePreferences
import java.util.concurrent.TimeUnit

interface ContentUpdateScheduler {
    fun schedulePeriodicUpdate(preferences: ContentUpdatePreferences)
    fun cancelPeriodicUpdate()
}

class WorkManagerContentUpdateScheduler(
    private val context: Context
) : ContentUpdateScheduler {

    override fun schedulePeriodicUpdate(preferences: ContentUpdatePreferences) {
        val workManager = WorkManager.getInstance(context)

        if (!preferences.autoCheckEnabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }

        val networkType = if (preferences.allowMeteredDownload) {
            NetworkType.CONNECTED
        } else {
            NetworkType.UNMETERED
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<ContentUpdateWorker>(
            repeatInterval = 24L,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2L,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.EXPONENTIAL,
                backoffDelay = 15L,
                timeUnit = TimeUnit.MINUTES
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun cancelPeriodicUpdate() {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    companion object {
        const val PERIODIC_WORK_NAME = "psc_periodic_content_update_work"
    }
}
