package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Buddy isteğini temsil eder
 */
data class BuddyRequest(
    @DocumentId
    val id: String = "",
    val fromUserId: String = "",              // İsteği gönderen
    val toUserId: String = "",                // İsteği alan
    val fromUserName: String = "",            // Gönderenin adı (bildirim için)
    val fromUserPhoto: String = "",           // Gönderenin fotoğrafı
    val toUserEmail: String? = null,          // Email ile gönderildiyse
    val toBuddyCode: String? = null,          // Kod ile gönderildiyse
    val status: BuddyRequestStatus = BuddyRequestStatus.PENDING,
    val message: String? = null,              // Özel mesaj
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,         // 7 gün sonra otomatik iptal
    @ServerTimestamp
    val respondedAt: Timestamp? = null
)

/**
 * Buddy isteği durumu
 */
enum class BuddyRequestStatus {
    PENDING,    // Beklemede
    ACCEPTED,   // Kabul edildi
    REJECTED,   // Reddedildi
    EXPIRED     // Süresi doldu
}

/**
 * Buddy isteği ile birlikte kullanıcı bilgilerini içeren model
 * (UI'da göstermek için)
 */
data class BuddyRequestWithUser(
    val request: BuddyRequest,
    val fromUser: User
)
