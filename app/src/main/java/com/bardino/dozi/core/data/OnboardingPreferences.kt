package com.bardino.dozi.core.data

import android.content.Context

object OnboardingPreferences {
    private const val PREF_NAME = "onboarding_prefs"
    private const val KEY_FIRST_TIME = "is_first_time"
    private const val KEY_COMPLETED = "onboarding_completed"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_COMPLETED_AT = "completed_at"

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
}