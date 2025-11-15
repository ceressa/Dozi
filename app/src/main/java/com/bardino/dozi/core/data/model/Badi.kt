package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Kullanıcılar arası badi ilişkisini temsil eder
 */
data class Badi(
    @DocumentId
    val id: String = "",
    val userId: String = "",                    // İsteği gönderen/oluşturan kullanıcı
    val buddyUserId: String = "",               // Badi olan kullanıcı
    val status: BadiStatus = BadiStatus.ACTIVE,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val nickname: String? = null,                // Badi için özel isim
    val permissions: BadiPermissions = BadiPermissions(),
    val notificationPreferences: BadiNotificationPreferences = BadiNotificationPreferences(),
    @ServerTimestamp
    val lastInteraction: Timestamp? = null
)

/**
 * Badi rolleri
 */
enum class BadiRole {
    VIEWER,      // Sadece görüntüleme
    HELPER,      // İlaç aldı işaretleyebilir
    CAREGIVER,   // İlaç ekle/düzenle
    ADMIN        // Tüm yetkiler
}

/**
 * Badi izinleri
 */
data class BadiPermissions(
    val role: BadiRole = BadiRole.VIEWER,       // Badi rolü
    val canViewReminders: Boolean = true,         // Hatırlatmaları görüntüleyebilir
    val canReceiveNotifications: Boolean = true,  // Bildirim alabilir
    val canMarkAsTaken: Boolean = false,          // İlaç aldı işaretleyebilir
    val canEditReminders: Boolean = false,        // Hatırlatmaları düzenleyebilir
    val canAddMedicine: Boolean = false,          // Yeni ilaç ekleyebilir
    val canDeleteMedicine: Boolean = false,       // İlaç silebilir
    val canViewMedicationHistory: Boolean = true, // İlaç geçmişini görüntüleyebilir
    val canManageBadis: Boolean = false         // Diğer badileri yönetebilir
) {
    companion object {
        /**
         * Rol bazlı izin seti döndür
         */
        fun fromRole(role: BadiRole): BadiPermissions {
            return when (role) {
                BadiRole.VIEWER -> BadiPermissions(
                    role = role,
                    canViewReminders = true,
                    canReceiveNotifications = true,
                    canMarkAsTaken = false,
                    canEditReminders = false,
                    canAddMedicine = false,
                    canDeleteMedicine = false,
                    canViewMedicationHistory = true,
                    canManageBadis = false
                )
                BadiRole.HELPER -> BadiPermissions(
                    role = role,
                    canViewReminders = true,
                    canReceiveNotifications = true,
                    canMarkAsTaken = true,          // ✅ İlaç aldı işaretleyebilir
                    canEditReminders = false,
                    canAddMedicine = false,
                    canDeleteMedicine = false,
                    canViewMedicationHistory = true,
                    canManageBadis = false
                )
                BadiRole.CAREGIVER -> BadiPermissions(
                    role = role,
                    canViewReminders = true,
                    canReceiveNotifications = true,
                    canMarkAsTaken = true,
                    canEditReminders = true,        // ✅ İlaç düzenleyebilir
                    canAddMedicine = true,          // ✅ İlaç ekleyebilir
                    canDeleteMedicine = false,
                    canViewMedicationHistory = true,
                    canManageBadis = false
                )
                BadiRole.ADMIN -> BadiPermissions(
                    role = role,
                    canViewReminders = true,
                    canReceiveNotifications = true,
                    canMarkAsTaken = true,
                    canEditReminders = true,
                    canAddMedicine = true,
                    canDeleteMedicine = true,       // ✅ İlaç silebilir
                    canViewMedicationHistory = true,
                    canManageBadis = true         // ✅ Badileri yönetebilir
                )
            }
        }
    }
}

/**
 * Badi rolü için Türkçe isimler
 */
fun BadiRole.toTurkish(): String = when (this) {
    BadiRole.VIEWER -> "İzleyici"
    BadiRole.HELPER -> "Yardımcı"
    BadiRole.CAREGIVER -> "Bakıcı"
    BadiRole.ADMIN -> "Yönetici"
}

/**
 * Badi rolü için açıklama
 */
fun BadiRole.toDescription(): String = when (this) {
    BadiRole.VIEWER -> "Sadece ilaç hatırlatmalarını ve geçmişi görüntüleyebilir"
    BadiRole.HELPER -> "İlaç alındı olarak işaretleyebilir"
    BadiRole.CAREGIVER -> "İlaç ekleyebilir ve düzenleyebilir"
    BadiRole.ADMIN -> "Tüm yetkiler (ilaç ekleme, silme, badi yönetimi)"
}

/**
 * Badi bildirim tercihleri
 */
data class BadiNotificationPreferences(
    val onMedicationTime: Boolean = true,        // İlaç zamanı geldiğinde bildir
    val onMedicationTaken: Boolean = true,       // İlaç alındığında bildir
    val onMedicationSkipped: Boolean = true,     // İlaç atlandığında bildir
    val onMedicationMissed: Boolean = true       // İlaç kaçırıldığında bildir
)

/**
 * Badi durumu
 */
enum class BadiStatus {
    ACTIVE,      // Aktif badi ilişkisi
    PAUSED,      // Geçici olarak duraklatılmış
    REMOVED      // Kaldırılmış
}

/**
 * Badi ile birlikte kullanıcı bilgilerini içeren model
 * (UI'da göstermek için)
 */
data class BadiWithUser(
    val badi: Badi,
    val user: User
)
