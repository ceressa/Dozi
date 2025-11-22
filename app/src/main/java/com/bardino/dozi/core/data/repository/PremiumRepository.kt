package com.bardino.dozi.core.data.repository

import com.bardino.dozi.core.data.model.*
import com.bardino.dozi.core.premium.PremiumFields
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🌟 Premium Repository
 *
 * Premium (Dozi Ekstra) ile ilgili tüm işlemleri yönetir:
 * - Premium durumu kontrolü
 * - Deneme sürümü aktivasyonu
 * - Satın alma işlemleri
 * - Real-time premium status updates
 */
@Singleton
class PremiumRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val PREMIUM_ANALYTICS_DOC = "premium_analytics"
        private const val DAILY_ANALYTICS_COLLECTION = "daily_analytics"
        private const val USER_BANS_COLLECTION = "user_bans"
    }

    /**
     * Real-time kullanıcı premium durumunu dinle
     */
    fun observePremiumStatus(): Flow<User?> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Kullanıcının şu anki premium durumunu getir
     */
    suspend fun getCurrentPremiumStatus(): User? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 3 günlük ücretsiz deneme başlat (onboarding için)
     * NOT: Trial sadece bir kez verilebilir! trialUsedAt ile kontrol edilir.
     */
    suspend fun activateTrialPeriod(userId: String): Boolean {
        return try {
            // Önce kullanıcının trial kullanıp kullanmadığını kontrol et
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            val trialUsedAt = userDoc.getLong("trialUsedAt")
            val premiumExpiryDate = userDoc.getLong("premiumExpiryDate") ?: 0L

            // Trial daha önce kullanıldıysa, tekrar verme
            if (trialUsedAt != null) {
                android.util.Log.d("PremiumRepository", "❌ Trial daha önce kullanılmış: $trialUsedAt")
                return false
            }

            // Daha önce premium kullanıldıysa, trial verme
            if (premiumExpiryDate > 0) {
                android.util.Log.d("PremiumRepository", "❌ Daha önce premium kullanılmış")
                return false
            }

            val now = System.currentTimeMillis()
            val expiryDate = now + (com.bardino.dozi.core.common.Constants.TRIAL_DURATION_DAYS * 24 * 60 * 60 * 1000L)

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(PremiumFields.activePlan(PremiumPlanType.TRIAL, now, expiryDate, isTrial = true))
                .await()

            // Analytics güncelle
            incrementTrialStarts()

            android.util.Log.d("PremiumRepository", "✅ Trial aktivasyonu başarılı")
            true
        } catch (e: Exception) {
            android.util.Log.e("PremiumRepository", "❌ Trial aktivasyonu hatası: ${e.message}")
            false
        }
    }

    /**
     * Premium satın alımını kaydet
     */
    suspend fun activatePremium(
        userId: String,
        planType: PremiumPlanType,
        purchaseToken: String?,
        orderId: String?
    ): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val durationMillis = when (planType) {
                PremiumPlanType.EKSTRA_MONTHLY, PremiumPlanType.AILE_MONTHLY -> 30 * 24 * 60 * 60 * 1000L
                PremiumPlanType.EKSTRA_YEARLY, PremiumPlanType.AILE_YEARLY -> 365 * 24 * 60 * 60 * 1000L
                PremiumPlanType.TRIAL -> com.bardino.dozi.core.common.Constants.TRIAL_DURATION_DAYS * 24 * 60 * 60 * 1000L
                else -> 0L
            }

            val expiryDate = now + durationMillis

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(PremiumFields.activePlan(planType, now, expiryDate))
                .await()

            // Analytics güncelle
            incrementPremiumPurchases(planType)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Premium'u manuel olarak ayarla (Admin için DB'den)
     */
    suspend fun setUserPremium(
        userId: String,
        planType: PremiumPlanType,
        durationDays: Int = 0
    ): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val durationMillis = durationDays * 24 * 60 * 60 * 1000L
            val expiryDate = now + durationMillis

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(PremiumFields.activePlan(planType, now, expiryDate))
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Premium'u iptal et
     */
    suspend fun cancelPremium(userId: String): Boolean {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(PremiumFields.reset())
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kullanıcıyı banla
     */
    suspend fun banUser(
        userId: String,
        reason: String,
        adminUserId: String,
        isPermanent: Boolean = true,
        durationDays: Int = 0
    ): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val expiresAt = if (isPermanent) {
                Long.MAX_VALUE
            } else {
                now + (durationDays * 24 * 60 * 60 * 1000L)
            }

            // User'ı güncelle
            val userUpdates = hashMapOf<String, Any>(
                "isBanned" to true,
                "banReason" to reason
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(userUpdates)
                .await()

            // Ban dokümanı oluştur
            val banData = UserBan(
                userId = userId,
                isBanned = true,
                reason = reason,
                bannedBy = adminUserId,
                isPermanent = isPermanent
            )

            firestore.collection(USER_BANS_COLLECTION)
                .document(userId)
                .set(banData)
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kullanıcının ban'ını kaldır
     */
    suspend fun unbanUser(userId: String): Boolean {
        return try {
            val userUpdates = hashMapOf<String, Any>(
                "isBanned" to false,
                "banReason" to ""
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(userUpdates)
                .await()

            firestore.collection(USER_BANS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Kullanıcının ban durumunu kontrol et
     */
    suspend fun checkBanStatus(userId: String): UserBan? {
        return try {
            firestore.collection(USER_BANS_COLLECTION)
                .document(userId)
                .get()
                .await()
                .toObject(UserBan::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📊 ANALYTICS METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Premium analytics verilerini getir
     */
    suspend fun getPremiumAnalytics(): PremiumAnalytics? {
        return try {
            firestore.collection("analytics")
                .document(PREMIUM_ANALYTICS_DOC)
                .get()
                .await()
                .toObject(PremiumAnalytics::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Günlük analytics verilerini getir
     */
    suspend fun getDailyAnalytics(date: String): DailyAnalytics? {
        return try {
            firestore.collection(DAILY_ANALYTICS_COLLECTION)
                .document(date)
                .get()
                .await()
                .toObject(DailyAnalytics::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Son N günün analytics verilerini getir
     */
    suspend fun getRecentAnalytics(days: Int = 30): List<DailyAnalytics> {
        return try {
            val calendar = Calendar.getInstance()
            val dates = mutableListOf<String>()

            repeat(days) {
                val date = String.format(
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                dates.add(date)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            }

            firestore.collection(DAILY_ANALYTICS_COLLECTION)
                .whereIn("date", dates)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(DailyAnalytics::class.java) }
                .sortedByDescending { it.date }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun incrementTrialStarts() {
        try {
            val today = getTodayString()
            firestore.collection(DAILY_ANALYTICS_COLLECTION)
                .document(today)
                .update("trialStarts", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            // Create new document if doesn't exist
        }
    }

    private suspend fun incrementPremiumPurchases(planType: PremiumPlanType) {
        try {
            val today = getTodayString()
            val updates = hashMapOf<String, Any>(
                "premiumPurchases" to com.google.firebase.firestore.FieldValue.increment(1)
            )

            firestore.collection(DAILY_ANALYTICS_COLLECTION)
                .document(today)
                .update(updates)
                .await()
        } catch (e: Exception) {
            // Create new document if doesn't exist
        }
    }

    private fun getTodayString(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
