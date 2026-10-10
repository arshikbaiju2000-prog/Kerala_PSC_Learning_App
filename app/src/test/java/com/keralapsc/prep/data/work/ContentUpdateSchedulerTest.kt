package com.keralapsc.prep.data.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import com.keralapsc.prep.domain.model.content.ContentUpdatePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ContentUpdateSchedulerTest {

    @Test
    fun periodicWorkRequest_has24HourIntervalAnd2HourFlex() {
        val request = PeriodicWorkRequestBuilder<ContentUpdateWorker>(
            repeatInterval = 24L,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2L,
            flexTimeIntervalUnit = TimeUnit.HOURS
        ).build()

        val expectedIntervalMillis = TimeUnit.HOURS.toMillis(24)
        val expectedFlexMillis = TimeUnit.HOURS.toMillis(2)

        assertEquals(expectedIntervalMillis, request.workSpec.intervalDuration)
        assertEquals(expectedFlexMillis, request.workSpec.flexDuration)
    }

    @Test
    fun periodicWorkRequest_hasExponentialBackoffWith15MinutesDelay() {
        val request = PeriodicWorkRequestBuilder<ContentUpdateWorker>(
            repeatInterval = 24L,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2L,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setBackoffCriteria(
                backoffPolicy = BackoffPolicy.EXPONENTIAL,
                backoffDelay = 15L,
                timeUnit = TimeUnit.MINUTES
            )
            .build()

        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
        assertEquals(TimeUnit.MINUTES.toMillis(15), request.workSpec.backoffDelayDuration)
    }

    @Test
    fun constraints_forUnmeteredOnly_requireUnmeteredNetworkAndBatteryNotLow() {
        val preferences = ContentUpdatePreferences(
            autoCheckEnabled = true,
            autoDownloadUnmeteredEnabled = true,
            allowMeteredDownload = false
        )

        val networkType = if (preferences.allowMeteredDownload) {
            NetworkType.CONNECTED
        } else {
            NetworkType.UNMETERED
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()

        assertEquals(NetworkType.UNMETERED, constraints.requiredNetworkType)
        assertTrue(constraints.requiresBatteryNotLow())
    }

    @Test
    fun constraints_forMeteredAllowed_requireConnectedNetworkAndBatteryNotLow() {
        val preferences = ContentUpdatePreferences(
            autoCheckEnabled = true,
            autoDownloadUnmeteredEnabled = true,
            allowMeteredDownload = true
        )

        val networkType = if (preferences.allowMeteredDownload) {
            NetworkType.CONNECTED
        } else {
            NetworkType.UNMETERED
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()

        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        assertTrue(constraints.requiresBatteryNotLow())
    }

    @Test
    fun uniqueWorkName_isConsistent() {
        assertEquals("psc_periodic_content_update_work", WorkManagerContentUpdateScheduler.PERIODIC_WORK_NAME)
    }
}
