package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * 📊 Kullanıcı istatistikleri ve gamification verileri
 *
 * Basitleştirilmiş versiyon - sadece streak ve temel sayaçlar
 */
data class UserStats(
    @DocumentId
    val id: String = "",
    val userId: String = "",

    // 🔥 Streak (düzenli kullanım)
    val currentStreak: Int = 0,              // Şu anki ardışık gün sayısı
    val longestStreak: Int = 0,              // En uzun ardışık gün sayısı
    val lastStreakDate: Timestamp? = null,   // Son streak tarihi

    // 📈 Temel sayaçlar
    val totalMedicationsTaken: Int = 0,      // Toplam alınan hatırlatma sayısı
    val totalMedicationsMissed: Int = 0,     // Toplam kaçırılan hatırlatma sayısı
    val totalMedicationsSkipped: Int = 0,    // Toplam atlanan hatırlatma sayısı

    // ⚡ Hızlı yanıt sayacı (eskalasyonsuz alınan)
    val quickResponseCount: Int = 0,         // Eskalasyona gerek kalmadan alınan sayısı

    // 👥 Sosyal sayaçlar
    val buddyCount: Int = 0,                 // Eklenen badi sayısı
    val buddyNotificationsSent: Int = 0,     // Gönderilen badi bildirimi sayısı

    // 🏆 Achievement rozetleri
    val achievements: List<String> = emptyList(), // Kazanılan achievement ID'leri

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

/**
 * 📊 Günlük özet verisi (grafikler için)
 */
data class DailySummary(
    val date: String,                    // "2025-11-14"
    val takenCount: Int,
    val totalCount: Int,
    val missedCount: Int
) {
    val completionRate: Float
        get() = if (totalCount > 0) takenCount.toFloat() / totalCount else 0f
}
