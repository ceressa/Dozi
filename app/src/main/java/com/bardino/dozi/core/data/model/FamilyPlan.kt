package com.bardino.dozi.core.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Aile Paketi Modeli
 *
 * Ana hesap (organizer) bir aile planı oluşturur ve en fazla 3 kişiyi davet edebilir.
 * Tüm aile üyeleri premium özelliklerden yararlanır.
 */
data class FamilyPlan(
    @DocumentId
    val id: String = "",

    // Organizatör (Plan Sahibi) Bilgileri
    val organizerId: String = "",           // Ana hesabın userId
    val organizerEmail: String = "",
    val organizerName: String = "",

    // Plan Detayları
    val planType: String = "FAMILY_PREMIUM", // Aile premium planı
    val maxMembers: Int = 3,                 // Maksimum 3 üye
    val currentMembers: List<String> = emptyList(), // Üye userId'leri

    // Davet Kodu
    val invitationCode: String = "",         // 6 haneli benzersiz kod

    // Durum
    val status: FamilyPlanStatus = FamilyPlanStatus.ACTIVE,

    // Tarihler
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,        // Premium bitiş tarihi
    val updatedAt: Timestamp? = null
) {
    /**
     * Aile planında yer var mı?
     */
    fun hasAvailableSlots(): Boolean {
        return currentMembers.size < maxMembers
    }

    /**
     * Kullanıcı bu aile planının üyesi mi?
     */
    fun isMember(userId: String): Boolean {
        return userId == organizerId || currentMembers.contains(userId)
    }

    /**
     * Plan aktif mi?
     */
    fun isActive(): Boolean {
        return status == FamilyPlanStatus.ACTIVE &&
               (expiresAt == null || expiresAt.toDate().time > System.currentTimeMillis())
    }
}

/**
 * Aile Planı Durumu
 */
enum class FamilyPlanStatus {
    ACTIVE,      // Aktif
    CANCELLED,   // İptal edildi
    EXPIRED      // Süresi doldu
}

/**
 * Kullanıcının aile planındaki rolü
 */
enum class FamilyRole {
    ORGANIZER,   // Plan sahibi (ana hesap)
    MEMBER       // Aile üyesi
}
