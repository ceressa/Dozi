package com.bardino.dozi.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.bardino.dozi.utils.reporting.WeeklyReportGenerator
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Haftalık rapor bildirimi gönderen worker
 */
@HiltWorker
class WeeklyReportNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val reportGenerator: WeeklyReportGenerator,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.failure()

        return try {
            val report = reportGenerator.generateReport(userId)

            // Bildirim göster
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationHelper.showWeeklyReportNotification(
                    context = context,
                    compliance = report.overallCompliance,
                    takenCount = report.takenDoses,
                    missedCount = report.missedDoses
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "weekly_report_notification"

        /**
         * Haftalık rapor worker'ını planla
         * Her Pazartesi saat 09:00'da çalışır
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val initialDelay = calculateDelayToNextMonday()

            val request = PeriodicWorkRequestBuilder<WeeklyReportNotificationWorker>(
                7, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        /**
         * Worker'ı iptal et
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Bir sonraki Pazartesi saat 09:00'a kadar olan süreyi hesapla
         */
        private fun calculateDelayToNextMonday(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Eğer bu hafta Pazartesi geçtiyse, gelecek hafta
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            return target.timeInMillis - now.timeInMillis
        }
    }
}
