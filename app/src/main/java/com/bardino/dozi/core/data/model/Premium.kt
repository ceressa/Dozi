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
 * 📦 Plan Kategorileri
 */
enum class PlanCategory {
    FREE,
    EKSTRA,
    AILE;

    fun toTurkish(): String = when (this) {
        FREE -> "Ücretsiz"
        EKSTRA -> "Dozi Ekstra"
        AILE -> "Dozi Aile"
    }
}

/**
 * 💎 Premium Plan Tipleri
 *
 * Fiyatlar artık Firestore'dan çekilir (config/pricing)
 */
enum class PremiumPlanType(
    val id: String,
    val displayName: String,
    val category: PlanCategory,
    val productId: String
) {
    FREE("free", "Ücretsiz", PlanCategory.FREE, ""),
    TRIAL("trial", "3 Gün Deneme", PlanCategory.EKSTRA, ""),
    EKSTRA_MONTHLY("ekstra_monthly", "Ekstra Aylık", PlanCategory.EKSTRA, "dozi_ekstra_monthly"),
    EKSTRA_YEARLY("ekstra_yearly", "Ekstra Yıllık", PlanCategory.EKSTRA, "dozi_ekstra_yearly"),
    AILE_MONTHLY("aile_monthly", "Aile Aylık", PlanCategory.AILE, "dozi_aile_monthly"),
    AILE_YEARLY("aile_yearly", "Aile Yıllık", PlanCategory.AILE, "dozi_aile_yearly");

    fun isPremium(): Boolean = this != FREE && this != TRIAL

    fun isFamilyPlan(): Boolean = category == PlanCategory.AILE

    fun isEkstra(): Boolean = category == PlanCategory.EKSTRA

    fun isAile(): Boolean = category == PlanCategory.AILE

    fun toTurkish(): String = displayName

    companion object {
        fun fromId(id: String): PremiumPlanType {
            return entries.find { it.id == id } ?: FREE
        }

        fun fromProductId(productId: String): PremiumPlanType {
            return entries.find { it.productId == productId } ?: FREE
        }
    }
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
    val ekstraMonthlyUsers: Int = 0,
    val ekstraYearlyUsers: Int = 0,
    val aileMonthlyUsers: Int = 0,
    val aileYearlyUsers: Int = 0,
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
