package com.bardino.dozi.core.common

object Constants {
    const val DATABASE_NAME = "dozi_database"
    const val PREFERENCES_NAME = "dozi_preferences"

    // Plan Limitleri
    const val FREE_MEDICINE_LIMIT = 1
    const val FREE_REMINDER_LIMIT = 2  // time slot bazlı
    const val EKSTRA_BADI_LIMIT = 1
    const val AILE_MAX_MEMBERS = 4  // organizer dahil
    const val TRIAL_DURATION_DAYS = 3

    // Sınırsız için kullanılan değer
    const val UNLIMITED = -1

    // Notification Channels
    const val REMINDER_CHANNEL_ID = "reminder_channel"
    const val BUDDY_CHANNEL_ID = "buddy_channel"
    const val CRITICAL_CHANNEL_ID = "critical_channel"

    // UI
    const val MIN_TOUCH_TARGET_DP = 48
}