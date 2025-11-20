package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * 🏆 Başarı/Rozet Sistemi
 */
data class Achievement(
    val id: String = "",
    val userId: String = "",
    val type: AchievementType = AchievementType.STREAK_7_DAYS,
    val isUnlocked: Boolean = false,
    @ServerTimestamp
    val unlockedAt: Timestamp? = null,
    val progress: Int = 0,              // Mevcut ilerleme
    val target: Int = 0,                // Hedef (örn: 7 gün, 10 ilaç)
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

/**
 * 🎖️ Başarı Tipleri
 */
enum class AchievementType(
    val displayName: String,
    val description: String,
    val emoji: String,
    val target: Int,
    val color: String
) {
    // 🔥 Streak Başarıları
    STREAK_7_DAYS(
        "Ateşli Başlangıç",
        "7 gün üst üste ilaç al",
        "🔥",
        7,
        "#FF5722"
    ),
    STREAK_30_DAYS(
        "Kararlı",
        "30 gün üst üste ilaç al",
        "💪",
        30,
        "#FF9800"
    ),
    STREAK_100_DAYS(
        "Efsane",
        "100 gün üst üste ilaç al",
        "🌟",
        100,
        "#FFC107"
    ),
    STREAK_365_DAYS(
        "Yılın Kralı",
        "365 gün üst üste ilaç al",
        "👑",
        365,
        "#FFD700"
    ),

    // 🎯 Uyum Başarıları
    PERFECT_WEEK(
        "Mükemmel Hafta",
        "Bir hafta boyunca %100 uyum sağla",
        "⭐",
        7,
        "#4CAF50"
    ),
    PERFECT_MONTH(
        "Mükemmel Ay",
        "Bir ay boyunca %100 uyum sağla",
        "🏅",
        30,
        "#2196F3"
    ),

    // 🏅 İlk Adımlar
    FIRST_MEDICINE(
        "İlk Adım",
        "İlk ilacını ekle",
        "💊",
        1,
        "#9C27B0"
    ),
    FIRST_DOSE_TAKEN(
        "Başlangıç",
        "İlk dozunu al",
        "✅",
        1,
        "#00BCD4"
    ),

    // 📚 Koleksiyon Başarıları
    MEDICINE_COLLECTOR_5(
        "Yeni Başlayan",
        "5 farklı ilaç ekle",
        "📦",
        5,
        "#795548"
    ),
    MEDICINE_COLLECTOR_10(
        "Uzman",
        "10 farklı ilaç ekle",
        "📚",
        10,
        "#607D8B"
    ),

    // 📊 Toplam Doz Başarıları
    TOTAL_DOSES_50(
        "Yarım Yüzyıl",
        "Toplam 50 doz al",
        "🎯",
        50,
        "#E91E63"
    ),
    TOTAL_DOSES_100(
        "Yüzüncü Doz",
        "Toplam 100 doz al",
        "💯",
        100,
        "#F44336"
    ),
    TOTAL_DOSES_365(
        "Yıllık Başarı",
        "Toplam 365 doz al",
        "🎊",
        365,
        "#9C27B0"
    ),
    TOTAL_DOSES_1000(
        "Binyıl",
        "Toplam 1000 doz al",
        "🏆",
        1000,
        "#FFD700"
    );

    fun getProgressPercentage(current: Int): Float {
        if (target == 0) return 0f
        return (current.toFloat() / target * 100).coerceIn(0f, 100f)
    }
}

/**
 * Note: UserStats model is defined in UserStats.kt
 * Fields used for achievements:
 * - currentStreak: Int
 * - longestStreak: Int
 * - totalMedicationsTaken: Int (total doses)
 * - achievements: List<String>
 */