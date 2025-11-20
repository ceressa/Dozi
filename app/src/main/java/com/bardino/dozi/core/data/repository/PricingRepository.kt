package com.bardino.dozi.core.data.repository

import android.util.Log
import com.bardino.dozi.core.data.model.DefaultPricing
import com.bardino.dozi.core.data.model.PlanPricing
import com.bardino.dozi.core.data.model.PremiumPlanType
import com.bardino.dozi.core.data.model.PricingConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 💰 Fiyatlandırma Repository
 *
 * Firestore'dan fiyat bilgilerini çeker ve cache'ler.
 * Offline durumda varsayılan fiyatları kullanır.
 */
@Singleton
class PricingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "PricingRepository"
        private const val CONFIG_COLLECTION = "config"
        private const val PRICING_DOCUMENT = "pricing"
    }

    private val _pricingConfig = MutableStateFlow(DefaultPricing.config)
    val pricingConfig: Flow<PricingConfig> = _pricingConfig.asStateFlow()

    private var isInitialized = false

    /**
     * Fiyatları Firestore'dan yükler
     */
    suspend fun loadPricing(): Result<PricingConfig> {
        return try {
            val document = firestore
                .collection(CONFIG_COLLECTION)
                .document(PRICING_DOCUMENT)
                .get()
                .await()

            if (document.exists()) {
                val config = parsePricingDocument(document.data)
                _pricingConfig.value = config
                isInitialized = true
                Log.d(TAG, "Pricing loaded from Firestore")
                Result.success(config)
            } else {
                Log.w(TAG, "Pricing document not found, using defaults")
                _pricingConfig.value = DefaultPricing.config
                isInitialized = true
                Result.success(DefaultPricing.config)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load pricing, using defaults", e)
            _pricingConfig.value = DefaultPricing.config
            isInitialized = true
            Result.failure(e)
        }
    }

    /**
     * Mevcut fiyat konfigürasyonunu döndürür
     */
    fun getCurrentPricing(): PricingConfig {
        return _pricingConfig.value
    }

    /**
     * Belirli bir plan için fiyat bilgisi döndürür
     */
    fun getPricingForPlan(planType: PremiumPlanType): PlanPricing? {
        val config = _pricingConfig.value
        return when (planType) {
            PremiumPlanType.EKSTRA_MONTHLY -> config.ekstraMonthly
            PremiumPlanType.EKSTRA_YEARLY -> config.ekstraYearly
            PremiumPlanType.AILE_MONTHLY -> config.aileMonthly
            PremiumPlanType.AILE_YEARLY -> config.aileYearly
            else -> null
        }
    }

    /**
     * Plan süresini gün olarak döndürür
     */
    fun getDurationDays(planType: PremiumPlanType): Int {
        return when (planType) {
            PremiumPlanType.FREE -> 0
            PremiumPlanType.TRIAL -> _pricingConfig.value.trialDurationDays
            PremiumPlanType.EKSTRA_MONTHLY -> _pricingConfig.value.ekstraMonthly.durationDays
            PremiumPlanType.EKSTRA_YEARLY -> _pricingConfig.value.ekstraYearly.durationDays
            PremiumPlanType.AILE_MONTHLY -> _pricingConfig.value.aileMonthly.durationDays
            PremiumPlanType.AILE_YEARLY -> _pricingConfig.value.aileYearly.durationDays
        }
    }

    /**
     * Trial süresini döndürür
     */
    fun getTrialDurationDays(): Int {
        return _pricingConfig.value.trialDurationDays
    }

    /**
     * Fiyatları real-time olarak dinler
     */
    fun observePricing() {
        firestore
            .collection(CONFIG_COLLECTION)
            .document(PRICING_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Pricing listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val config = parsePricingDocument(snapshot.data)
                    _pricingConfig.value = config
                    Log.d(TAG, "Pricing updated from real-time listener")
                }
            }
    }

    /**
     * Firestore document'ını PricingConfig'e parse eder
     */
    @Suppress("UNCHECKED_CAST")
    private fun parsePricingDocument(data: Map<String, Any>?): PricingConfig {
        if (data == null) return DefaultPricing.config

        return try {
            PricingConfig(
                ekstraMonthly = parsePlanPricing(data["ekstra_monthly"] as? Map<String, Any>)
                    ?: DefaultPricing.config.ekstraMonthly,
                ekstraYearly = parsePlanPricing(data["ekstra_yearly"] as? Map<String, Any>)
                    ?: DefaultPricing.config.ekstraYearly,
                aileMonthly = parsePlanPricing(data["aile_monthly"] as? Map<String, Any>)
                    ?: DefaultPricing.config.aileMonthly,
                aileYearly = parsePlanPricing(data["aile_yearly"] as? Map<String, Any>)
                    ?: DefaultPricing.config.aileYearly,
                trialDurationDays = (data["trial_duration_days"] as? Long)?.toInt()
                    ?: DefaultPricing.config.trialDurationDays
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse pricing document", e)
            DefaultPricing.config
        }
    }

    /**
     * Plan fiyat bilgisini parse eder
     */
    private fun parsePlanPricing(data: Map<String, Any>?): PlanPricing? {
        if (data == null) return null

        return try {
            PlanPricing(
                price = (data["price"] as? Number)?.toFloat() ?: 0f,
                currency = data["currency"] as? String ?: "TRY",
                durationDays = (data["duration_days"] as? Long)?.toInt() ?: 30,
                displayName = data["display_name"] as? String ?: "",
                isActive = data["is_active"] as? Boolean ?: true,
                savingsPercent = (data["savings_percent"] as? Long)?.toInt() ?: 0,
                discountPercent = (data["discount_percent"] as? Long)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse plan pricing", e)
            null
        }
    }

    /**
     * Fiyatları Firestore'a kaydeder (Admin kullanımı için)
     */
    suspend fun savePricing(config: PricingConfig): Result<Unit> {
        return try {
            val data = mapOf(
                "ekstra_monthly" to planPricingToMap(config.ekstraMonthly),
                "ekstra_yearly" to planPricingToMap(config.ekstraYearly),
                "aile_monthly" to planPricingToMap(config.aileMonthly),
                "aile_yearly" to planPricingToMap(config.aileYearly),
                "trial_duration_days" to config.trialDurationDays,
                "last_updated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            firestore
                .collection(CONFIG_COLLECTION)
                .document(PRICING_DOCUMENT)
                .set(data)
                .await()

            _pricingConfig.value = config
            Log.d(TAG, "Pricing saved to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save pricing", e)
            Result.failure(e)
        }
    }

    private fun planPricingToMap(pricing: PlanPricing): Map<String, Any?> {
        return mapOf(
            "price" to pricing.price,
            "currency" to pricing.currency,
            "duration_days" to pricing.durationDays,
            "display_name" to pricing.displayName,
            "is_active" to pricing.isActive,
            "savings_percent" to pricing.savingsPercent,
            "discount_percent" to pricing.discountPercent,
            "campaign_end_date" to pricing.campaignEndDate
        )
    }
}
