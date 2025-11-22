package com.bardino.dozi.core.ui.screens.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Onboarding durumunu yöneten sınıf
 */
@Singleton
class OnboardingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_COMPLETED = "onboarding_completed"
        private const val KEY_FIRST_MEDICINE_ADDED = "first_medicine_added"
        private const val KEY_PREMIUM_SHOWN = "premium_shown"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Onboarding tamamlandı mı?
     */
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    /**
     * Onboarding'i tamamlandı olarak işaretle
     */
    fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    /**
     * İlk ilaç eklendi mi?
     */
    fun isFirstMedicineAdded(): Boolean {
        return prefs.getBoolean(KEY_FIRST_MEDICINE_ADDED, false)
    }

    /**
     * İlk ilaç eklendi olarak işaretle
     */
    fun markFirstMedicineAdded() {
        prefs.edit().putBoolean(KEY_FIRST_MEDICINE_ADDED, true).apply()
    }

    /**
     * Premium ekranı gösterildi mi?
     */
    fun isPremiumShown(): Boolean {
        return prefs.getBoolean(KEY_PREMIUM_SHOWN, false)
    }

    /**
     * Premium ekranı gösterildi olarak işaretle
     */
    fun markPremiumShown() {
        prefs.edit().putBoolean(KEY_PREMIUM_SHOWN, true).apply()
    }

    /**
     * Onboarding gösterilmeli mi?
     */
    fun shouldShowOnboarding(): Boolean {
        return !isOnboardingCompleted()
    }

    /**
     * Tüm onboarding verilerini sıfırla
     */
    fun reset() {
        prefs.edit().clear().apply()
    }
}
