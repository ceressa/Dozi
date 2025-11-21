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
    val hourlyCompliance: Map<String, Float> = emptyMap(), // Saatlik uyumluluk ("0"-"23" -> 0-100)

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

/**
 * Note: Achievement model is defined in Achievement.kt
 * This file only contains UserStats and ComplianceTrend models
 */

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
