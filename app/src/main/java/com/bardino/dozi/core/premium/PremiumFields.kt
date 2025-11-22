package com.bardino.dozi.core.premium

import com.bardino.dozi.core.data.model.PremiumPlanType

/**
 * Tek noktadan premium alanlarının Firestore güncellemelerini üretir.
 */
object PremiumFields {
    fun activePlan(
        planType: PremiumPlanType,
        startDate: Long,
        expiryDate: Long,
        isTrial: Boolean = false
    ): Map<String, Any> {
        val fields = mutableMapOf<String, Any>(
            "isPremium" to true,
            "isTrial" to isTrial,
            "planType" to planType.id,
            "premiumStartDate" to startDate,
            "premiumExpiryDate" to expiryDate
        )

        // Trial aktivasyonunda trialUsedAt'i kaydet
        if (isTrial) {
            fields["trialUsedAt"] = startDate
        }

        return fields
    }

    fun reset(): Map<String, Any> = mapOf(
        "isPremium" to false,
        "isTrial" to false,
        "planType" to PremiumPlanType.FREE.id,
        "premiumStartDate" to 0L,
        "premiumExpiryDate" to 0L
    )
}
