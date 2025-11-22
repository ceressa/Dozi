package com.bardino.dozi.core.data.repository

import android.util.Log
import com.bardino.dozi.core.data.model.DailyReminderLogSummary
import com.bardino.dozi.core.data.model.ReminderEventType
import com.bardino.dozi.core.data.model.ReminderLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hatırlatma loglarını Firebase'de yöneten repository
 */
@Singleton
class ReminderLogRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "ReminderLogRepository"
        private const val COLLECTION_REMINDER_LOGS = "reminderLogs"
        private const val MAX_LOGS_PER_QUERY = 500
        private const val LOG_RETENTION_DAYS = 30 // 30 gün sonra eski logları temizle
    }

    /**
     * Yeni log kaydı ekle
     */
    suspend fun addLog(log: ReminderLog): String? {
        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "⚠️ Kullanıcı giriş yapmamış, log kaydedilemedi")
            return null
        }

        return try {
            val logWithUser = log.copy(userId = userId)
            val docRef = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDER_LOGS)
                .add(logWithUser)
                .await()

            Log.d(TAG, "✅ Log kaydedildi: ${log.eventType} - ${log.medicineName}")
            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Log kaydedilemedi: ${log.eventType}", e)
            null
        }
    }

    /**
     * Belirli bir ilaç için logları getir
     */
    suspend fun getLogsForMedicine(medicineId: String, limit: Int = 100): List<ReminderLog> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDER_LOGS)
                .whereEqualTo("medicineId", medicineId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.toObjects(ReminderLog::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "❌ İlaç logları alınamadı: $medicineId", e)
            emptyList()
        }
    }

    /**
     * Tarih aralığına göre logları getir
     */
    suspend fun getLogsByDateRange(
        startDate: Long,
        endDate: Long,
        limit: Int = MAX_LOGS_PER_QUERY
    ): List<ReminderLog> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDER_LOGS)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.toObjects(ReminderLog::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Tarih aralığı logları alınamadı", e)
            emptyList()
        }
    }

    /**
     * Bugünün loglarını getir
     */
    suspend fun getTodayLogs(): List<ReminderLog> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return getLogsByDateRange(startOfDay, endOfDay)
    }

    /**
     * Son X gündeki logları getir
     */
    suspend fun getRecentLogs(days: Int = 7, limit: Int = MAX_LOGS_PER_QUERY): List<ReminderLog> {
        val endDate = System.currentTimeMillis()
        val startDate = endDate - (days * 24 * 60 * 60 * 1000L)

        return getLogsByDateRange(startDate, endDate, limit)
    }

    /**
     * Event tipine göre logları getir
     */
    suspend fun getLogsByEventType(
        eventType: ReminderEventType,
        limit: Int = 100
    ): List<ReminderLog> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDER_LOGS)
                .whereEqualTo("eventType", eventType.name)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.toObjects(ReminderLog::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Event tipi logları alınamadı: $eventType", e)
            emptyList()
        }
    }

    /**
     * Sadece hata loglarını getir
     */
    suspend fun getErrorLogs(limit: Int = 100): List<ReminderLog> {
        val userId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDER_LOGS)
                .whereEqualTo("success", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.toObjects(ReminderLog::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Hata logları alınamadı", e)
            emptyList()
        }
    }

    /**
     * Günlük log özetini hesapla
     */
    suspend fun getDailySummary(date: Long = System.currentTimeMillis()): DailyReminderLogSummary {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        val logs = getLogsByDateRange(startOfDay, endOfDay)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date(date))

        return DailyReminderLogSummary(
            date = dateString,
            totalEvents = logs.size,
            successfulEvents = logs.count { it.success },
            failedEvents = logs.count { !it.success },
            alarmsScheduled = logs.count { it.eventType == ReminderEventType.ALARM_SCHEDULED.name },
            alarmsTriggered = logs.count { it.eventType == ReminderEventType.ALARM_TRIGGERED.name },
            dosesTaken = logs.count { it.eventType == ReminderEventType.DOSE_TAKEN.name },
            dosesSkipped = logs.count { it.eventType == ReminderEventType.DOSE_SKIPPED.name },
            dosesSnoozed = logs.count { it.eventType == ReminderEventType.DOSE_SNOOZED.name },
            errors = logs.count {
                it.eventType == ReminderEventType.ERROR.name ||
                it.eventType == ReminderEventType.SCHEDULE_ERROR.name ||
                it.eventType == ReminderEventType.NOTIFICATION_ERROR.name ||
                it.eventType == ReminderEventType.FIREBASE_ERROR.name
            }
        )
    }

    /**
     * Eski logları temizle (30 günden eski)
     */
    suspend fun cleanupOldLogs(): Int {
        val userId = auth.currentUser?.uid ?: return 0

        val cutoffDate = System.currentTimeMillis() - (LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000L)

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDER_LOGS)
                .whereLessThan("timestamp", cutoffDate)
                .get()
                .await()

            var deletedCount = 0
            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
                deletedCount++
            }

            Log.d(TAG, "🗑️ $deletedCount eski log silindi")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "❌ Eski loglar silinemedi", e)
            0
        }
    }

    /**
     * Tüm logları sil (debug için)
     */
    suspend fun deleteAllLogs(): Boolean {
        val userId = auth.currentUser?.uid ?: return false

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDER_LOGS)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d(TAG, "🗑️ Tüm loglar silindi")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Loglar silinemedi", e)
            false
        }
    }

    /**
     * Log sayısını getir
     */
    suspend fun getLogCount(): Int {
        val userId = auth.currentUser?.uid ?: return 0

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDER_LOGS)
                .get()
                .await()

            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Log sayısı alınamadı", e)
            0
        }
    }

    /**
     * Son log'u getir
     */
    suspend fun getLastLog(): ReminderLog? {
        val userId = auth.currentUser?.uid ?: return null

        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection(COLLECTION_REMINDER_LOGS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            snapshot.toObjects(ReminderLog::class.java).firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Son log alınamadı", e)
            null
        }
    }
}
