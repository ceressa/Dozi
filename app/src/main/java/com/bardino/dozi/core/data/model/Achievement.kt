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
    val target: Int = 0,                // Hedef (örn: 7 gün, 10 hatırlatma)
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

/**
 * 🎖️ Başarı Tipleri
 *
 * NOT: Başarımlar ilaç ALMAYA değil, DÜZENLİ OLMAYA teşvik eder.
 * Toplam doz/ilaç sayısı gibi başarımlar tehlikeli olduğu için kaldırıldı.
 */
enum class AchievementType(
    val displayName: String,
    val description: String,
    val emoji: String,
    val target: Int,
    val color: String
) {
    // 🔥 Streak Başarıları (Düzenlilik)
    STREAK_7_DAYS(
        "Ateşli Başlangıç",
        "7 gün üst üste hatırlatmalarını kaçırma",
        "🔥",
        7,
        "#FF5722"
    ),
    STREAK_30_DAYS(
        "Kararlı",
        "30 gün üst üste hatırlatmalarını kaçırma",
        "💪",
        30,
        "#FF9800"
    ),
    STREAK_100_DAYS(
        "Efsane",
        "100 gün üst üste hatırlatmalarını kaçırma",
        "🌟",
        100,
        "#FFC107"
    ),
    STREAK_365_DAYS(
        "Yılın Kralı",
        "365 gün üst üste hatırlatmalarını kaçırma",
        "👑",
        365,
        "#FFD700"
    ),

    // 🎯 Mükemmel Hafta/Ay (Düzenlilik)
    PERFECT_WEEK(
        "Mükemmel Hafta",
        "Bir hafta boyunca hiç hatırlatma kaçırma",
        "⭐",
        7,
        "#4CAF50"
    ),
    PERFECT_MONTH(
        "Mükemmel Ay",
        "Bir ay boyunca hiç hatırlatma kaçırma",
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
        "İlk hatırlatmana cevap ver",
        "✅",
        1,
        "#00BCD4"
    ),
    FIRST_BUDDY(
        "Badi Var",
        "İlk badini ekle",
        "👥",
        1,
        "#3F51B5"
    ),
    FIRST_PREMIUM(
        "Premium Üye",
        "Premium'a geç",
        "💎",
        1,
        "#9C27B0"
    ),

    // ⏰ Hatırlatma Kurulum Başarıları
    REMINDERS_5(
        "Düzenli Kullanıcı",
        "5 hatırlatma kur",
        "⏰",
        5,
        "#FF9800"
    ),
    REMINDERS_10(
        "Organize",
        "10 hatırlatma kur",
        "📋",
        10,
        "#795548"
    ),

    // 🚀 Hızlı Yanıt Başarıları
    QUICK_RESPONDER(
        "Hızlı Cevap",
        "10 hatırlatmayı eskalasyona gerek kalmadan al",
        "⚡",
        10,
        "#00BCD4"
    ),
    SUPER_QUICK_RESPONDER(
        "Şimşek Hızı",
        "50 hatırlatmayı eskalasyona gerek kalmadan al",
        "🚀",
        50,
        "#2196F3"
    ),

    // 👨‍👩‍👧‍👦 Aile/Sosyal Başarıları
    FAMILY_MEMBER(
        "Aile Üyesi",
        "Birinin badisi ol",
        "👨‍👩‍👧",
        1,
        "#E91E63"
    ),
    CARING_BUDDY(
        "İlgili Badi",
        "5 kez badine bildirim gönder",
        "💝",
        5,
        "#F44336"
    );

    fun getProgressPercentage(current: Int): Float {
        if (target == 0) return 0f
        return (current.toFloat() / target * 100).coerceIn(0f, 100f)
    }
}
