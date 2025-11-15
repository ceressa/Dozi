package com.bardino.dozi.core.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = 0L,
    val planType: String = "free", // "free", "trial", "weekly", "monthly", "yearly", "lifetime"
    val timezone: String = "Europe/Istanbul",
    val language: String = "tr",
    val vibration: Boolean = true,
    val theme: String = "light",
    val voiceGender: String = "erkek", // "erkek" (Ozan) veya "kadin" (Efsun)
    val onboardingCompleted: Boolean = false,

    // 🌟 Premium (Dozi Ekstra) bilgileri
    val isPremium: Boolean = false,              // Premium aktif mi?
    val isTrial: Boolean = false,                // Deneme sürümü mü?
    val premiumExpiryDate: Long = 0L,            // Premium bitiş tarihi (timestamp)
    val premiumStartDate: Long = 0L,             // Premium başlangıç tarihi

    // 🚫 Ban sistemi
    val isBanned: Boolean = false,               // Kullanıcı banlandı mı?
    val banReason: String? = null,               // Ban nedeni

    // 🤝 Badi sistem için
    val fcmToken: String? = null,                // Firebase Cloud Messaging token
    val buddyCode: String? = null,               // 6 haneli buddy kodu

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

    // 🎵 Bildirim sesi özelleştirme (Premium özellik)
    val customSoundUri: String? = null,          // Özel bildirim sesi URI
    val customSoundName: String = "Varsayılan"   // Özel ses adı
) {
    /**
     * Kullanıcının şu anda premium olup olmadığını kontrol eder
     */
    fun isCurrentlyPremium(): Boolean {
        if (!isPremium) return false
        val now = System.currentTimeMillis()
        return now < premiumExpiryDate
    }

    /**
     * Premium'un kaç gün kaldığını hesaplar
     */
    fun premiumDaysRemaining(): Int {
        if (!isCurrentlyPremium()) return 0
        val now = System.currentTimeMillis()
        val diff = premiumExpiryDate - now
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * Plan tipini PremiumPlanType enum'a çevirir
     */
    fun getPremiumPlanType(): PremiumPlanType {
        return when (planType.lowercase()) {
            "trial" -> PremiumPlanType.TRIAL
            "weekly" -> PremiumPlanType.WEEKLY
            "monthly" -> PremiumPlanType.MONTHLY
            "yearly" -> PremiumPlanType.YEARLY
            "lifetime" -> PremiumPlanType.LIFETIME
            else -> PremiumPlanType.FREE
        }
    }
}
