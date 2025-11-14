package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Kullanıcılar arası buddy ilişkisini temsil eder
 */
data class Buddy(
    @DocumentId
    val id: String = "",
    val userId: String = "",                    // İsteği gönderen/oluşturan kullanıcı
    val buddyUserId: String = "",               // Buddy olan kullanıcı
    val status: BuddyStatus = BuddyStatus.ACTIVE,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val nickname: String? = null,                // Buddy için özel isim
    val permissions: BuddyPermissions = BuddyPermissions(),
    val notificationPreferences: BuddyNotificationPreferences = BuddyNotificationPreferences(),
    @ServerTimestamp
    val lastInteraction: Timestamp? = null
)

/**
 * Buddy izinleri
 */
data class BuddyPermissions(
    val canViewReminders: Boolean = true,        // Hatırlatmaları görüntüleyebilir
    val canReceiveNotifications: Boolean = true, // Bildirim alabilir
    val canEditReminders: Boolean = false,       // Hatırlatmaları düzenleyebilir
    val canViewMedicationHistory: Boolean = true // İlaç geçmişini görüntüleyebilir
)

/**
 * Buddy bildirim tercihleri
 */
data class BuddyNotificationPreferences(
    val onMedicationTime: Boolean = true,        // İlaç zamanı geldiğinde bildir
    val onMedicationTaken: Boolean = true,       // İlaç alındığında bildir
    val onMedicationSkipped: Boolean = true,     // İlaç atlandığında bildir
    val onMedicationMissed: Boolean = true       // İlaç kaçırıldığında bildir
)

/**
 * Buddy durumu
 */
enum class BuddyStatus {
    ACTIVE,      // Aktif buddy ilişkisi
    PAUSED,      // Geçici olarak duraklatılmış
    REMOVED      // Kaldırılmış
}

/**
 * Buddy ile birlikte kullanıcı bilgilerini içeren model
 * (UI'da göstermek için)
 */
data class BuddyWithUser(
    val buddy: Buddy,
    val user: User
)
