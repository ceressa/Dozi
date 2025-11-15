package com.bardino.dozi.core.data.repository

import android.content.Context
import android.util.Log
import com.bardino.dozi.core.data.local.DoziDatabase
import com.bardino.dozi.core.data.local.entity.MedicationLogEntity
import com.bardino.dozi.core.data.local.entity.SyncQueueEntity
import com.bardino.dozi.core.data.local.entity.SyncActionType
import com.bardino.dozi.core.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

/**
 * İlaç alma geçmişini yöneten repository (Offline-first with Room DB)
 */
class MedicationLogRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val localDb = DoziDatabase.getDatabase(context)
    private val medicationLogDao = localDb.medicationLogDao()
    private val syncQueueDao = localDb.syncQueueDao()
    private val gson = Gson()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    companion object {
        private const val TAG = "MedicationLogRepository"
    }

    /**
     * İlaç log'u oluştur (Offline-first)
     * 1. Room DB'ye kaydet (hemen)
     * 2. Sync queue'ya ekle
     * 3. Firestore'a sync et (online ise)
     */
    suspend fun createMedicationLog(log: MedicationLog): Result<String> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val logId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            // 1. ✅ Save to Room DB immediately (offline support)
            val entity = MedicationLogEntity(
                id = logId,
                userId = userId,
                medicineId = log.medicineId,
                medicineName = log.medicineName,
                dosage = log.dosage,
                scheduledTime = log.scheduledTime?.toDate()?.time ?: now,
                takenAt = log.takenAt?.toDate()?.time,
                status = log.status.name,
                notes = log.notes,
                sideEffects = gson.toJson(log.sideEffects),
                mood = log.mood,
                locationLat = log.location?.latitude,
                locationLng = log.location?.longitude,
                createdAt = now,
                updatedAt = now,
                isSynced = false
            )
            medicationLogDao.insert(entity)

            // 2. ✅ Queue for Firestore sync
            val syncData = mapOf(
                "logId" to logId,
                "log" to gson.toJson(log.copy(id = logId, userId = userId))
            )
            queueForSync(SyncActionType.MEDICATION_TAKEN, syncData)

            // 3. ✅ Try to sync immediately (if online)
            syncPendingLogs()

            Log.d(TAG, "✅ Medication log created: $logId")
            Result.success(logId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating medication log", e)
            Result.failure(e)
        }
    }

    /**
     * Quick helpers for common actions (used by HomeScreen)
     */
    suspend fun logMedicationTaken(
        medicineId: String,
        medicineName: String,
        dosage: String,
        scheduledTime: Long,
        notes: String? = null
    ): Result<String> {
        val log = MedicationLog(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = Timestamp(Date(scheduledTime)),
            takenAt = Timestamp(Date()),
            status = MedicationStatus.TAKEN,
            notes = notes
        )
        return createMedicationLog(log)
    }

    suspend fun logMedicationSkipped(
        medicineId: String,
        medicineName: String,
        dosage: String,
        scheduledTime: Long,
        reason: String? = null
    ): Result<String> {
        val log = MedicationLog(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = Timestamp(Date(scheduledTime)),
            status = MedicationStatus.SKIPPED,
            notes = reason
        )
        return createMedicationLog(log)
    }

    suspend fun logMedicationSnoozed(
        medicineId: String,
        medicineName: String,
        dosage: String,
        scheduledTime: Long,
        snoozeMinutes: Int
    ): Result<String> {
        val log = MedicationLog(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = Timestamp(Date(scheduledTime)),
            status = MedicationStatus.SNOOZED,
            notes = "Snoozed for $snoozeMinutes minutes"
        )
        return createMedicationLog(log)
    }

    /**
     * Queue action for Firestore sync
     */
    private suspend fun queueForSync(actionType: String, data: Map<String, Any?>) {
        try {
            val userId = currentUserId ?: return
            val action = SyncQueueEntity(
                actionType = actionType,
                dataJson = gson.toJson(data),
                userId = userId,
                retryCount = 0,
                createdAt = System.currentTimeMillis()
            )
            syncQueueDao.insert(action)
            Log.d(TAG, "📥 Queued for sync: $actionType")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error queuing for sync", e)
        }
    }

    /**
     * Sync pending logs to Firestore
     */
    suspend fun syncPendingLogs(): Int {
        val userId = currentUserId ?: return 0

        return try {
            val pendingActions = syncQueueDao.getPendingWithRetries(userId)
            var syncedCount = 0

            pendingActions.forEach { action ->
                try {
                    val data = gson.fromJson(action.dataJson, Map::class.java) as Map<String, Any?>
                    val logJson = data["log"] as? String
                    val logId = data["logId"] as? String

                    if (logJson != null && logId != null) {
                        val log = gson.fromJson(logJson, MedicationLog::class.java)

                        // Sync to Firestore
                        db.collection("medication_logs")
                            .document(logId)
                            .set(log)
                            .await()

                        // Mark as synced in Room
                        medicationLogDao.markAsSynced(logId)

                        // Remove from sync queue
                        syncQueueDao.deleteById(action.id)
                        syncedCount++

                        Log.d(TAG, "✅ Synced: ${action.actionType}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Sync failed for action ${action.id}", e)
                    syncQueueDao.incrementRetryCount(
                        action.id,
                        System.currentTimeMillis(),
                        e.message
                    )
                }
            }

            Log.d(TAG, "🔄 Synced $syncedCount / ${pendingActions.size} logs to Firestore")
            syncedCount
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing pending logs", e)
            0
        }
    }

    /**
     * Get unsynced logs count
     */
    suspend fun getUnsyncedCount(): Int {
        val userId = currentUserId ?: return 0
        return syncQueueDao.getPendingCount(userId)
    }

    /**
     * İlaç log'unu güncelle
     */
    suspend fun updateMedicationLog(logId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("medication_logs")
                .document(logId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * İlaç durumunu güncelle (Alındı, Atlandı, vb.)
     */
    suspend fun updateMedicationStatus(
        logId: String,
        status: MedicationStatus,
        takenAt: Timestamp? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            if (takenAt != null) {
                updates["takenAt"] = takenAt
            }

            db.collection("medication_logs")
                .document(logId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kullanıcının tüm ilaç geçmişini real-time dinle
     */
    fun getMedicationLogsFlow(): Flow<List<MedicationLog>> = callbackFlow {
        val userId = currentUserId ?: run {
            close()
            return@callbackFlow
        }

        val listener = db.collection("medication_logs")
            .whereEqualTo("userId", userId)
            .orderBy("scheduledTime", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val logs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MedicationLog::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(logs)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Belirli bir tarih aralığındaki ilaç geçmişini getir
     */
    suspend fun getMedicationLogsByDateRange(
        startDate: Timestamp,
        endDate: Timestamp
    ): List<MedicationLog> {
        val userId = currentUserId ?: return emptyList()

        return try {
            val snapshot = db.collection("medication_logs")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("scheduledTime", startDate)
                .whereLessThanOrEqualTo("scheduledTime", endDate)
                .orderBy("scheduledTime", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MedicationLog::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Belirli bir LocalDate için ilaç loglarını getir
     */
    suspend fun getMedicationLogsForDate(date: LocalDate): List<MedicationLog> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault())
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault())

        val startTimestamp = Timestamp(Date.from(startOfDay.toInstant()))
        val endTimestamp = Timestamp(Date.from(endOfDay.toInstant()))

        return getMedicationLogsByDateRange(startTimestamp, endTimestamp)
    }

    /**
     * Bugünkü ilaç geçmişini getir
     */
    suspend fun getTodayMedicationLogs(): DailyMedicationLogs {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val logs = getMedicationLogsByDateRange(
            Timestamp(today.time),
            Timestamp(tomorrow.time)
        )

        return DailyMedicationLogs(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time),
            logs = logs,
            takenCount = logs.count { it.status == MedicationStatus.TAKEN },
            missedCount = logs.count { it.status == MedicationStatus.MISSED },
            skippedCount = logs.count { it.status == MedicationStatus.SKIPPED },
            totalCount = logs.size
        )
    }

    /**
     * Son 7 günlük ilaç geçmişini getir
     */
    suspend fun getWeeklyMedicationLogs(): List<DailyMedicationLogs> {
        val result = mutableListOf<DailyMedicationLogs>()

        for (i in 0..6) {
            val day = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val nextDay = Calendar.getInstance().apply {
                timeInMillis = day.timeInMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }

            val logs = getMedicationLogsByDateRange(
                Timestamp(day.time),
                Timestamp(nextDay.time)
            )

            result.add(
                DailyMedicationLogs(
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(day.time),
                    logs = logs,
                    takenCount = logs.count { it.status == MedicationStatus.TAKEN },
                    missedCount = logs.count { it.status == MedicationStatus.MISSED },
                    skippedCount = logs.count { it.status == MedicationStatus.SKIPPED },
                    totalCount = logs.size
                )
            )
        }

        return result
    }

    /**
     * Belirli bir ilaç için geçmişi getir
     */
    suspend fun getMedicationLogsByMedicineId(medicineId: String): List<MedicationLog> {
        val userId = currentUserId ?: return emptyList()

        return try {
            val snapshot = db.collection("medication_logs")
                .whereEqualTo("userId", userId)
                .whereEqualTo("medicineId", medicineId)
                .orderBy("scheduledTime", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MedicationLog::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Buddy'nin ilaç geçmişini getir (buddy permission kontrolü yapılmalı)
     */
    suspend fun getBuddyMedicationLogs(buddyUserId: String): List<MedicationLog> {
        return try {
            val snapshot = db.collection("medication_logs")
                .whereEqualTo("userId", buddyUserId)
                .orderBy("scheduledTime", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MedicationLog::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * İlaç alma istatistikleri
     */
    suspend fun getMedicationStats(days: Int = 30): MedicationStats {
        val userId = currentUserId ?: return MedicationStats()

        val startDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }

        val logs = getMedicationLogsByDateRange(
            Timestamp(startDate.time),
            Timestamp(Date())
        )

        val takenCount = logs.count { it.status == MedicationStatus.TAKEN }
        val missedCount = logs.count { it.status == MedicationStatus.MISSED }
        val skippedCount = logs.count { it.status == MedicationStatus.SKIPPED }
        val totalCount = logs.size

        return MedicationStats(
            totalCount = totalCount,
            takenCount = takenCount,
            missedCount = missedCount,
            skippedCount = skippedCount,
            adherenceRate = if (totalCount > 0) takenCount.toFloat() / totalCount else 0f
        )
    }
}

/**
 * İlaç alma istatistikleri
 */
data class MedicationStats(
    val totalCount: Int = 0,
    val takenCount: Int = 0,
    val missedCount: Int = 0,
    val skippedCount: Int = 0,
    val adherenceRate: Float = 0f // 0.0 - 1.0 arası (uyum oranı)
)
