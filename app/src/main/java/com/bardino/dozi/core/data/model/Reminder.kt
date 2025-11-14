package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * İlaç hatırlatmasını temsil eder
 */
data class Reminder(
    @DocumentId
    val id: String = "",
    val userId: String = "",                    // Hatırlatma sahibi
    val medicineId: String = "",                // İlaç ID (local DB'den)
    val medicineName: String = "",
    val dosage: String = "",
    val frequency: ReminderFrequency = ReminderFrequency.DAILY,
    val times: List<String> = emptyList(),      // ["08:00", "20:00"]
    val days: List<Int>? = null,                // Haftalık için: [1,2,3,4,5] (Pazartesi=1)
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val isActive: Boolean = true,
    val isMuted: Boolean = false,               // Geçici olarak sessize alınmış
    val reminderSound: String = "default",
    val vibrationPattern: String = "default",
    val notes: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val sharedWithBuddies: List<String> = emptyList() // Paylaşılan buddy userId'leri
)

/**
 * Hatırlatma sıklığı
 */
enum class ReminderFrequency {
    DAILY,          // Her gün
    WEEKLY,         // Haftanın belirli günleri
    AS_NEEDED,      // Gerektiğinde
    CUSTOM          // Özel
}

/**
 * Hatırlatma ile birlikte ilaç bilgilerini içeren model
 * (UI'da göstermek için)
 */
data class ReminderWithMedicine(
    val reminder: Reminder,
    val medicine: Medicine? = null
)

/**
 * Günlük hatırlatma görünümü
 * Belirli bir gün için tüm hatırlatmaları gruplamak için
 */
data class DailyReminderGroup(
    val date: String,                           // "2025-11-14"
    val reminders: List<ReminderWithTime>
)

/**
 * Zaman ile hatırlatma
 */
data class ReminderWithTime(
    val reminder: Reminder,
    val time: String,                           // "08:00"
    val isPast: Boolean = false,
    val isTaken: Boolean = false
)
