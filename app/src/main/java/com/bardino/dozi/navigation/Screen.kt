package com.bardino.dozi.navigation

sealed class Screen(val route: String) {
    // Onboarding
    object OnboardingWelcome : Screen("onboarding/welcome")
    object OnboardingIntro : Screen("onboarding/intro")
    object OnboardingMedicine : Screen("onboarding/medicine")
    object OnboardingName : Screen("onboarding/name")
    object OnboardingReminder : Screen("onboarding/reminder")
    object OnboardingHomeTour : Screen("onboarding/home_tour")
    object OnboardingPremium : Screen("onboarding/premium")

    // Main app
    object Home : Screen("home")
    object MedicineList : Screen("medicine_list")

    object MedicineDetail : Screen("medicine_detail/{medicineId}") {
        fun createRoute(id: String) = "medicine_detail/$id"
    }

    object MedicineEdit : Screen("medicine_edit/{id}") {
        fun createRoute(id: String) = "medicine_edit/$id"
    }

    object MedicineLookup : Screen("medicine_lookup")
    object ReminderList : Screen("reminder_list")
    object AddReminder : Screen("add_reminder")
    object EditReminder : Screen("edit_reminder/{medicineId}") {
        fun createRoute(medicineId: String) = "edit_reminder/$medicineId"
    }
    object Profile : Screen("profile")
    object Premium : Screen("premium")
    object Locations : Screen("locations")
    object Settings : Screen("settings")
    object NotificationSettings : Screen("notification_settings")
    object About : Screen("about")

    // Medication Action (from notification)
    object MedicationAction : Screen("medication_action/{time}") {
        fun createRoute(time: String) = "medication_action/$time"
    }

}