package com.bardino.dozi.notifications

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bardino.dozi.core.data.repository.BadiRepository
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 🔄 MedicationSyncWorker
 *
 * WorkManager ile ağır Firestore işlemlerini güvenli şekilde yapar.
 * BroadcastReceiver'dan tetiklenir, uygulama kapalı olsa bile çalışır.
 *
 * Desteklenen action'lar:
 * - ACTION_LOG_TAKEN: İlaç alındı kaydı
 * - ACTION_LOG_SKIPPED: İlaç atlandı kaydı
 * - ACTION_LOG_SNOOZED: İlaç ertelendi kaydı
 * - ACTION_BUDDY_ACCEPT: Badi isteği kabul
 * - ACTION_BUDDY_REJECT: Badi isteği red
 */
class MedicationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "MedicationSyncWorker"

        // Action types
        const val ACTION_LOG_TAKEN = "action_log_taken"
        const val ACTION_LOG_SKIPPED = "action_log_skipped"
        const val ACTION_LOG_SNOOZED = "action_log_snoozed"
        const val ACTION_BUDDY_ACCEPT = "action_buddy_accept"
        const val ACTION_BUDDY_REJECT = "action_buddy_reject"

        // Input data keys
        const val KEY_ACTION = "key_action"
        const val KEY_MEDICINE_ID = "key_medicine_id"
        const val KEY_MEDICINE_NAME = "key_medicine_name"
        const val KEY_DOSAGE = "key_dosage"
        const val KEY_SCHEDULED_TIME = "key_scheduled_time"
        const val KEY_TAKEN_TIME = "key_taken_time"
        const val KEY_SNOOZE_MINUTES = "key_snooze_minutes"
        const val KEY_SKIP_REASON = "key_skip_reason"
        const val KEY_REQUEST_ID = "key_request_id"

        /**
         * İlaç alındı kaydı için Worker'ı başlat
         */
        fun enqueueLogTaken(
            context: Context,
            medicineId: String,
            medicineName: String,
            dosage: String,
            scheduledTime: Long,
            takenTime: Long
        ) {
            val inputData = workDataOf(
                KEY_ACTION to ACTION_LOG_TAKEN,
                KEY_MEDICINE_ID to medicineId,
                KEY_MEDICINE_NAME to medicineName,
                KEY_DOSAGE to dosage,
                KEY_SCHEDULED_TIME to scheduledTime,
                KEY_TAKEN_TIME to takenTime
            )

            enqueueWork(context, "log_taken_$medicineId", inputData)
        }

        /**
         * İlaç atlandı kaydı için Worker'ı başlat
         */
        fun enqueueLogSkipped(
            context: Context,
            medicineId: String,
            medicineName: String,
            dosage: String,
            scheduledTime: Long,
            reason: String = "Kullanıcı atladı"
        ) {
            val inputData = workDataOf(
                KEY_ACTION to ACTION_LOG_SKIPPED,
                KEY_MEDICINE_ID to medicineId,
                KEY_MEDICINE_NAME to medicineName,
                KEY_DOSAGE to dosage,
                KEY_SCHEDULED_TIME to scheduledTime,
                KEY_SKIP_REASON to reason
            )

            enqueueWork(context, "log_skipped_$medicineId", inputData)
        }

        /**
         * İlaç ertelendi kaydı için Worker'ı başlat
         */
        fun enqueueLogSnoozed(
            context: Context,
            medicineId: String,
            medicineName: String,
            dosage: String,
            scheduledTime: Long,
            snoozeMinutes: Int = 10
        ) {
            val inputData = workDataOf(
                KEY_ACTION to ACTION_LOG_SNOOZED,
                KEY_MEDICINE_ID to medicineId,
                KEY_MEDICINE_NAME to medicineName,
                KEY_DOSAGE to dosage,
                KEY_SCHEDULED_TIME to scheduledTime,
                KEY_SNOOZE_MINUTES to snoozeMinutes
            )

            enqueueWork(context, "log_snoozed_$medicineId", inputData)
        }

        /**
         * Badi isteği kabul için Worker'ı başlat
         */
        fun enqueueBuddyAccept(context: Context, requestId: String) {
            val inputData = workDataOf(
                KEY_ACTION to ACTION_BUDDY_ACCEPT,
                KEY_REQUEST_ID to requestId
            )

            enqueueWork(context, "buddy_accept_$requestId", inputData)
        }

        /**
         * Badi isteği red için Worker'ı başlat
         */
        fun enqueueBuddyReject(context: Context, requestId: String) {
            val inputData = workDataOf(
                KEY_ACTION to ACTION_BUDDY_REJECT,
                KEY_REQUEST_ID to requestId
            )

            enqueueWork(context, "buddy_reject_$requestId", inputData)
        }

        private fun enqueueWork(context: Context, uniqueWorkName: String, inputData: Data) {
            // Network constraint YOK - Firestore offline persistence kullanır
            // İnternet yoksa cache'e yazar, gelince otomatik sync olur
            val workRequest = OneTimeWorkRequestBuilder<MedicationSyncWorker>()
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    uniqueWorkName,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

            Log.d(TAG, "✅ Work enqueued: $uniqueWorkName")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val action = inputData.getString(KEY_ACTION) ?: return@withContext Result.failure()

        Log.d(TAG, "🔄 Processing action: $action")

        return@withContext try {
            when (action) {
                ACTION_LOG_TAKEN -> handleLogTaken()
                ACTION_LOG_SKIPPED -> handleLogSkipped()
                ACTION_LOG_SNOOZED -> handleLogSnoozed()
                ACTION_BUDDY_ACCEPT -> handleBuddyAccept()
                ACTION_BUDDY_REJECT -> handleBuddyReject()
                else -> {
                    Log.e(TAG, "❌ Unknown action: $action")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Worker failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun handleLogTaken(): Result {
        val medicineId = inputData.getString(KEY_MEDICINE_ID) ?: return Result.failure()
        val medicineName = inputData.getString(KEY_MEDICINE_NAME) ?: return Result.failure()
        val dosage = inputData.getString(KEY_DOSAGE) ?: ""
        val scheduledTime = inputData.getLong(KEY_SCHEDULED_TIME, 0L)
        val takenTime = inputData.getLong(KEY_TAKEN_TIME, System.currentTimeMillis())

        val medicationLogRepository = MedicationLogRepository(
            applicationContext,
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )

        medicationLogRepository.logMedicationTaken(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime
        )

        // Gecikme pattern'ini kaydet
        SmartReminderHelper.recordDelayPattern(
            context = applicationContext,
            medicineId = medicineId,
            scheduledTime = scheduledTime,
            takenTime = takenTime
        )

        Log.d(TAG, "✅ Medication TAKEN logged: $medicineName")
        return Result.success()
    }

    private suspend fun handleLogSkipped(): Result {
        val medicineId = inputData.getString(KEY_MEDICINE_ID) ?: return Result.failure()
        val medicineName = inputData.getString(KEY_MEDICINE_NAME) ?: return Result.failure()
        val dosage = inputData.getString(KEY_DOSAGE) ?: ""
        val scheduledTime = inputData.getLong(KEY_SCHEDULED_TIME, 0L)
        val reason = inputData.getString(KEY_SKIP_REASON) ?: "Kullanıcı atladı"

        val medicationLogRepository = MedicationLogRepository(
            applicationContext,
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )

        medicationLogRepository.logMedicationSkipped(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            reason = reason
        )

        Log.d(TAG, "✅ Medication SKIPPED logged: $medicineName")
        return Result.success()
    }

    private suspend fun handleLogSnoozed(): Result {
        val medicineId = inputData.getString(KEY_MEDICINE_ID) ?: return Result.failure()
        val medicineName = inputData.getString(KEY_MEDICINE_NAME) ?: return Result.failure()
        val dosage = inputData.getString(KEY_DOSAGE) ?: ""
        val scheduledTime = inputData.getLong(KEY_SCHEDULED_TIME, 0L)
        val snoozeMinutes = inputData.getInt(KEY_SNOOZE_MINUTES, 10)

        val medicationLogRepository = MedicationLogRepository(
            applicationContext,
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )

        medicationLogRepository.logMedicationSnoozed(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            snoozeMinutes = snoozeMinutes
        )

        Log.d(TAG, "✅ Medication SNOOZED logged: $medicineName")
        return Result.success()
    }

    private suspend fun handleBuddyAccept(): Result {
        val requestId = inputData.getString(KEY_REQUEST_ID) ?: return Result.failure()

        val buddyRepository = BadiRepository()
        val result = buddyRepository.acceptBadiRequest(requestId)

        return if (result.isSuccess) {
            Log.d(TAG, "✅ Buddy request accepted: $requestId")
            Result.success()
        } else {
            Log.e(TAG, "❌ Buddy accept failed: ${result.exceptionOrNull()?.message}")
            Result.retry()
        }
    }

    private suspend fun handleBuddyReject(): Result {
        val requestId = inputData.getString(KEY_REQUEST_ID) ?: return Result.failure()

        val buddyRepository = BadiRepository()
        val result = buddyRepository.rejectBadiRequest(requestId)

        return if (result.isSuccess) {
            Log.d(TAG, "✅ Buddy request rejected: $requestId")
            Result.success()
        } else {
            Log.e(TAG, "❌ Buddy reject failed: ${result.exceptionOrNull()?.message}")
            Result.retry()
        }
    }
}
