package com.bardino.dozi.core.data.repository

import android.util.Log
import com.bardino.dozi.core.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * UserStats Repository - Kullanıcı istatistikleri ve gamification
 */
class UserStatsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "UserStatsRepository"

    /**
     * Kullanıcının stats belgesini al
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
                // İlk kez oluştur
                val initialStats = UserStats(
                    id = userId,
                    userId = userId,
                    currentStreak = 0,
                    longestStreak = 0,
                    lastStreakDate = null
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
     * Streak'i güncelle (her gün ilaç alındığında çağrılır)
     */
    suspend fun updateStreak(medicationLogRepository: MedicationLogRepository) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val stats = getUserStats() ?: return

            // Bugünün tarihini al
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            // Son streak tarihini kontrol et
            val lastStreakDate = stats.lastStreakDate?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())?.toLocalDate()

            // Bugün tüm ilaçlar alındı mı kontrol et
            val todayLogs = medicationLogRepository.getMedicationLogsForDate(today)
            val allTakenToday = todayLogs.isNotEmpty() &&
                todayLogs.all { it.status == MedicationStatus.TAKEN }

            if (!allTakenToday) {
                Log.d(TAG, "Not all medications taken today, streak not updated")
                return
            }

            // Streak hesapla
            val newStreak = when {
                lastStreakDate == null -> 1 // İlk gün
                lastStreakDate == yesterday -> stats.currentStreak + 1 // Ardışık
                lastStreakDate == today -> stats.currentStreak // Bugün zaten sayılmış
                else -> 1 // Kopmuş, yeniden başla
            }

            val newLongestStreak = maxOf(newStreak, stats.longestStreak)

            // Firestore'u güncelle
            firestore.collection("user_stats")
                .document(userId)
                .update(
                    mapOf(
                        "currentStreak" to newStreak,
                        "longestStreak" to newLongestStreak,
                        "lastStreakDate" to Timestamp(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())),
                        "totalMedicationsTaken" to (stats.totalMedicationsTaken + 1)
                    )
                )
                .await()

            Log.d(TAG, "Streak updated: $newStreak days")

            // Achievement kontrolü
            checkAndUnlockAchievements(newStreak, stats)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating streak", e)
        }
    }

    /**
     * İlaç kaçırıldığında streak'i sıfırla
     */
    suspend fun breakStreak() {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection("user_stats")
                .document(userId)
                .update(
                    mapOf(
                        "currentStreak" to 0,
                        "totalMedicationsMissed" to com.google.firebase.firestore.FieldValue.increment(1)
                    )
                )
                .await()

            Log.d(TAG, "Streak broken")
        } catch (e: Exception) {
            Log.e(TAG, "Error breaking streak", e)
        }
    }

    /**
     * Achievement kontrolü ve kilidi açma
     */
    private suspend fun checkAndUnlockAchievements(currentStreak: Int, stats: UserStats) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val unlockedAchievements = stats.achievements.toMutableList()

            Achievements.ALL.forEach { achievement ->
                if (achievement.id !in unlockedAchievements) {
                    val unlocked = when (val req = achievement.requirement) {
                        is AchievementRequirement.StreakDays -> currentStreak >= req.days
                        is AchievementRequirement.TotalMedications -> stats.totalMedicationsTaken >= req.count
                        is AchievementRequirement.ComplianceRate -> stats.complianceRate >= req.rate
                        is AchievementRequirement.ConsecutivePerfectDays -> currentStreak >= req.days
                    }

                    if (unlocked) {
                        unlockedAchievements.add(achievement.id)
                        Log.d(TAG, "Achievement unlocked: ${achievement.title}")

                        // Bildirim gönder (opsiyonel, NotificationRepository ile)
                        sendAchievementNotification(achievement)
                    }
                }
            }

            // Güncelle
            if (unlockedAchievements.size > stats.achievements.size) {
                firestore.collection("user_stats")
                    .document(userId)
                    .update("achievements", unlockedAchievements)
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking achievements", e)
        }
    }

    /**
     * Achievement bildirimi gönder
     */
    private suspend fun sendAchievementNotification(achievement: Achievement) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val notification = DoziNotification(
                userId = userId,
                type = NotificationType.ACHIEVEMENT,
                title = "🏆 Başarı Kazandın!",
                message = "${achievement.icon} ${achievement.title}: ${achievement.description}",
                isRead = false,
                createdAt = Timestamp.now()
            )

            firestore.collection("notifications")
                .add(notification)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending achievement notification", e)
        }
    }

    /**
     * Uyumluluk oranını hesapla ve güncelle
     */
    suspend fun updateComplianceRate(medicationLogRepository: MedicationLogRepository) {
        try {
            val userId = auth.currentUser?.uid ?: return

            // Son 30 günün loglarını al
            val last30Days = (0..29).map { LocalDate.now().minusDays(it.toLong()) }
            var totalScheduled = 0
            var totalTaken = 0

            last30Days.forEach { date ->
                val logs = medicationLogRepository.getMedicationLogsForDate(date)
                totalScheduled += logs.size
                totalTaken += logs.count { it.status == MedicationStatus.TAKEN }
            }

            val complianceRate = if (totalScheduled > 0) {
                (totalTaken.toFloat() / totalScheduled * 100)
            } else {
                0f
            }

            firestore.collection("user_stats")
                .document(userId)
                .update("complianceRate", complianceRate)
                .await()

            Log.d(TAG, "Compliance rate updated: $complianceRate%")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating compliance rate", e)
        }
    }

    /**
     * İlaç alındı - stats güncelle
     */
    suspend fun onMedicationTaken(medicationLogRepository: MedicationLogRepository) {
        updateStreak(medicationLogRepository)
        updateComplianceRate(medicationLogRepository)
    }

    /**
     * İlaç kaçırıldı - stats güncelle
     */
    suspend fun onMedicationMissed(medicationLogRepository: MedicationLogRepository) {
        breakStreak()
        updateComplianceRate(medicationLogRepository)
    }
}
