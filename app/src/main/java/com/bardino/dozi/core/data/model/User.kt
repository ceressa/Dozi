package com.bardino.dozi.core.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = 0L,
    val planType: String = "free",
    val timezone: String = "Europe/Istanbul",
    val language: String = "tr",
    val vibration: Boolean = true,
    val theme: String = "light",
    val voiceGender: String = "erkek", // "erkek" (Ozan) veya "kadin" (Efsun)
    val onboardingCompleted: Boolean = false,

    // 🤝 Buddy sistem için
    val fcmToken: String? = null,        // Firebase Cloud Messaging token
    val buddyCode: String? = null,       // 6 haneli buddy kodu

    // 🔕 DND (Do Not Disturb) ayarları
    val dndEnabled: Boolean = false,     // DND aktif mi?
    val dndStartHour: Int = 22,          // DND başlangıç saati (0-23)
    val dndStartMinute: Int = 0,         // DND başlangıç dakikası (0-59)
    val dndEndHour: Int = 8,             // DND bitiş saati (0-23)
    val dndEndMinute: Int = 0,           // DND bitiş dakikası (0-59)

    // 🔔 Adaptive timing (akıllı zamanlama)
    val adaptiveTimingEnabled: Boolean = false,  // Adaptive timing aktif mi?
    val preferredMorningHour: Int = 8,           // Sabah tercihi (7-11)
    val preferredEveningHour: Int = 20           // Akşam tercihi (18-22)
)
