package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * 🌟 Dozi Ekstra (Premium) Subscription Model
 *
 * Kullanıcının premium durumunu, plan tipini ve süresini yönetir.
 */
data class PremiumSubscription(
    val userId: String = "",
    val isActive: Boolean = false,
    val planType: PremiumPlanType = PremiumPlanType.FREE,
    val isTrial: Boolean = false,
    @ServerTimestamp
    val startDate: Timestamp? = null,
    @ServerTimestamp
    val expiryDate: Timestamp? = null,
    val autoRenew: Boolean = false,
    val purchaseToken: String? = null,
    val orderId: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    /**
     * Premium aktif mi kontrol eder
     */
    fun isCurrentlyActive(): Boolean {
        if (!isActive) return false
        val now = System.currentTimeMillis()
        val expiry = expiryDate?.toDate()?.time ?: 0
        return now < expiry
    }

    /**
     * Kalan gün sayısını hesaplar
     */
    fun daysRemaining(): Int {
        if (!isCurrentlyActive()) return 0
        val now = System.currentTimeMillis()
        val expiry = expiryDate?.toDate()?.time ?: 0
        val diff = expiry - now
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}

/**
 * 💎 Premium Plan Tipleri
 */
enum class PremiumPlanType(
    val displayName: String,
    val durationDays: Int,
    val price: String,
    val productId: String
) {
    FREE("Ücretsiz", 0, "0", ""),
    TRIAL("Deneme", 7, "0", "dozi_trial"),
    WEEKLY("Haftalık", 7, "24.99", "dozi_weekly"),
    MONTHLY("Aylık", 30, "69.99", "dozi_monthly"),
    YEARLY("Yıllık", 365, "599.99", "dozi_yearly"),
    LIFETIME("Ömür Boyu", Int.MAX_VALUE, "1999.99", "dozi_lifetime");

    fun isPremium(): Boolean = this != FREE

    fun toTurkish(): String = displayName
}

/**
 * 📊 Premium Analytics Model
 *
 * Premium kullanıcı istatistiklerini takip eder.
 */
data class PremiumAnalytics(
    val totalUsers: Int = 0,
    val premiumUsers: Int = 0,
    val trialUsers: Int = 0,
    val weeklyUsers: Int = 0,
    val monthlyUsers: Int = 0,
    val yearlyUsers: Int = 0,
    val lifetimeUsers: Int = 0,
    val conversionRate: Float = 0f,
    val totalRevenue: Float = 0f,
    @ServerTimestamp
    val lastUpdated: Timestamp? = null
)

/**
 * 📈 Daily Analytics Snapshot
 *
 * Günlük kullanıcı aktivitesi ve retention metrikleri
 */
data class DailyAnalytics(
    val date: String = "", // "2025-11-15" formatında
    val activeUsers: Int = 0,
    val newSignups: Int = 0,
    val premiumPurchases: Int = 0,
    val trialStarts: Int = 0,
    val trialConversions: Int = 0,
    val notificationsSent: Int = 0,
    val medicationsTaken: Int = 0,
    val retention1Day: Float = 0f,
    val retention7Day: Float = 0f,
    val retention30Day: Float = 0f,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

/**
 * 🚫 Ban System Model
 *
 * Kullanıcı ban yönetimi için
 */
data class UserBan(
    val userId: String = "",
    val isBanned: Boolean = false,
    val reason: String = "",
    val bannedBy: String = "", // Admin user ID
    @ServerTimestamp
    val bannedAt: Timestamp? = null,
    @ServerTimestamp
    val expiresAt: Timestamp? = null, // null = permanent
    val isPermanent: Boolean = true
) {
    /**
     * Ban aktif mi kontrol eder
     */
    fun isCurrentlyBanned(): Boolean {
        if (!isBanned) return false
        if (isPermanent) return true

        val now = System.currentTimeMillis()
        val expiry = expiresAt?.toDate()?.time ?: Long.MAX_VALUE
        return now < expiry
    }
}

/**
 * 🎵 Notification Sound Settings (Premium Feature)
 */
data class NotificationSoundSettings(
    val userId: String = "",
    val soundEnabled: Boolean = true,
    val soundUri: String? = null, // Custom sound URI (premium only)
    val soundName: String = "Varsayılan",
    val vibrationPattern: List<Long> = listOf(0, 300, 200, 300), // Pattern (premium only)
    val volume: Float = 0.8f, // 0.0 - 1.0
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
