package com.bardino.dozi.core.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for background sync operations
 * Handles periodic sync and network-dependent sync
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val PERIODIC_SYNC_WORK_NAME = "dozi_periodic_sync"
        private const val ONE_TIME_SYNC_WORK_NAME = "dozi_one_time_sync"

        /**
         * Schedule periodic background sync
         * Runs every 15 minutes when network is available
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 5,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag("sync")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    PERIODIC_SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )

            Log.d(TAG, "✅ Periodic sync scheduled (every 15 minutes)")
        }

        /**
         * Cancel periodic sync
         */
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
            Log.d(TAG, "🗑️ Periodic sync cancelled")
        }

        /**
         * Request immediate sync when network becomes available
         */
        fun requestImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag("immediate_sync")
                .build()

            WorkManager.getInstance(context)
                .enqueue(syncRequest)

            Log.d(TAG, "✅ Immediate sync requested (will run when network available)")
        }

        /**
         * Force sync now (no network constraint - may fail if offline)
         */
        fun forceSyncNow(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .addTag("force_sync")
                .build()

            WorkManager.getInstance(context)
                .enqueue(syncRequest)

            Log.d(TAG, "✅ Force sync requested")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔄 SyncWorker started")

        return try {
            val syncManager = SyncManager(applicationContext)
            val syncedCount = syncManager.processPendingSync()

            Log.d(TAG, "✅ SyncWorker completed: $syncedCount items synced")

            if (syncedCount > 0 || syncManager.getPendingSyncCount() == 0) {
                Result.success()
            } else {
                // Still have pending items - retry later
                Log.w(TAG, "⚠️ Some items still pending, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SyncWorker failed", e)
            Result.retry()
        }
    }
}
