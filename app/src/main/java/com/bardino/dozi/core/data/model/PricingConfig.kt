package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * 💰 Fiyatlandırma Konfigürasyonu
 *
 * Firestore'dan çekilen dinamik fiyat bilgileri.
 * Collection: config/pricing
 */
data class PricingConfig(
    val ekstraMonthly: PlanPricing = PlanPricing(),
    val ekstraYearly: PlanPricing = PlanPricing(),
    val aileMonthly: PlanPricing = PlanPricing(),
    val aileYearly: PlanPricing = PlanPricing(),
    val trialDurationDays: Int = 3,
    @ServerTimestamp
    val lastUpdated: Timestamp? = null
)

/**
 * 📋 Tek Plan Fiyat Bilgisi
 */
data class PlanPricing(
    val price: Float = 0f,
    val currency: String = "TRY",
    val durationDays: Int = 30,
    val displayName: String = "",
    val isActive: Boolean = true,
    val savingsPercent: Int = 0,  // Yıllık planlarda tasarruf yüzdesi
    val discountPercent: Int = 0,  // Kampanya indirimi
    val campaignEndDate: Timestamp? = null
) {
    /**
     * İndirimli fiyatı hesaplar
     */
    fun getDiscountedPrice(): Float {
        return if (discountPercent > 0) {
            price * (1 - discountPercent / 100f)
        } else {
            price
        }
    }

    /**
     * Kampanya aktif mi kontrol eder
     */
    fun isCampaignActive(): Boolean {
        if (discountPercent <= 0) return false
        val now = System.currentTimeMillis()
        val endDate = campaignEndDate?.toDate()?.time ?: return true
        return now < endDate
    }

    /**
     * Görüntülenecek fiyat stringi
     */
    fun getDisplayPrice(): String {
        val finalPrice = if (isCampaignActive()) getDiscountedPrice() else price
        return "%.2f %s".format(finalPrice, getCurrencySymbol())
    }

    private fun getCurrencySymbol(): String = when (currency) {
        "TRY" -> "₺"
        "USD" -> "$"
        "EUR" -> "€"
        else -> currency
    }
}

/**
 * 🔄 Varsayılan fiyatlar (offline fallback)
 */
object DefaultPricing {
    val config = PricingConfig(
        ekstraMonthly = PlanPricing(
            price = 149.99f,
            currency = "TRY",
            durationDays = 30,
            displayName = "Ekstra Aylık",
            isActive = true
        ),
        ekstraYearly = PlanPricing(
            price = 999.99f,
            currency = "TRY",
            durationDays = 365,
            displayName = "Ekstra Yıllık",
            isActive = true,
            savingsPercent = 44
        ),
        aileMonthly = PlanPricing(
            price = 249.99f,
            currency = "TRY",
            durationDays = 30,
            displayName = "Aile Aylık",
            isActive = true
        ),
        aileYearly = PlanPricing(
            price = 1999.99f,
            currency = "TRY",
            durationDays = 365,
            displayName = "Aile Yıllık",
            isActive = true,
            savingsPercent = 33
        ),
        trialDurationDays = 3
    )
}
