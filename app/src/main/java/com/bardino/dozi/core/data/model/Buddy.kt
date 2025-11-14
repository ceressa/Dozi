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
 * Buddy rolleri
 */
enum class BuddyRole {
    VIEWER,      // Sadece görüntüleme
    HELPER,      // İlaç aldı işaretleyebilir
    CAREGIVER,   // İlaç ekle/düzenle
    ADMIN        // Tüm yetkiler
}

/**
 * Buddy izinleri
 */
data class BuddyPermissions(
    val role: BuddyRole = BuddyRole.VIEWER,       // Buddy rolü
    val canViewReminders: Boolean = true,         // Hatırlatmaları görüntüleyebilir
    val canReceiveNotifications: Boolean = true,  // Bildirim alabilir
    val canMarkAsTaken: Boolean = false,          // İlaç aldı işaretleyebilir
    val canEditReminders: Boolean = false,        // Hatırlatmaları düzenleyebilir
    val canAddMedicine: Boolean = false,          // Yeni ilaç ekleyebilir
    val canDeleteMedicine: Boolean = false,       // İlaç silebilir
    val canViewMedicationHistory: Boolean = true, // İlaç geçmişini görüntüleyebilir
    val canManageBuddies: Boolean = false         // Diğer buddy'leri yönetebilir
) {
    companion object {
        /**
         * Rol bazlı izin seti döndür
         */
        fun fromRole(role: BuddyRole): BuddyPermissions {
            return when (role) {
                BuddyRole.VIEWER -> BuddyPermissions(
                    role = role,
                    canViewReminders = true,
                    canReceiveNotifications = true,
                    canMarkAsTaken = false,
                    canEditReminders = false,
                    canAddMedicine = false,
                    canDeleteMedicine = false,
                    canViewMedicationHistory = true,
                    canManageBuddies = false
                )
                BuddyRole.HELPER -> BuddyPermissions(
                    role = role,
                    canViewReminders = true,
                    canReceiveNotifications = true,
                    canMarkAsTaken = true,          // ✅ İlaç aldı işaretleyebilir
                    canEditReminders = false,
                    canAddMedicine = false,
                    canDeleteMedicine = false,
                    canViewMedicationHistory = true,
                    canManageBuddies = false
                )
                BuddyRole.CAREGIVER -> BuddyPermissions(
                    role = role,
                    canViewReminders = true,
                    canReceiveNotifications = true,
                    canMarkAsTaken = true,
                    canEditReminders = true,        // ✅ İlaç düzenleyebilir
                    canAddMedicine = true,          // ✅ İlaç ekleyebilir
                    canDeleteMedicine = false,
                    canViewMedicationHistory = true,
                    canManageBuddies = false
                )
                BuddyRole.ADMIN -> BuddyPermissions(
                    role = role,
                    canViewReminders = true,
                    canReceiveNotifications = true,
                    canMarkAsTaken = true,
                    canEditReminders = true,
                    canAddMedicine = true,
                    canDeleteMedicine = true,       // ✅ İlaç silebilir
                    canViewMedicationHistory = true,
                    canManageBuddies = true         // ✅ Buddy'leri yönetebilir
                )
            }
        }
    }
}

/**
 * Buddy rolü için Türkçe isimler
 */
fun BuddyRole.toTurkish(): String = when (this) {
    BuddyRole.VIEWER -> "İzleyici"
    BuddyRole.HELPER -> "Yardımcı"
    BuddyRole.CAREGIVER -> "Bakıcı"
    BuddyRole.ADMIN -> "Yönetici"
}

/**
 * Buddy rolü için açıklama
 */
fun BuddyRole.toDescription(): String = when (this) {
    BuddyRole.VIEWER -> "Sadece ilaç hatırlatmalarını ve geçmişi görüntüleyebilir"
    BuddyRole.HELPER -> "İlaç alındı olarak işaretleyebilir"
    BuddyRole.CAREGIVER -> "İlaç ekleyebilir ve düzenleyebilir"
    BuddyRole.ADMIN -> "Tüm yetkiler (ilaç ekleme, silme, buddy yönetimi)"
}

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
