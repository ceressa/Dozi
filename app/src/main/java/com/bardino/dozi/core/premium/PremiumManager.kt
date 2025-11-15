package com.bardino.dozi.core.premium

import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.PremiumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🌟 Premium Manager
 *
 * Uygulama genelinde premium durumunu kontrol etmek için kullanılır.
 * Feature gating (özellik kısıtlama) için merkezi yönetim sağlar.
 */
@Singleton
class PremiumManager @Inject constructor(
    private val premiumRepository: PremiumRepository
) {
    /**
     * Real-time premium durumunu Flow olarak döndürür
     */
    fun isPremiumFlow(): Flow<Boolean> {
        return premiumRepository.observePremiumStatus()
            .map { user ->
                user?.isCurrentlyPremium() ?: false
            }
    }

    /**
     * Senkron olarak premium durumunu kontrol eder
     */
    suspend fun isPremium(): Boolean {
        val user = premiumRepository.getCurrentPremiumStatus()
        return user?.isCurrentlyPremium() ?: false
    }

    /**
     * Kullanıcı trial sürecinde mi?
     */
    suspend fun isTrial(): Boolean {
        val user = premiumRepository.getCurrentPremiumStatus()
        return user?.isTrial == true && user.isCurrentlyPremium()
    }

    /**
     * Kullanıcı banlandı mı?
     */
    suspend fun isBanned(): Boolean {
        val user = premiumRepository.getCurrentPremiumStatus()
        return user?.isBanned == true
    }

    /**
     * Premium kalan gün sayısı
     */
    suspend fun daysRemaining(): Int {
        val user = premiumRepository.getCurrentPremiumStatus()
        return user?.premiumDaysRemaining() ?: 0
    }

    /**
     * Kullanıcının planını al
     */
    suspend fun getPlanType(): String {
        val user = premiumRepository.getCurrentPremiumStatus()
        return user?.planType ?: "free"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 FEATURE GATES (Özellik Kısıtlamaları)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Özel bildirim sesi kullanabilir mi?
     */
    suspend fun canUseCustomNotificationSound(): Boolean = isPremium()

    /**
     * Gelişmiş istatistikler görebilir mi?
     */
    suspend fun canViewAdvancedStats(): Boolean = isPremium()

    /**
     * Bulut yedekleme kullanabilir mi?
     */
    suspend fun canUseCloudBackup(): Boolean = isPremium()

    /**
     * Akıllı hatırlatmalar kullanabilir mi?
     */
    suspend fun canUseSmartReminders(): Boolean = isPremium()

    /**
     * Aile takibi kullanabilir mi?
     */
    suspend fun canUseFamilyTracking(): Boolean = isPremium()

    /**
     * Öncelikli destek alabilir mi?
     */
    suspend fun hasPrioritySupport(): Boolean = isPremium()

    /**
     * Premium özelliğe erişimi kontrol et
     * Premium değilse false döndürür
     */
    suspend fun checkPremiumFeature(feature: PremiumFeature): Boolean {
        if (isBanned()) return false

        return when (feature) {
            PremiumFeature.CUSTOM_NOTIFICATION_SOUND -> canUseCustomNotificationSound()
            PremiumFeature.ADVANCED_STATS -> canViewAdvancedStats()
            PremiumFeature.CLOUD_BACKUP -> canUseCloudBackup()
            PremiumFeature.SMART_REMINDERS -> canUseSmartReminders()
            PremiumFeature.FAMILY_TRACKING -> canUseFamilyTracking()
            PremiumFeature.PRIORITY_SUPPORT -> hasPrioritySupport()
        }
    }

    /**
     * Premium özellik için kullanıcıya mesaj göster
     */
    fun getPremiumFeatureMessage(feature: PremiumFeature): String {
        return when (feature) {
            PremiumFeature.CUSTOM_NOTIFICATION_SOUND ->
                "Özel bildirim sesleri Dozi Ekstra'ya özeldir"

            PremiumFeature.ADVANCED_STATS ->
                "Gelişmiş istatistikler için Dozi Ekstra'ya katıl"

            PremiumFeature.CLOUD_BACKUP ->
                "Bulut yedekleme özelliği Dozi Ekstra'da"

            PremiumFeature.SMART_REMINDERS ->
                "Akıllı hatırlatmalar için Dozi Ekstra'ya geç"

            PremiumFeature.FAMILY_TRACKING ->
                "Aile takibi Dozi Ekstra ile mümkün"

            PremiumFeature.PRIORITY_SUPPORT ->
                "Öncelikli destek Dozi Ekstra üyelerine özeldir"
        }
    }
}

/**
 * 💎 Premium Özellikler Enum
 */
enum class PremiumFeature(val displayName: String) {
    CUSTOM_NOTIFICATION_SOUND("Özel Bildirim Sesi"),
    ADVANCED_STATS("Gelişmiş İstatistikler"),
    CLOUD_BACKUP("Bulut Yedekleme"),
    SMART_REMINDERS("Akıllı Hatırlatmalar"),
    FAMILY_TRACKING("Aile Takibi"),
    PRIORITY_SUPPORT("Öncelikli Destek")
}
