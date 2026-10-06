package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class GoogleSheetSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "GoogleSheetSyncWorker started: executing two-phase sync (1. get from sheet -> 2. update by app)...")
        // Automatically: First gets latest data from sheet, then updates local attendance to sheet
        val result = GoogleSheetSyncManager.autoSyncTwoWay(applicationContext)
        return if (result.isSuccess) {
            val summary = result.getOrNull()
            Log.d(TAG, "GoogleSheetSyncWorker succeeded: ${summary?.message}")
            Result.success()
        } else {
            val exception = result.exceptionOrNull()
            Log.w(TAG, "GoogleSheetSyncWorker failed: ${exception?.message}")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "GoogleSheetSyncWorker"
        const val ONE_TIME_SYNC_WORK = "google_sheet_one_time_sync"
        const val PERIODIC_SYNC_WORK = "google_sheet_periodic_sync"

        /**
         * Enqueues an immediate one-time sync task that triggers as soon as internet
         * connection is available.
         */
        fun enqueueAutoSyncWhenOnline(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<GoogleSheetSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_SYNC_WORK,
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
            Log.d(TAG, "Enqueued one-time sync with NetworkType.CONNECTED constraint")
        }

        /**
         * Schedules recurring periodic background synchronization every 1 hour
         * whenever an internet connection is active.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicSyncRequest = PeriodicWorkRequestBuilder<GoogleSheetSyncWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicSyncRequest
            )
            Log.d(TAG, "Scheduled periodic sync with WorkManager")
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_WORK)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_SYNC_WORK)
        }
    }
}
