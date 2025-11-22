package com.bardino.dozi.core.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.bardino.dozi.DoziApplication
import com.bardino.dozi.core.data.model.*
import com.bardino.dozi.notifications.NotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏆 Achievement Repository - Rozet sistemi yönetimi
 */
@Singleton
class AchievementRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "AchievementRepository"

    /**
     * Get achievements collection for current user
     */
    private fun getAchievementsCollection() = auth.currentUser?.let { user ->
        firestore.collection("users").document(user.uid).collection("achievements")
    }

    /**
     * Kullanıcının tüm başarılarını getir
     */
    suspend fun getAllAchievements(): List<Achievement> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()
            val collection = getAchievementsCollection() ?: return emptyList()

            val snapshot = collection.get().await()

            if (snapshot.isEmpty) {
                // İlk kez - Tüm başarıları oluştur (kilitli halde)
                Log.d(TAG, "First time - Creating all achievements")
                initializeAchievements(userId)
                return getAllAchievements() // Recursive call after initialization
            }

            snapshot.documents.mapNotNull { it.toObject(Achievement::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting achievements", e)
            emptyList()
        }
    }

    /**
     * Real-time achievements flow
     */
    fun getAchievementsFlow(): Flow<List<Achievement>> {
        val collection = getAchievementsCollection()
        if (collection == null) {
            return callbackFlow {
                trySend(emptyList())
                awaitClose {}
            }
        }

        return callbackFlow {
            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to achievements", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val achievements = snapshot?.documents?.mapNotNull {
                    it.toObject(Achievement::class.java)
                } ?: emptyList()

                trySend(achievements)
            }

            awaitClose { listener.remove() }
        }
    }

    /**
     * Belirli bir başarıyı getir
     */
    suspend fun getAchievement(achievementId: String): Achievement? {
        return try {
            val collection = getAchievementsCollection() ?: return null
            val doc = collection.document(achievementId).get().await()
            doc.toObject(Achievement::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting achievement: $achievementId", e)
            null
        }
    }

    /**
     * Başarı ilerlemesini güncelle
     */
    suspend fun updateAchievementProgress(
        achievementType: AchievementType,
        currentProgress: Int
    ): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val collection = getAchievementsCollection() ?: return false

            val achievementId = "${userId}_${achievementType.name}"

            // Başarı var mı kontrol et
            val doc = collection.document(achievementId).get().await()

            if (!doc.exists()) {
                // Yoksa oluştur
                val newAchievement = Achievement(
                    id = achievementId,
                    userId = userId,
                    type = achievementType,
                    progress = currentProgress,
                    target = achievementType.target,
                    isUnlocked = currentProgress >= achievementType.target
                )
                collection.document(achievementId).set(newAchievement).await()
                Log.d(TAG, "Achievement created: ${achievementType.displayName}")
            } else {
                val achievement = doc.toObject(Achievement::class.java)
                if (achievement != null && !achievement.isUnlocked) {
                    // Henüz kilitli - İlerlemeyi güncelle
                    val isNowUnlocked = currentProgress >= achievementType.target

                    collection.document(achievementId).update(
                        mapOf(
                            "progress" to currentProgress,
                            "isUnlocked" to isNowUnlocked,
                            "unlockedAt" to if (isNowUnlocked) Timestamp.now() else null
                        )
                    ).await()

                    if (isNowUnlocked) {
                        Log.d(TAG, "🎉 Achievement UNLOCKED: ${achievementType.displayName}")
                        // Bildirim gönder
                        val unlockedAchievement = achievement.copy(
                            isUnlocked = true,
                            progress = currentProgress,
                            unlockedAt = Timestamp.now()
                        )
                        sendAchievementUnlockedNotification(unlockedAchievement)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating achievement progress", e)
            false
        }
    }

    /**
     * Kullanıcı için tüm başarıları başlat (ilk kez kullanımda)
     */
    private suspend fun initializeAchievements(userId: String) {
        try {
            val collection = getAchievementsCollection() ?: return

            AchievementType.entries.forEach { type ->
                val achievementId = "${userId}_${type.name}"
                val achievement = Achievement(
                    id = achievementId,
                    userId = userId,
                    type = type,
                    isUnlocked = false,
                    progress = 0,
                    target = type.target
                )

                collection.document(achievementId).set(achievement).await()
            }

            Log.d(TAG, "✅ Initialized ${AchievementType.entries.size} achievements")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing achievements", e)
        }
    }

    /**
     * Kullanıcının kazandığı (unlocked) başarıları getir
     */
    suspend fun getUnlockedAchievements(): List<Achievement> {
        return getAllAchievements().filter { it.isUnlocked }
    }

    /**
     * Kullanıcının henüz kazanmadığı (locked) başarıları getir
     */
    suspend fun getLockedAchievements(): List<Achievement> {
        return getAllAchievements().filter { !it.isUnlocked }
    }

    /**
     * Başarı sayılarını getir
     */
    suspend fun getAchievementStats(): Pair<Int, Int> {
        val all = getAllAchievements()
        val unlocked = all.count { it.isUnlocked }
        val total = all.size
        return Pair(unlocked, total)
    }

    /**
     * Streak başarılarını kontrol et ve güncelle
     */
    suspend fun checkStreakAchievements(currentStreak: Int) {
        listOf(
            AchievementType.STREAK_7_DAYS,
            AchievementType.STREAK_30_DAYS,
            AchievementType.STREAK_100_DAYS,
            AchievementType.STREAK_365_DAYS
        ).forEach { type ->
            if (currentStreak >= type.target) {
                updateAchievementProgress(type, currentStreak)
            }
        }
    }

    /**
     * Perfect week/month başarılarını kontrol et
     */
    suspend fun checkPerfectComplianceAchievements(consecutivePerfectDays: Int) {
        when {
            consecutivePerfectDays >= 30 -> {
                updateAchievementProgress(AchievementType.PERFECT_MONTH, consecutivePerfectDays)
            }
            consecutivePerfectDays >= 7 -> {
                updateAchievementProgress(AchievementType.PERFECT_WEEK, consecutivePerfectDays)
            }
        }
    }

    /**
     * İlk adım başarılarını kontrol et
     */
    suspend fun checkFirstStepAchievements(
        hasMedicine: Boolean = false,
        hasTakenDose: Boolean = false
    ) {
        if (hasMedicine) {
            updateAchievementProgress(AchievementType.FIRST_MEDICINE, 1)
        }
        if (hasTakenDose) {
            updateAchievementProgress(AchievementType.FIRST_DOSE_TAKEN, 1)
        }
    }

    /**
     * Koleksiyon başarılarını kontrol et (ilaç sayısı)
     */
    suspend fun checkMedicineCollectorAchievements(totalMedicines: Int) {
        if (totalMedicines >= 5) {
            updateAchievementProgress(AchievementType.MEDICINE_COLLECTOR_5, totalMedicines)
        }
        if (totalMedicines >= 10) {
            updateAchievementProgress(AchievementType.MEDICINE_COLLECTOR_10, totalMedicines)
        }
    }

    /**
     * Toplam doz başarılarını kontrol et
     */
    suspend fun checkTotalDosesAchievements(totalDoses: Int) {
        listOf(
            AchievementType.TOTAL_DOSES_50,
            AchievementType.TOTAL_DOSES_100,
            AchievementType.TOTAL_DOSES_365,
            AchievementType.TOTAL_DOSES_1000
        ).forEach { type ->
            if (totalDoses >= type.target) {
                updateAchievementProgress(type, totalDoses)
            }
        }
    }

    /**
     * Tüm başarıları kontrol et ve güncelle
     * (İlaç alındığında, ilaç eklendiğinde, vb. çağrılır)
     */
    suspend fun checkAllAchievements(userStats: UserStats) {
        // Streak başarıları
        checkStreakAchievements(userStats.currentStreak)

        // Perfect compliance başarıları
        // TODO: consecutivePerfectDays hesapla

        // First step başarıları
        checkFirstStepAchievements(
            hasMedicine = userStats.totalMedicines > 0,
            hasTakenDose = userStats.totalDosesTaken > 0
        )

        // Koleksiyon başarıları
        checkMedicineCollectorAchievements(userStats.totalMedicines)

        // Toplam doz başarıları
        checkTotalDosesAchievements(userStats.totalDosesTaken)
    }

    /**
     * Başarı kilidi açıldığında bildirim gönder
     */
    private suspend fun sendAchievementUnlockedNotification(achievement: Achievement) {
        try {
            val userId = auth.currentUser?.uid ?: return

            // Firebase'e kaydet
            val notification = mapOf(
                "userId" to userId,
                "type" to "ACHIEVEMENT",
                "title" to "🏆 Başarı Kazandın!",
                "body" to "${achievement.type.emoji} ${achievement.type.displayName}: ${achievement.type.description}",
                "isRead" to false,
                "achievementId" to achievement.id,
                "createdAt" to Timestamp.now()
            )

            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "Achievement notification saved to Firestore")

            // Cihaza bildirim gönder
            val context = DoziApplication.instance
            if (context != null && hasNotificationPermission(context)) {
                NotificationHelper.showAchievementNotification(
                    context = context,
                    achievementName = achievement.type.displayName,
                    achievementDescription = achievement.type.description,
                    achievementEmoji = achievement.type.emoji,
                    achievementId = achievement.id
                )
                Log.d(TAG, "🏆 Achievement device notification sent: ${achievement.type.displayName}")
            } else {
                Log.w(TAG, "⚠️ Cannot send device notification - context null or no permission")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending achievement notification", e)
        }
    }

    /**
     * Bildirim izni kontrolü
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
