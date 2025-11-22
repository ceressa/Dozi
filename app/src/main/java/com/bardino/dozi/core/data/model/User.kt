package com.bardino.dozi.core.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.Locale

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = 0L,
    val planType: String = "free", // "free", "trial", "ekstra_monthly", "ekstra_yearly", "aile_monthly", "aile_yearly"
    val timezone: String = "Europe/Istanbul",
    val language: String = "tr",
    val vibration: Boolean = true,
    val theme: String = "light",
    val voiceGender: String = "erkek", // "erkek" (Ozan) veya "kadin" (Efsun)
    val onboardingCompleted: Boolean = false,

    // 🌟 Premium (Dozi Ekstra) bilgileri
    @PropertyName("isPremium")
    val isPremium: Boolean = false,              // Premium aktif mi?

    @PropertyName("isTrial")
    val isTrial: Boolean = false,                // Deneme sürümü mü?

    @Exclude
    val legacyPremiumFlag: Boolean = false,

    @Exclude
val legacyCurrentlyPremium: Boolean = false,

@Exclude
val legacyPremiumPlanType: String? = null,


val premiumExpiryDate: Long = 0L,            // Premium bitiş tarihi (timestamp)
    val premiumStartDate: Long = 0L,             // Premium başlangıç tarihi
    val trialUsedAt: Long? = null,               // Trial ilk kez kullanıldığı tarih (null = hiç kullanmadı)

    // 🚫 Ban sistemi
    @PropertyName("isBanned")
    val isBanned: Boolean = false,               // Kullanıcı banlandı mı?

    val banReason: String? = null,               // Ban nedeni

    // 🤝 Badi sistem için
    val fcmToken: String? = null,                // Firebase Cloud Messaging token
    val buddyCode: String? = null,               // 6 haneli buddy kodu

    // 📱 Device bilgileri
    val deviceId: String? = null,                // Android Device ID (telefonu tanımlamak için)

    // 🔕 DND (Do Not Disturb) ayarları
    val dndEnabled: Boolean = false,             // DND aktif mi?
    val dndStartHour: Int = 22,                  // DND başlangıç saati (0-23)
    val dndStartMinute: Int = 0,                 // DND başlangıç dakikası (0-59)
    val dndEndHour: Int = 8,                     // DND bitiş saati (0-23)
    val dndEndMinute: Int = 0,                   // DND bitiş dakikası (0-59)

    // 🔔 Adaptive timing (akıllı zamanlama)
    val adaptiveTimingEnabled: Boolean = false,  // Adaptive timing aktif mi?
    val preferredMorningHour: Int = 8,           // Sabah tercihi (7-11)
    val preferredEveningHour: Int = 20,          // Akşam tercihi (18-22)

    // 🧠 Smart reminder (akıllı hatırlatma önerileri)
    val smartReminderEnabled: Boolean = false,   // Akıllı erteleme önerileri aktif mi? (default: kapalı)

    // 🔴 Important notifications (kritik hatırlatmalar - DND bypass)
    val importantNotificationsEnabled: Boolean = true,  // Önemli bildirimler aktif mi? (1 saat sonraki escalation)

    // 🎵 Bildirim sesi özelleştirme (Premium özellik)
    val customSoundUri: String? = null,          // Özel bildirim sesi URI
    val customSoundName: String = "Varsayılan",  // Özel ses adı

    // 👨‍👩‍👧‍👦 Aile Paketi (Dozi Ekstra Aile)
    val familyPlanId: String? = null,            // Hangi aile planına ait (null ise yok)
    val familyRole: String? = null,              // "ORGANIZER" veya "MEMBER"

    // 📍 Kayıtlı Konumlar (Firestore'da saklanıyor)
    val locations: List<Map<String, Any>> = emptyList()  // Kayıtlı konumlar listesi
) {
    /**
     * Kullanıcının şu anda premium olup olmadığını döndürür.
     * Getter conflict yaşamamak için is-prefiksli değildir.
     */
    @Exclude
    fun currentlyPremium(): Boolean {
        return premiumStatus().isActive
    }

    /**
     * Premium'un kaç gün kaldığını hesaplar.
     * Getter olarak algılanmaması için renamed.
     */
    @Exclude
    fun remainingPremiumDays(): Int {
        return premiumStatus().daysRemaining()
    }

    /**
     * Plan tipini PremiumPlanType olarak döndürür.
     */
    @Exclude
    fun resolvedPremiumPlanType(): PremiumPlanType {
        return premiumStatus().planType
    }

    /**
     * Premium durumunu normalize eder ve tek noktadan hesaplar.
     */
    @Exclude
    fun premiumStatus(now: Long = System.currentTimeMillis()): PremiumStatus {
        val planType = resolvePlanType()
        val expiry = premiumExpiryDate

        val hasPremiumFlag =
            isPremium || legacyPremiumFlag || legacyCurrentlyPremium || planType.isPremium()

        val isActive = hasPremiumFlag && expiry > now && planType != PremiumPlanType.FREE
        val isTrialActive = isTrial && isActive

        val source = when {
            isInFamilyPlan() -> PremiumSource.FAMILY
            isTrialActive -> PremiumSource.TRIAL
            isActive -> PremiumSource.INDIVIDUAL
            else -> PremiumSource.NONE
        }

        return PremiumStatus(
            planType = planType,
            isActive = isActive,
            isTrial = isTrialActive,
            premiumStartDate = premiumStartDate,
            premiumExpiryDate = expiry,
            source = source
        )
    }



    private fun resolvePlanType(): PremiumPlanType {
        val normalizedPlanId = normalizePlanId(planType)
            ?: normalizePlanId(legacyPremiumPlanType)
            ?: "free"

        return PremiumPlanType.fromId(normalizedPlanId)
    }

    private fun normalizePlanId(raw: String?): String? {
        val normalized = raw?.trim()?.lowercase(Locale.ROOT)
        if (normalized.isNullOrEmpty() || normalized == "free") return null

        return when (normalized) {
            "trial" -> PremiumPlanType.TRIAL.id
            "ekstra_monthly", "weekly", "monthly" -> PremiumPlanType.EKSTRA_MONTHLY.id
            "ekstra_yearly", "yearly", "lifetime" -> PremiumPlanType.EKSTRA_YEARLY.id
            "aile_monthly", "monthly_family" -> PremiumPlanType.AILE_MONTHLY.id
            "aile_yearly", "yearly_family", "family_premium" -> PremiumPlanType.AILE_YEARLY.id
            else -> normalized
        }
    }

    /**
     * Kullanıcı bir aile planının üyesi mi?
     */
    fun isInFamilyPlan(): Boolean {
        return !familyPlanId.isNullOrEmpty()
    }

    /**
     * Kullanıcı aile planının organizatörü mü?
     */
    fun isFamilyOrganizer(): Boolean {
        return familyRole == "ORGANIZER"
    }

    /**
     * Kullanıcı aile planının üyesi mi? (organizatör değil)
     */
    fun isFamilyMember(): Boolean {
        return familyRole == "MEMBER"
    }
}

data class PremiumStatus(
    val planType: PremiumPlanType,
    val isActive: Boolean,
    val isTrial: Boolean,
    val premiumStartDate: Long,
    val premiumExpiryDate: Long,
    val source: PremiumSource
) {
    fun daysRemaining(now: Long = System.currentTimeMillis()): Int {
        if (!isActive) return 0
        val diff = premiumExpiryDate - now
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}

enum class PremiumSource {
    INDIVIDUAL,
    FAMILY,
    TRIAL,
    NONE
}
