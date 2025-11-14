package com.bardino.dozi.core.data

import android.content.Context

object OnboardingPreferences {
    private const val PREF_NAME = "onboarding_prefs"
    private const val KEY_FIRST_TIME = "is_first_time"
    private const val KEY_COMPLETED = "onboarding_completed"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_COMPLETED_AT = "completed_at"
    private const val KEY_CURRENT_STEP = "current_onboarding_step"
    private const val KEY_IN_ONBOARDING = "is_in_onboarding"

    fun isFirstTime(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_TIME, true)
    }

    fun setFirstTimeComplete(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_FIRST_TIME, false)
            .putBoolean(KEY_COMPLETED, true)
            .putLong(KEY_COMPLETED_AT, System.currentTimeMillis())
            .apply()
    }

    fun saveUserName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun skipOnboarding(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_FIRST_TIME, false)
            .apply()
    }

    // Onboarding state yönetimi
    fun setOnboardingStep(context: Context, step: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CURRENT_STEP, step)
            .putBoolean(KEY_IN_ONBOARDING, true)
            .apply()
    }

    fun getOnboardingStep(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_STEP, null)
    }

    fun isInOnboarding(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IN_ONBOARDING, false)
    }

    fun clearOnboardingState(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_CURRENT_STEP)
            .putBoolean(KEY_IN_ONBOARDING, false)
            .apply()
    }
}