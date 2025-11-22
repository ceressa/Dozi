package com.bardino.dozi.core.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.bardino.dozi.core.data.local.DoziDatabase
import com.bardino.dozi.core.data.local.entity.MedicationLogEntity
import com.bardino.dozi.core.data.local.entity.SyncActionType
import com.bardino.dozi.core.data.local.entity.SyncQueueEntity
import com.bardino.dozi.core.data.model.DailyMedicationLogs
import com.bardino.dozi.core.data.model.MedicationLog
import com.bardino.dozi.core.data.model.MedicationStatus
import com.bardino.dozi.core.data.model.MedicationStats
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

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

            val entity = MedicationLogEntity(
                id = logId,
                userId = userId,
                medicineId = log.medicineId,
                medicineName = log.medicineName,
                dosage = log.dosage,
                scheduledTime = log.scheduledTime?.toDate()?.time ?: now,
                takenAt = log.takenAt?.toDate()?.time,
                status = log.status, // String
                notes = log.notes,
                sideEffects = gson.toJson(log.sideEffects),
                mood = log.mood,
                locationLat = log.locationLat ?: log.location?.latitude,
                locationLng = log.locationLng ?: log.location?.longitude,
                createdAt = now,
                updatedAt = now,
                isSynced = false
            )
            medicationLogDao.insert(entity)

            val syncData = mapOf(
                "logId" to logId,
                "log" to gson.toJson(log.copy(id = logId, userId = userId))
            )
            queueForSync(SyncActionType.MEDICATION_TAKEN, syncData)

            syncPendingLogs()

            Log.d(TAG, "✅ Medication log created: $logId")
            Result.success(logId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating medication log", e)
            Result.failure(e)
        }
    }

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
            status = MedicationStatus.TAKEN.name,
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
            status = MedicationStatus.SKIPPED.name,
            notes = reason
        )
        return createMedicationLog(log)
    }

    suspend fun logMedicationMissed(
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
            status = MedicationStatus.MISSED.name,
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
            status = MedicationStatus.SNOOZED.name,
            notes = "Snoozed for $snoozeMinutes minutes"
        )
        return createMedicationLog(log)
    }

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

                        db.collection("medication_logs")
                            .document(logId)
                            .set(log)
                            .await()

                        medicationLogDao.markAsSynced(logId)
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

    suspend fun getUnsyncedCount(): Int {
        val userId = currentUserId ?: return 0
        return syncQueueDao.getPendingCount(userId)
    }

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

    suspend fun updateMedicationStatus(
        logId: String,
        status: MedicationStatus,
        takenAt: Timestamp? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis()
            )

            if (takenAt != null) {
                updates["takenAt"] = takenAt
            }

            db.collection("medication_logs")
                .document(logId)
                .update(updates as Map<String, Any>)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getMedicationLogsForDate(date: LocalDate): List<MedicationLog> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault())
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault())

        val startTimestamp = Timestamp(Date.from(startOfDay.toInstant()))
        val endTimestamp = Timestamp(Date.from(endOfDay.toInstant()))

        return getMedicationLogsByDateRange(startTimestamp, endTimestamp)
    }

    suspend fun isMedicationLoggedForTime(
        medicineId: String,
        scheduledTime: Long
    ): Boolean {
        val userId = currentUserId ?: return false

        return try {
            val localLog = medicationLogDao.getLogForMedicineAndTime(
                medicineId = medicineId,
                scheduledTime = scheduledTime,
                userId = userId
            )

            if (localLog != null && localLog.status in listOf(
                    MedicationStatus.TAKEN.name,
                    MedicationStatus.SKIPPED.name,
                    MedicationStatus.SNOOZED.name
                )
            ) {
                Log.d(TAG, "✅ Local DB'de log bulundu: $medicineId - ${localLog.status}")
                return true
            }

            val tolerance = 5 * 60 * 1000
            val startTime = Timestamp(Date(scheduledTime - tolerance))
            val endTime = Timestamp(Date(scheduledTime + tolerance))

            val snapshot = db.collection("medication_logs")
                .whereEqualTo("userId", userId)
                .whereEqualTo("medicineId", medicineId)
                .whereGreaterThanOrEqualTo("scheduledTime", startTime)
                .whereLessThanOrEqualTo("scheduledTime", endTime)
                .get()
                .await()

            val hasLog = snapshot.documents.any { doc ->
                val status = doc.getString("status")
                status in listOf(
                    MedicationStatus.TAKEN.name,
                    MedicationStatus.SKIPPED.name,
                    MedicationStatus.SNOOZED.name
                )
            }

            if (hasLog) {
                Log.d(TAG, "✅ Firestore'da log bulundu: $medicineId")
            }

            hasLog
        } catch (e: Exception) {
            Log.e(TAG, "❌ isMedicationLoggedForTime hatası", e)
            false
        }
    }

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
            takenCount = logs.count { it.status == MedicationStatus.TAKEN.name },
            missedCount = logs.count { it.status == MedicationStatus.MISSED.name },
            skippedCount = logs.count { it.status == MedicationStatus.SKIPPED.name },
            totalCount = logs.size
        )
    }

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
                    takenCount = logs.count { it.status == MedicationStatus.TAKEN.name },
                    missedCount = logs.count { it.status == MedicationStatus.MISSED.name },
                    skippedCount = logs.count { it.status == MedicationStatus.SKIPPED.name },
                    totalCount = logs.size
                )
            )
        }

        return result
    }

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

    suspend fun getLogsForMedicine(medicineId: String, startTime: Long): List<MedicationLog> {
        val userId = currentUserId ?: return emptyList()

        return try {
            val startTimestamp = Timestamp(Date(startTime))

            val snapshot = db.collection("medication_logs")
                .whereEqualTo("userId", userId)
                .whereEqualTo("medicineId", medicineId)
                .whereGreaterThanOrEqualTo("scheduledTime", startTimestamp)
                .orderBy("scheduledTime", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MedicationLog::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting logs for medicine $medicineId from $startTime", e)
            emptyList()
        }
    }

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

    suspend fun getMedicationStats(days: Int = 30): MedicationStats {
        val userId = currentUserId ?: return MedicationStats()

        val startDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }

        val logs = getMedicationLogsByDateRange(
            Timestamp(startDate.time),
            Timestamp(Date())
        )

        val takenCount = logs.count { it.status == MedicationStatus.TAKEN.name }
        val missedCount = logs.count { it.status == MedicationStatus.MISSED.name }
        val skippedCount = logs.count { it.status == MedicationStatus.SKIPPED.name }
        val totalCount = logs.size

        return MedicationStats(
            totalCount = totalCount,
            takenCount = takenCount,
            missedCount = missedCount,
            skippedCount = skippedCount,
            adherenceRate = if (totalCount > 0) takenCount.toFloat() / totalCount else 0f
        )
    }

    /**
     * Belirli kullanıcı + tarih aralığı için log listesi (insights & raporlar için)
     */
    suspend fun getLogsBetweenDates(
        userId: String,
        startMillis: Long,
        endMillis: Long
    ): List<MedicationLog> {
        return try {
            val snapshot = db.collection("medication_logs")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("scheduledTime", Timestamp(Date(startMillis)))
                .whereLessThanOrEqualTo("scheduledTime", Timestamp(Date(endMillis)))
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
     * Son 7 günde kaç MISSED log var
     */
    suspend fun getMissedCountLast7Days(userId: String): Int {
        val end = System.currentTimeMillis()
        val start = end - 7L * 24 * 60 * 60 * 1000

        val logs = getLogsBetweenDates(userId, start, end)
        return logs.count { it.status == MedicationStatus.MISSED.name }
    }

    /**
     * İlk log'dan bu yana kaç gün geçti
     */
    suspend fun getDaysSinceFirstLog(userId: String): Int {
        return try {
            val snapshot = db.collection("medication_logs")
                .whereEqualTo("userId", userId)
                .orderBy("scheduledTime", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull() ?: return 0

            val ts = doc.getTimestamp("scheduledTime")
                ?: doc.getTimestamp("createdAt")
                ?: return 0

            val first = ts.toDate().time
            val diff = System.currentTimeMillis() - first
            (diff / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }
}
