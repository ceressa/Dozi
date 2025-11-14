package com.bardino.dozi.core.utils

import android.util.Log
import com.bardino.dozi.core.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🔥 Streak Manager
 * Kullanıcının ardışık ilaç alma düzenini yönetir
 */
class StreakManager {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "StreakManager"
        private const val COLLECTION_USER_STATS = "userStats"
    }

    /**
     * Kullanıcının streak'ini güncelle
     * Her gün en az bir ilaç alındığında çağrılmalı
     */
    suspend fun updateStreak() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val today = getTodayDateString()

            val statsDoc = firestore.collection(COLLECTION_USER_STATS)
                .document(userId)
                .get()
                .await()

            val currentStats = if (statsDoc.exists()) {
                statsDoc.toObject(UserStats::class.java) ?: UserStats(userId = userId)
            } else {
                UserStats(userId = userId)
            }

            // Son streak tarihi bugün mü?
            val lastStreakDate = currentStats.lastStreakDate?.toDate()
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.time

            val newStreak = when {
                lastStreakDate == null -> {
                    // İlk streak
                    1
                }
                isSameDay(lastStreakDate, Date()) -> {
                    // Bugün zaten güncellenmiş
                    currentStats.currentStreak
                }
                isSameDay(lastStreakDate, yesterday) -> {
                    // Dün güncellenmiş, streak devam ediyor
                    currentStats.currentStreak + 1
                }
                else -> {
                    // Streak kırıldı, yeniden başla
                    1
                }
            }

            val updatedStats = currentStats.copy(
                currentStreak = newStreak,
                longestStreak = maxOf(newStreak, currentStats.longestStreak),
                lastStreakDate = Timestamp.now()
            )

            // Firestore'a kaydet
            firestore.collection(COLLECTION_USER_STATS)
                .document(userId)
                .set(updatedStats)
                .await()

            Log.d(TAG, "✅ Streak updated: $newStreak days (longest: ${updatedStats.longestStreak})")

            // Achievement kontrolü yap
            checkAchievements(updatedStats)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating streak", e)
        }
    }

    /**
     * Kullanıcının istatistiklerini güncelle
     */
    suspend fun updateStats(
        takenCount: Int = 0,
        missedCount: Int = 0,
        skippedCount: Int = 0
    ) {
        try {
            val userId = auth.currentUser?.uid ?: return

            val statsDoc = firestore.collection(COLLECTION_USER_STATS)
                .document(userId)
                .get()
                .await()

            val currentStats = if (statsDoc.exists()) {
                statsDoc.toObject(UserStats::class.java) ?: UserStats(userId = userId)
            } else {
                UserStats(userId = userId)
            }

            val newTotalTaken = currentStats.totalMedicationsTaken + takenCount
            val newTotalMissed = currentStats.totalMedicationsMissed + missedCount
            val newTotalSkipped = currentStats.totalMedicationsSkipped + skippedCount

            val totalAttempts = newTotalTaken + newTotalMissed + newTotalSkipped
            val newComplianceRate = if (totalAttempts > 0) {
                (newTotalTaken.toFloat() / totalAttempts.toFloat()) * 100
            } else {
                0f
            }

            val updatedStats = currentStats.copy(
                totalMedicationsTaken = newTotalTaken,
                totalMedicationsMissed = newTotalMissed,
                totalMedicationsSkipped = newTotalSkipped,
                complianceRate = newComplianceRate
            )

            firestore.collection(COLLECTION_USER_STATS)
                .document(userId)
                .set(updatedStats)
                .await()

            Log.d(TAG, "✅ Stats updated: Taken=$newTotalTaken, Compliance=${newComplianceRate}%")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating stats", e)
        }
    }

    /**
     * Achievement kontrolü yap ve kazanılanları kaydet
     */
    private suspend fun checkAchievements(stats: UserStats) {
        try {
            val newAchievements = mutableListOf<String>()

            // Streak bazlı achievement'lar
            if (stats.currentStreak >= 7 && !stats.achievements.contains("first_week")) {
                newAchievements.add("first_week")
                Log.d(TAG, "🏅 Achievement unlocked: İlk Hafta!")
            }

            if (stats.currentStreak >= 30 && !stats.achievements.contains("30_days")) {
                newAchievements.add("30_days")
                Log.d(TAG, "🎖️ Achievement unlocked: Bir Ay!")
            }

            if (stats.currentStreak >= 365 && !stats.achievements.contains("year_streak")) {
                newAchievements.add("year_streak")
                Log.d(TAG, "🏆 Achievement unlocked: Bir Yıl!")
            }

            // İlaç sayısı bazlı achievement'lar
            if (stats.totalMedicationsTaken >= 100 && !stats.achievements.contains("hundred_meds")) {
                newAchievements.add("hundred_meds")
                Log.d(TAG, "💯 Achievement unlocked: Yüzlük Kulüp!")
            }

            // Uyumluluk oranı bazlı achievement'lar
            if (stats.complianceRate >= 100f && !stats.achievements.contains("perfect_month")) {
                newAchievements.add("perfect_month")
                Log.d(TAG, "👑 Achievement unlocked: Mükemmel Ay!")
            }

            // Yeni achievement'lar varsa kaydet
            if (newAchievements.isNotEmpty()) {
                val userId = auth.currentUser?.uid ?: return
                val allAchievements = stats.achievements + newAchievements

                firestore.collection(COLLECTION_USER_STATS)
                    .document(userId)
                    .update("achievements", allAchievements)
                    .await()

                // TODO: Achievement bildirimi göster
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking achievements", e)
        }
    }

    /**
     * Kullanıcı istatistiklerini çek
     */
    suspend fun getUserStats(): UserStats? {
        return try {
            val userId = auth.currentUser?.uid ?: return null

            val statsDoc = firestore.collection(COLLECTION_USER_STATS)
                .document(userId)
                .get()
                .await()

            if (statsDoc.exists()) {
                statsDoc.toObject(UserStats::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user stats", e)
            null
        }
    }

    /**
     * İki tarihin aynı gün olup olmadığını kontrol et
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Bugünün tarih string'ini döndür (yyyy-MM-dd)
     */
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
