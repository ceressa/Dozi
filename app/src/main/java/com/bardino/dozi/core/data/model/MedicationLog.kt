package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp

/**
 * İlaç alma geçmişini temsil eder
 */
data class MedicationLog(
    @DocumentId
    val id: String = "",
    val userId: String = "",                    // İlaç alan kullanıcı
    val medicineId: String = "",                // İlaç ID
    val medicineName: String = "",
    val dosage: String = "",
    val scheduledTime: Timestamp? = null,       // Planlanmış zaman
    val takenAt: Timestamp? = null,             // Gerçek alma zamanı
    val status: MedicationStatus = MedicationStatus.PENDING,
    val notes: String? = null,
    val sideEffects: List<String> = emptyList(), // Yan etkiler
    val mood: String? = null,                   // Ruh hali
    val location: GeoPoint? = null,             // Konum
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

/**
 * İlaç alma durumu
 */
enum class MedicationStatus {
    PENDING,    // Beklemede (henüz zaman gelmedi)
    TAKEN,      // Alındı
    SKIPPED,    // Atlandı (kullanıcı atlayı dedi)
    MISSED,     // Kaçırıldı (zaman geçti, alınmadı)
    SNOOZED     // Ertelendi
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
    val date: String,                           // "2025-11-14"
    val logs: List<MedicationLog>,
    val takenCount: Int,
    val missedCount: Int,
    val skippedCount: Int,
    val totalCount: Int
) {
    val completionRate: Float
        get() = if (totalCount > 0) takenCount.toFloat() / totalCount else 0f
}
