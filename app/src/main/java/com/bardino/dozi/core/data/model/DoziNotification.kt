package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Dozi uygulaması bildirimleri
 * Hem app içi hem de push notification için kullanılır
 */
data class DoziNotification(
    @DocumentId
    val id: String = "",
    val userId: String = "",                    // Bildirimi alan kullanıcı
    val type: NotificationType = NotificationType.GENERAL,
    val title: String = "",
    val body: String = "",
    val data: Map<String, String> = emptyMap(), // Ekstra veriler
    val isRead: Boolean = false,
    val isSent: Boolean = false,                // Push notification gönderildi mi?
    @ServerTimestamp
    val sentAt: Timestamp? = null,
    @ServerTimestamp
    val readAt: Timestamp? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val actionUrl: String? = null,              // Deep link
    val imageUrl: String? = null                // Bildirim görseli
)

/**
 * Bildirim türü
 */
enum class NotificationType {
    GENERAL,                        // Genel bildirim
    BUDDY_REQUEST,                  // Buddy isteği
    BUDDY_ACCEPTED,                 // Buddy isteği kabul edildi
    BUDDY_REJECTED,                 // Buddy isteği reddedildi
    MEDICATION_REMINDER,            // İlaç hatırlatması
    BUDDY_MEDICATION_ALERT,         // Buddy ilaç uyarısı
    MEDICATION_TAKEN,               // İlaç alındı bildirimi
    MEDICATION_MISSED,              // İlaç kaçırıldı uyarısı
    CRITICAL_MEDICATION_MISSED,     // 🚨 Kritik ilaç kaçırıldı (escalation)
    STOCK_LOW,                      // Stok azaldı
    STOCK_EMPTY,                    // Stok bitti
    SYSTEM                          // Sistem bildirimi
}

/**
 * Bildirim önceliği
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH
}

/**
 * Bildirim türü için Türkçe isimler
 */
fun NotificationType.toTurkish(): String = when (this) {
    NotificationType.GENERAL -> "Genel"
    NotificationType.BUDDY_REQUEST -> "Buddy İsteği"
    NotificationType.BUDDY_ACCEPTED -> "Buddy Kabul Edildi"
    NotificationType.BUDDY_REJECTED -> "Buddy Reddedildi"
    NotificationType.MEDICATION_REMINDER -> "İlaç Hatırlatması"
    NotificationType.BUDDY_MEDICATION_ALERT -> "Buddy İlaç Uyarısı"
    NotificationType.MEDICATION_TAKEN -> "İlaç Alındı"
    NotificationType.MEDICATION_MISSED -> "İlaç Kaçırıldı"
    NotificationType.CRITICAL_MEDICATION_MISSED -> "Kritik İlaç Kaçırıldı"
    NotificationType.STOCK_LOW -> "Stok Azaldı"
    NotificationType.STOCK_EMPTY -> "Stok Bitti"
    NotificationType.SYSTEM -> "Sistem"
}

/**
 * Bildirim türü için emoji
 */
fun NotificationType.toEmoji(): String = when (this) {
    NotificationType.GENERAL -> "📢"
    NotificationType.BUDDY_REQUEST -> "🤝"
    NotificationType.BUDDY_ACCEPTED -> "✅"
    NotificationType.BUDDY_REJECTED -> "❌"
    NotificationType.MEDICATION_REMINDER -> "💊"
    NotificationType.BUDDY_MEDICATION_ALERT -> "⚠️"
    NotificationType.MEDICATION_TAKEN -> "✅"
    NotificationType.MEDICATION_MISSED -> "❌"
    NotificationType.CRITICAL_MEDICATION_MISSED -> "🚨"
    NotificationType.STOCK_LOW -> "📉"
    NotificationType.STOCK_EMPTY -> "🚫"
    NotificationType.SYSTEM -> "⚙️"
}
