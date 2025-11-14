package com.bardino.dozi.core.data.repository

import com.bardino.dozi.core.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * İlaç alma geçmişini yöneten repository
 */
class MedicationLogRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * İlaç log'u oluştur
     */
    suspend fun createMedicationLog(log: MedicationLog): Result<String> {
        val userId = currentUserId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val logWithUser = log.copy(userId = userId)
            val docRef = db.collection("medication_logs").add(logWithUser).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            .where("userId", "==", userId)
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
                .where("userId", "==", userId)
                .where("scheduledTime", ">=", startDate)
                .where("scheduledTime", "<=", endDate)
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
                .where("userId", "==", userId)
                .where("medicineId", "==", medicineId)
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
                .where("userId", "==", buddyUserId)
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
