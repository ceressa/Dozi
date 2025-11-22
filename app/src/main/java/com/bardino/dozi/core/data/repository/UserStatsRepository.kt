package com.bardino.dozi.core.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.bardino.dozi.core.data.model.MedicationStatus
import com.bardino.dozi.core.data.model.UserStats
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserStats Repository - Kullanıcı istatistikleri ve gamification
 */
@Singleton
class UserStatsRepository @Inject constructor(
    private val achievementRepository: AchievementRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "UserStatsRepository"

    suspend fun getUserStats(): UserStats? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val doc = firestore.collection("user_stats")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                doc.toObject(UserStats::class.java)
            } else {
                val initialStats = UserStats(
                    id = userId,
                    userId = userId,
                    currentStreak = 0,
                    longestStreak = 0,
                    lastStreakDate = null,
                    totalMedicationsTaken = 0,
                    totalMedicationsMissed = 0,
                    complianceRate = 0f
                )

                firestore.collection("user_stats")
                    .document(userId)
                    .set(initialStats)
                    .await()

                initialStats
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user stats", e)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateStreak(medicationLogRepository: MedicationLogRepository) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val stats = getUserStats() ?: return

            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val lastStreakDate = stats.lastStreakDate?.toDate()
                ?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

            val todayLogs = medicationLogRepository.getMedicationLogsForDate(today)

            val allTakenToday = todayLogs.isNotEmpty() &&
                    todayLogs.all { it.status == MedicationStatus.TAKEN.name }

            if (!allTakenToday) {
                Log.d(TAG, "Not all medications taken today, streak unchanged")
                return
            }

            val newStreak = when {
                lastStreakDate == null -> 1
                lastStreakDate == yesterday -> stats.currentStreak + 1
                lastStreakDate == today -> stats.currentStreak
                else -> 1
            }

            val newLongestStreak = maxOf(newStreak, stats.longestStreak)

            firestore.collection("user_stats")
                .document(userId)
                .update(
                    mapOf(
                        "currentStreak" to newStreak,
                        "longestStreak" to newLongestStreak,
                        "lastStreakDate" to Timestamp(
                            Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
                        ),
                        "totalMedicationsTaken" to stats.totalMedicationsTaken + 1
                    )
                )
                .await()

            achievementRepository.checkStreakAchievements(newStreak)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating streak", e)
        }
    }

    suspend fun breakStreak() {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection("user_stats")
                .document(userId)
                .update(
                    mapOf(
                        "currentStreak" to 0,
                        "totalMedicationsMissed" to FieldValue.increment(1)
                    )
                )
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "Error breaking streak", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateComplianceRate(medicationLogRepository: MedicationLogRepository) {
        try {
            val userId = auth.currentUser?.uid ?: return

            val last30Days = (0..29).map { LocalDate.now().minusDays(it.toLong()) }
            var totalScheduled = 0
            var totalTaken = 0

            for (date in last30Days) {
                val logs = medicationLogRepository.getMedicationLogsForDate(date)
                totalScheduled += logs.size
                totalTaken += logs.count { it.status == MedicationStatus.TAKEN.name }
            }

            val complianceRate = if (totalScheduled > 0) {
                totalTaken.toFloat() / totalScheduled * 100f
            } else {
                0f
            }

            firestore.collection("user_stats")
                .document(userId)
                .update("complianceRate", complianceRate)
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "Error updating compliance rate", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun onMedicationTaken(medicationLogRepository: MedicationLogRepository) {
        updateStreak(medicationLogRepository)
        updateComplianceRate(medicationLogRepository)
        getUserStats()?.let { achievementRepository.checkAllAchievements(it) }
    }

    suspend fun onMedicationMissed(medicationLogRepository: MedicationLogRepository) {
        breakStreak()
        updateComplianceRate(medicationLogRepository)
    }
}
