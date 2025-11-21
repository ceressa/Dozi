package com.bardino.dozi.core.premium

import com.bardino.dozi.core.common.Constants
import com.bardino.dozi.core.data.model.PlanCategory
import com.bardino.dozi.core.data.model.PremiumPlanType
import com.bardino.dozi.core.data.repository.PremiumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🌟 Premium Manager
 *
 * Uygulama genelinde premium durumunu ve plan kategorisini kontrol eder.
 * Feature gating (özellik kısıtlama) için merkezi yönetim sağlar.
 */
@Singleton
class PremiumManager @Inject constructor(
    private val premiumRepository: PremiumRepository
) {
    // ═══════════════════════════════════════════════════════════════════════
    // 📊 PLAN DURUMU KONTROLÜ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Real-time premium durumunu Flow olarak döndürür
     */
    fun isPremiumFlow(): Flow<Boolean> {
        return premiumRepository.observePremiumStatus()
            .map { user ->
                user?.premiumStatus()?.isActive ?: false
            }
    }

    /**
     * Plan kategorisini Flow olarak döndürür
     */
    fun planCategoryFlow(): Flow<PlanCategory> {
        return premiumRepository.observePremiumStatus()
            .map { user ->
                user?.premiumStatus()?.planType?.category ?: PlanCategory.FREE
            }
    }

    /**
     * Senkron olarak premium durumunu kontrol eder
     */
    suspend fun isPremium(): Boolean {
        val user = premiumRepository.getCurrentPremiumStatus()
        return user?.premiumStatus()?.isActive ?: false
    }

    /**
     * Plan kategorisini döndürür
     */
    suspend fun getPlanCategory(): PlanCategory {
        val user = premiumRepository.getCurrentPremiumStatus()
        return user?.premiumStatus()?.planType?.category ?: PlanCategory.FREE
    }

    /**
     * Plan tipini döndürür
     */
    suspend fun getPlanType(): PremiumPlanType {
        val user = premiumRepository.getCurrentPremiumStatus()
        return user?.premiumStatus()?.planType ?: PremiumPlanType.FREE
    }

    /**
     * Kullanıcı trial sürecinde mi?
     */
    suspend fun isTrial(): Boolean {
        val user = premiumRepository.getCurrentPremiumStatus()
        return user?.premiumStatus()?.isTrial == true
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
        return user?.premiumStatus()?.daysRemaining() ?: 0
    }

    /**
     * Ekstra veya üstü mü?
     */
    suspend fun isEkstraOrAbove(): Boolean {
        val category = getPlanCategory()
        return category == PlanCategory.EKSTRA || category == PlanCategory.AILE
    }

    /**
     * Aile planı mı?
     */
    suspend fun isAile(): Boolean {
        return getPlanCategory() == PlanCategory.AILE
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📏 LİMİT KONTROL METODLARİ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * İlaç limitini döndürür
     * -1 = sınırsız
     */
    suspend fun getMedicineLimit(): Int {
        return when (getPlanCategory()) {
            PlanCategory.FREE -> Constants.FREE_MEDICINE_LIMIT
            PlanCategory.EKSTRA, PlanCategory.AILE -> Constants.UNLIMITED
        }
    }

    /**
     * Hatırlatma limitini döndürür (time slot bazlı)
     * -1 = sınırsız
     */
    suspend fun getReminderLimit(): Int {
        return when (getPlanCategory()) {
            PlanCategory.FREE -> Constants.FREE_REMINDER_LIMIT
            PlanCategory.EKSTRA, PlanCategory.AILE -> Constants.UNLIMITED
        }
    }

    /**
     * Badi limitini döndürür
     * -1 = sınırsız
     */
    suspend fun getBadiLimit(): Int {
        return when (getPlanCategory()) {
            PlanCategory.FREE -> 0
            PlanCategory.EKSTRA -> Constants.EKSTRA_BADI_LIMIT
            PlanCategory.AILE -> Constants.UNLIMITED
        }
    }

    /**
     * Daha fazla ilaç ekleyebilir mi?
     */
    suspend fun canAddMoreMedicines(currentCount: Int): Boolean {
        val limit = getMedicineLimit()
        return limit == Constants.UNLIMITED || currentCount < limit
    }

    /**
     * Daha fazla hatırlatma ekleyebilir mi?
     */
    suspend fun canAddMoreReminders(currentCount: Int): Boolean {
        val limit = getReminderLimit()
        return limit == Constants.UNLIMITED || currentCount < limit
    }

    /**
     * Daha fazla badi ekleyebilir mi?
     */
    suspend fun canAddMoreBadis(currentCount: Int): Boolean {
        val limit = getBadiLimit()
        return limit == Constants.UNLIMITED || currentCount < limit
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 FEATURE GATES (Özellik Kısıtlamaları)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Özel bildirim sesi kullanabilir mi? (Ekstra+)
     */
    suspend fun canUseCustomNotificationSound(): Boolean = isEkstraOrAbove()

    /**
     * Gelişmiş istatistikler görebilir mi? (Ekstra+)
     */
    suspend fun canViewAdvancedStats(): Boolean = isEkstraOrAbove()

    /**
     * Bulut yedekleme kullanabilir mi? (Ekstra+)
     */
    suspend fun canUseCloudBackup(): Boolean = isEkstraOrAbove()

    /**
     * Akıllı hatırlatmalar kullanabilir mi? (Ekstra+)
     */
    suspend fun canUseSmartReminders(): Boolean = isEkstraOrAbove()

    /**
     * Sesli hatırlatıcı kullanabilir mi? (Ekstra+)
     */
    suspend fun canUseVoiceReminders(): Boolean = isEkstraOrAbove()

    /**
     * Kritik bildirimler kullanabilir mi? (Ekstra+)
     */
    suspend fun canUseCriticalNotifications(): Boolean = isEkstraOrAbove()

    /**
     * Tema özelleştirme yapabilir mi? (Ekstra+)
     */
    suspend fun canUseThemeCustomization(): Boolean = isEkstraOrAbove()

    /**
     * Öncelikli destek alabilir mi? (Ekstra+)
     */
    suspend fun hasPrioritySupport(): Boolean = isEkstraOrAbove()

    /**
     * Badi sistemi kullanabilir mi? (Ekstra: 1, Aile: sınırsız)
     */
    suspend fun canUseBadiSystem(): Boolean {
        val category = getPlanCategory()
        return category == PlanCategory.EKSTRA || category == PlanCategory.AILE
    }

    /**
     * Birden fazla badi ekleyebilir mi? (Sadece Aile)
     */
    suspend fun canUseMultipleBadis(): Boolean = isAile()

    /**
     * Aile kontrol paneli kullanabilir mi? (Sadece Aile)
     */
    suspend fun canUseFamilyDashboard(): Boolean = isAile()

    /**
     * Aile takibi kullanabilir mi? (Sadece Aile)
     */
    suspend fun canUseFamilyTracking(): Boolean = isAile()

    /**
     * Premium özelliğe erişimi kontrol et
     */
    suspend fun checkPremiumFeature(feature: PremiumFeature): Boolean {
        if (isBanned()) return false

        return when (feature) {
            // Ekstra+ özellikleri
            PremiumFeature.CUSTOM_NOTIFICATION_SOUND -> canUseCustomNotificationSound()
            PremiumFeature.ADVANCED_STATS -> canViewAdvancedStats()
            PremiumFeature.CLOUD_BACKUP -> canUseCloudBackup()
            PremiumFeature.SMART_REMINDERS -> canUseSmartReminders()
            PremiumFeature.VOICE_REMINDERS -> canUseVoiceReminders()
            PremiumFeature.CRITICAL_NOTIFICATIONS -> canUseCriticalNotifications()
            PremiumFeature.THEME_CUSTOMIZATION -> canUseThemeCustomization()
            PremiumFeature.PRIORITY_SUPPORT -> hasPrioritySupport()
            PremiumFeature.UNLIMITED_MEDICINES -> isEkstraOrAbove()
            PremiumFeature.UNLIMITED_REMINDERS -> isEkstraOrAbove()

            // Badi özellikleri
            PremiumFeature.BADI_SYSTEM -> canUseBadiSystem()
            PremiumFeature.MULTIPLE_BADIS -> canUseMultipleBadis()

            // Aile özellikleri
            PremiumFeature.FAMILY_TRACKING -> canUseFamilyTracking()
            PremiumFeature.FAMILY_DASHBOARD -> canUseFamilyDashboard()
        }
    }

    /**
     * Özelliğin hangi planda açıldığını döndürür
     */
    fun getRequiredPlanForFeature(feature: PremiumFeature): PlanCategory {
        return when (feature) {
            // Sadece Aile planında olan özellikler
            PremiumFeature.FAMILY_TRACKING,
            PremiumFeature.FAMILY_DASHBOARD,
            PremiumFeature.MULTIPLE_BADIS -> PlanCategory.AILE

            // Ekstra ve üstünde olan özellikler
            else -> PlanCategory.EKSTRA
        }
    }

    /**
     * Premium özellik için kullanıcıya mesaj göster
     */
    fun getPremiumFeatureMessage(feature: PremiumFeature): String {
        val requiredPlan = getRequiredPlanForFeature(feature)
        val planName = requiredPlan.toTurkish()

        return when (feature) {
            PremiumFeature.CUSTOM_NOTIFICATION_SOUND ->
                "Özel bildirim sesleri $planName'ya özeldir"

            PremiumFeature.ADVANCED_STATS ->
                "Gelişmiş istatistikler için $planName'ya katıl"

            PremiumFeature.CLOUD_BACKUP ->
                "Bulut yedekleme özelliği $planName'da"

            PremiumFeature.SMART_REMINDERS ->
                "Akıllı hatırlatmalar için $planName'ya geç"

            PremiumFeature.VOICE_REMINDERS ->
                "Sesli hatırlatıcılar $planName'ya özeldir"

            PremiumFeature.CRITICAL_NOTIFICATIONS ->
                "Kritik bildirimler için $planName'ya geç"

            PremiumFeature.THEME_CUSTOMIZATION ->
                "Tema özelleştirme $planName'da mümkün"

            PremiumFeature.PRIORITY_SUPPORT ->
                "Öncelikli destek $planName üyelerine özeldir"

            PremiumFeature.UNLIMITED_MEDICINES ->
                "Sınırsız ilaç ekleme için $planName'ya geç"

            PremiumFeature.UNLIMITED_REMINDERS ->
                "Sınırsız hatırlatma için $planName'ya geç"

            PremiumFeature.BADI_SYSTEM ->
                "Badi sistemi için $planName'ya katıl"

            PremiumFeature.MULTIPLE_BADIS ->
                "Birden fazla badi için $planName'ya geç"

            PremiumFeature.FAMILY_TRACKING ->
                "Aile takibi $planName ile mümkün"

            PremiumFeature.FAMILY_DASHBOARD ->
                "Aile kontrol paneli $planName'ya özeldir"
        }
    }
}

/**
 * 💎 Premium Özellikler Enum
 */
enum class PremiumFeature(val displayName: String) {
    // Ekstra+ özellikleri
    CUSTOM_NOTIFICATION_SOUND("Özel Bildirim Sesi"),
    ADVANCED_STATS("Gelişmiş İstatistikler"),
    CLOUD_BACKUP("Bulut Yedekleme"),
    SMART_REMINDERS("Akıllı Hatırlatmalar"),
    VOICE_REMINDERS("Sesli Hatırlatıcılar"),
    CRITICAL_NOTIFICATIONS("Kritik Bildirimler"),
    THEME_CUSTOMIZATION("Tema Özelleştirme"),
    PRIORITY_SUPPORT("Öncelikli Destek"),
    UNLIMITED_MEDICINES("Sınırsız İlaç"),
    UNLIMITED_REMINDERS("Sınırsız Hatırlatma"),

    // Badi özellikleri
    BADI_SYSTEM("Badi Sistemi"),
    MULTIPLE_BADIS("Çoklu Badi"),

    // Aile özellikleri
    FAMILY_TRACKING("Aile Takibi"),
    FAMILY_DASHBOARD("Aile Kontrol Paneli")
}

/**
 * 🗺️ Feature Matrix
 * Hangi özelliğin hangi planlarda açık olduğunu tanımlar
 */
object FeatureMatrix {
    private val matrix: Map<PremiumFeature, Set<PlanCategory>> = mapOf(
        // Ekstra ve Aile'de açık
        PremiumFeature.CUSTOM_NOTIFICATION_SOUND to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.ADVANCED_STATS to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.CLOUD_BACKUP to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.SMART_REMINDERS to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.VOICE_REMINDERS to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.CRITICAL_NOTIFICATIONS to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.THEME_CUSTOMIZATION to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.PRIORITY_SUPPORT to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.UNLIMITED_MEDICINES to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.UNLIMITED_REMINDERS to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),
        PremiumFeature.BADI_SYSTEM to setOf(PlanCategory.EKSTRA, PlanCategory.AILE),

        // Sadece Aile'de açık
        PremiumFeature.MULTIPLE_BADIS to setOf(PlanCategory.AILE),
        PremiumFeature.FAMILY_TRACKING to setOf(PlanCategory.AILE),
        PremiumFeature.FAMILY_DASHBOARD to setOf(PlanCategory.AILE)
    )

    /**
     * Özellik belirtilen planda açık mı?
     */
    fun isFeatureAvailable(feature: PremiumFeature, category: PlanCategory): Boolean {
        return matrix[feature]?.contains(category) ?: false
    }

    /**
     * Belirtilen planda açık olan tüm özellikleri döndürür
     */
    fun getFeaturesForPlan(category: PlanCategory): List<PremiumFeature> {
        return matrix.filter { it.value.contains(category) }.keys.toList()
    }

    /**
     * Özelliğin açık olduğu minimum planı döndürür
     */
    fun getMinimumPlanForFeature(feature: PremiumFeature): PlanCategory {
        val plans = matrix[feature] ?: return PlanCategory.AILE
        return when {
            plans.contains(PlanCategory.FREE) -> PlanCategory.FREE
            plans.contains(PlanCategory.EKSTRA) -> PlanCategory.EKSTRA
            else -> PlanCategory.AILE
        }
    }
}
