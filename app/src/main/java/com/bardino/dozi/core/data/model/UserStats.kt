package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * 📊 Kullanıcı istatistikleri ve gamification verileri
 */
data class UserStats(
    @DocumentId
    val id: String = "",
    val userId: String = "",

    // 🔥 Streak (düzenli kullanım)
    val currentStreak: Int = 0,          // Şu anki ardışık gün sayısı
    val longestStreak: Int = 0,          // En uzun ardışık gün sayısı
    val lastStreakDate: Timestamp? = null, // Son streak tarihi

    // 📈 Genel istatistikler
    val totalMedicationsTaken: Int = 0,   // Toplam alınan ilaç sayısı
    val totalMedicationsMissed: Int = 0,  // Toplam kaçırılan ilaç sayısı
    val totalMedicationsSkipped: Int = 0, // Toplam atlanan ilaç sayısı
    val complianceRate: Float = 0f,       // Uyumluluk oranı (0-100)

    // 💊 İlaç koleksiyonu
    val totalMedicines: Int = 0,          // Kullanıcının eklediği toplam ilaç sayısı
    val totalDosesTaken: Int = 0,         // Toplam alınan doz sayısı (totalMedicationsTaken ile aynı)

    // 🏆 Achievement rozetleri
    val achievements: List<String> = emptyList(), // Kazanılan achievement ID'leri

    // ⏰ En iyi/en kötü saatler
    val bestComplianceHour: Int = 9,      // En iyi uyumluluk saati (0-23)
    val worstComplianceHour: Int = 21,    // En kötü uyumluluk saati (0-23)
    val hourlyCompliance: Map<Int, Float> = emptyMap(), // Saatlik uyumluluk (0-23 -> 0-100)

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

/**
 * 🏆 Achievement (başarı rozeti)
 */
data class Achievement(
    val id: String,                      // "first_week", "30_days", etc.
    val title: String,                   // "İlk Hafta"
    val description: String,             // "7 gün üst üste ilaç aldın!"
    val icon: String,                    // "🏅", "🎖️", "👑"
    val requirement: AchievementRequirement,
    val unlockedAt: Timestamp? = null    // Null = henüz kazanılmadı
)

/**
 * Achievement gereksinimleri
 */
sealed class AchievementRequirement {
    data class StreakDays(val days: Int) : AchievementRequirement()
    data class TotalMedications(val count: Int) : AchievementRequirement()
    data class ComplianceRate(val rate: Float) : AchievementRequirement() // 0-100
    data class ConsecutivePerfectDays(val days: Int) : AchievementRequirement()
}

/**
 * Predefined achievements
 */
object Achievements {
    val FIRST_WEEK = Achievement(
        id = "first_week",
        title = "İlk Hafta",
        description = "7 gün üst üste ilaçlarını aldın!",
        icon = "🏅",
        requirement = AchievementRequirement.StreakDays(7)
    )

    val THIRTY_DAYS = Achievement(
        id = "30_days",
        title = "Bir Ay",
        description = "30 gün üst üste düzenli kullanım!",
        icon = "🎖️",
        requirement = AchievementRequirement.StreakDays(30)
    )

    val PERFECT_MONTH = Achievement(
        id = "perfect_month",
        title = "Mükemmel Ay",
        description = "Bir ay boyunca %100 uyumluluk!",
        icon = "👑",
        requirement = AchievementRequirement.ComplianceRate(100f)
    )

    val HUNDRED_MEDS = Achievement(
        id = "hundred_meds",
        title = "Yüzlük Kulüp",
        description = "100 ilaç aldın!",
        icon = "💯",
        requirement = AchievementRequirement.TotalMedications(100)
    )

    val YEAR_STREAK = Achievement(
        id = "year_streak",
        title = "Bir Yıl",
        description = "365 gün üst üste! İnanılmaz!",
        icon = "🏆",
        requirement = AchievementRequirement.StreakDays(365)
    )

    val ALL = listOf(
        FIRST_WEEK,
        THIRTY_DAYS,
        PERFECT_MONTH,
        HUNDRED_MEDS,
        YEAR_STREAK
    )
}

/**
 * 📊 Uyumluluk trend verisi (grafikler için)
 */
data class ComplianceTrend(
    val date: String,                    // "2025-11-14"
    val complianceRate: Float,           // 0-100
    val takenCount: Int,
    val totalCount: Int,
    val missedCount: Int
)

/**
 * 📈 30 günlük trend hesapla
 */
fun List<DailyMedicationLogs>.toComplianceTrend(): List<ComplianceTrend> {
    return this.map { daily ->
        ComplianceTrend(
            date = daily.date,
            complianceRate = daily.completionRate * 100,
            takenCount = daily.takenCount,
            totalCount = daily.totalCount,
            missedCount = daily.missedCount
        )
    }
}
