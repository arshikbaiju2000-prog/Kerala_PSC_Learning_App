package com.keralapsc.prep.data.work

import android.content.Context
import android.net.ConnectivityManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.keralapsc.prep.PSCApplication

class ContentUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PSCApplication ?: return Result.failure()
        val container = app.container
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val isMeteredProvider = { connectivityManager?.isActiveNetworkMetered ?: false }

        val coordinator = ContentUpdateCoordinator(
            prefRepo = container.contentUpdatePreferencesRepository,
            checkUseCase = container.checkRemoteContentUpdatesUseCase,
            downloadAndInstallUseCase = container.downloadAndInstallContentPackUseCase,
            isMeteredNetworkProvider = isMeteredProvider
        )

        val result = coordinator.execute(runAttemptCount)
        return if (result is Result.Failure) {
            // Coordinator already recorded the error in preferences/diagnostics.
            // Returning Result.success() to WorkManager ensures the periodic work remains ENQUEUED for the next 24h cycle
            // instead of entering WorkManager's terminal FAILED state.
            Result.success()
        } else {
            result
        }
    }

    companion object {
        const val MAX_RETRIES = ContentUpdateCoordinator.MAX_RETRIES
    }
}
