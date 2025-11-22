package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

/**
 * İlaç alma geçmişini temsil eder
 *
 * Firestore'da status STRING olarak saklanır (örn: "TAKEN").
 * Uygulama içinde ENUM gibi kullanmak için statusEnum getter'ı tanımlıdır.
 */
data class MedicationLog(
    @DocumentId
    val id: String = "",

    val userId: String = "",            // İlaç alan kullanıcı
    val medicineId: String = "",        // İlaç ID
    val medicineName: String = "",
    val dosage: String = "",

    val scheduledTime: Timestamp? = null,   // Planlanmış zaman
    val takenAt: Timestamp? = null,         // Gerçek alma zamanı

    // Firestore STRING field (ENUM değil!)
    val status: String = "PENDING",

    val notes: String? = null,
    val sideEffects: List<String> = emptyList(),
    val mood: String? = null,

    val location: GeoPoint? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,

    // Timestamp değil Long (GenerateInsights / WeeklyReport için gerekli)
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) {
    /**
     * Uygulama içinde ENUM olarak kullanılır.
     *
     * Örnek:
     * log.statusEnum == MedicationStatus.TAKEN
     */
    val statusEnum: MedicationStatus
        get() = MedicationStatus.from(status)
}

/**
 * ENUM – uygulamanın geri kalanı hâlâ ENUM ile çalışıyor
 */
enum class MedicationStatus {
    PENDING,
    TAKEN,
    SKIPPED,
    MISSED,
    SNOOZED;

    companion object {
        fun from(value: String?): MedicationStatus {
            return entries.firstOrNull { it.name == value } ?: PENDING
        }
    }
}

/**
 * İlaç alma durumu için Türkçe isimler
 */
fun MedicationStatus.toTurkish(): String = when (this) {
    MedicationStatus.PENDING -> "Beklemede"
    MedicationStatus.TAKEN -> "Alındı"
    MedicationStatus.SKIPPED -> "Atlandı"
    MedicationStatus.MISSED -> "Kaçırıldı"
    MedicationStatus.SNOOZED -> "Ertelendi"
}

/**
 * İlaç alma durumu için emoji
 */
fun MedicationStatus.toEmoji(): String = when (this) {
    MedicationStatus.PENDING -> "⏳"
    MedicationStatus.TAKEN -> "✅"
    MedicationStatus.SKIPPED -> "⏭️"
    MedicationStatus.MISSED -> "❌"
    MedicationStatus.SNOOZED -> "⏰"
}

/**
 * İlaç log'u ile birlikte ilaç bilgilerini içeren model
 */
data class MedicationLogWithMedicine(
    val log: MedicationLog,
    val medicine: Medicine
)

/**
 * Günlük ilaç geçmişi
 */
data class DailyMedicationLogs(
    val date: String,               // "2025-11-14"
    val logs: List<MedicationLog>,
    val takenCount: Int,
    val missedCount: Int,
    val skippedCount: Int,
    val totalCount: Int
) {
    val completionRate: Float
        get() = if (totalCount > 0) takenCount.toFloat() / totalCount else 0f
}
