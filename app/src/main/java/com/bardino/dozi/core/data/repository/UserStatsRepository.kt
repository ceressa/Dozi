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
 *
 * Basitleştirilmiş versiyon - sadece streak ve temel sayaçlar
 */
@Singleton
class UserStatsRepository @Inject constructor(
    private val achievementRepository: AchievementRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "UserStatsRepository"

    /**
     * Kullanıcı istatistiklerini getir veya oluştur
     */
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
                    totalMedicationsSkipped = 0,
                    quickResponseCount = 0,
                    buddyCount = 0,
                    buddyNotificationsSent = 0,
                    achievements = emptyList()
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

    /**
     * Streak güncelle - Basitleştirilmiş mantık
     *
     * Kurallar:
     * - lastStreakDate null → streak = 1 (ilk)
     * - lastStreakDate == bugün → streak değişmez
     * - lastStreakDate == dün → streak + 1 (devam)
     * - Aksi halde → streak = 1 (sıfırla)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateStreak(medicationLogRepository: MedicationLogRepository) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val stats = getUserStats() ?: return

            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val lastStreakDate = stats.lastStreakDate?.toDate()
                ?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

            // Bugün için logları kontrol et
            val todayLogs = medicationLogRepository.getMedicationLogsForDate(today)

            // En az bir ilaç alınmalı
            val hasTakenToday = todayLogs.any { it.status == MedicationStatus.TAKEN.name }

            if (!hasTakenToday) {
                Log.d(TAG, "No medications taken today, streak unchanged")
                return
            }

            // Yeni streak hesapla
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
                        "totalMedicationsTaken" to FieldValue.increment(1)
                    )
                )
                .await()

            Log.d(TAG, "✅ Streak updated: $newStreak (longest: $newLongestStreak)")

            // Başarıları kontrol et
            achievementRepository.checkStreakAchievements(newStreak)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating streak", e)
        }
    }

    /**
     * Streak sıfırla (ilaç kaçırıldığında)
     */
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

            Log.d(TAG, "❌ Streak broken")

        } catch (e: Exception) {
            Log.e(TAG, "Error breaking streak", e)
        }
    }

    /**
     * İlaç atlandığında
     */
    suspend fun onMedicationSkipped() {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection("user_stats")
                .document(userId)
                .update("totalMedicationsSkipped", FieldValue.increment(1))
                .await()

            Log.d(TAG, "⏭️ Medication skipped count incremented")

        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing skipped count", e)
        }
    }

    /**
     * Hızlı yanıt sayacını artır (eskalasyona gerek kalmadan alınan)
     */
    suspend fun incrementQuickResponseCount() {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection("user_stats")
                .document(userId)
                .update("quickResponseCount", FieldValue.increment(1))
                .await()

            // Başarıları kontrol et
            val stats = getUserStats()
            stats?.let {
                achievementRepository.checkQuickResponseAchievements(it.quickResponseCount)
            }

            Log.d(TAG, "⚡ Quick response count incremented")

        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing quick response count", e)
        }
    }

    /**
     * Badi sayacını artır
     */
    suspend fun incrementBuddyCount() {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection("user_stats")
                .document(userId)
                .update("buddyCount", FieldValue.increment(1))
                .await()

            // Başarıları kontrol et
            achievementRepository.checkFirstBuddyAchievement(true)

            Log.d(TAG, "👥 Buddy count incremented")

        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing buddy count", e)
        }
    }

    /**
     * Gönderilen badi bildirimi sayacını artır
     */
    suspend fun incrementBuddyNotificationsSent() {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection("user_stats")
                .document(userId)
                .update("buddyNotificationsSent", FieldValue.increment(1))
                .await()

            // Başarıları kontrol et
            val stats = getUserStats()
            stats?.let {
                achievementRepository.checkCaringBuddyAchievement(it.buddyNotificationsSent)
            }

            Log.d(TAG, "📤 Buddy notifications sent count incremented")

        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing buddy notifications sent", e)
        }
    }

    /**
     * İlaç alındığında çağrılır
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun onMedicationTaken(medicationLogRepository: MedicationLogRepository) {
        updateStreak(medicationLogRepository)
        getUserStats()?.let { achievementRepository.checkAllAchievements(it) }
    }

    /**
     * İlaç kaçırıldığında çağrılır
     */
    suspend fun onMedicationMissed() {
        breakStreak()
    }
}
