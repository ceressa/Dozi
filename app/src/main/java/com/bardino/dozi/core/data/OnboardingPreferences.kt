package com.bardino.dozi.core.data

import android.content.Context
import android.util.Log
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.notifications.ReminderScheduler
import org.json.JSONArray
import org.json.JSONObject

object OnboardingPreferences {
    private const val PREF_NAME = "onboarding_prefs"
    private const val KEY_FIRST_TIME = "is_first_time"
    private const val KEY_COMPLETED = "onboarding_completed"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_COMPLETED_AT = "completed_at"
    private const val KEY_CURRENT_STEP = "current_onboarding_step"
    private const val KEY_IN_ONBOARDING = "is_in_onboarding"
    private const val KEY_NEVER_SHOW_AGAIN = "never_show_onboarding_again"

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

    // "Bir daha gösterme" tercihi
    fun setNeverShowOnboardingAgain(context: Context, neverShow: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_NEVER_SHOW_AGAIN, neverShow)
            .apply()
    }

    fun shouldNeverShowOnboardingAgain(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NEVER_SHOW_AGAIN, false)
    }

    // Onboarding gösterilmeli mi? (hem ilk sefer hem de "bir daha gösterme" kontrolü)
    fun shouldShowOnboarding(context: Context): Boolean {
        return isFirstTime(context) && !shouldNeverShowOnboardingAgain(context)
    }

    /**
     * 🔥 Onboarding sırasında lokale kaydedilen ilaçları Firebase'e sync et
     * Bu fonksiyon onboarding tamamlandığında çağrılmalı
     */
    suspend fun syncLocalRemindersToFirebase(context: Context) {
        try {
            val prefs = context.getSharedPreferences("local_reminders", Context.MODE_PRIVATE)
            val remindersJson = prefs.getString("reminders", "[]") ?: "[]"

            val remindersArray = try {
                JSONArray(remindersJson)
            } catch (e: Exception) {
                Log.e("OnboardingPreferences", "❌ JSON parse hatası", e)
                return
            }

            if (remindersArray.length() == 0) {
                Log.d("OnboardingPreferences", "ℹ️ Sync edilecek lokal ilaç yok")
                return
            }

            val medicineRepository = MedicineRepository()
            var syncedCount = 0

            for (i in 0 until remindersArray.length()) {
                try {
                    val reminderObj = remindersArray.getJSONObject(i)

                    // Zamanları parse et
                    val timesArray = reminderObj.getJSONArray("times")
                    val times = mutableListOf<String>()
                    for (j in 0 until timesArray.length()) {
                        times.add(timesArray.getString(j))
                    }

                    // Tarihleri parse et
                    val datesArray = reminderObj.optJSONArray("selectedDates") ?: JSONArray()
                    val selectedDates = mutableListOf<String>()
                    for (j in 0 until datesArray.length()) {
                        selectedDates.add(datesArray.getString(j))
                    }

                    // Frequency value hesapla
                    val frequency = reminderObj.getString("frequency")
                    val xValue = reminderObj.optInt("xValue", 1)
                    val frequencyValue = when (frequency) {
                        "Her gün" -> 1
                        "Gün aşırı" -> 2
                        "Haftada bir" -> 7
                        "15 günde bir" -> 15
                        "Ayda bir" -> 30
                        "Her X günde bir" -> xValue
                        else -> 1
                    }

                    val medicine = Medicine(
                        id = "", // Repository tarafından oluşturulacak
                        userId = "", // Repository tarafından oluşturulacak
                        name = reminderObj.getString("name"),
                        dosage = reminderObj.optString("dosage", ""),
                        unit = reminderObj.optString("unit", "hap"),
                        form = "tablet",
                        times = times,
                        days = if (frequency == "İstediğim tarihlerde") selectedDates else emptyList(),
                        frequency = frequency,
                        frequencyValue = frequencyValue,
                        startDate = reminderObj.optLong("startDate", System.currentTimeMillis()),
                        endDate = null,
                        stockCount = 0,
                        boxSize = 0,
                        notes = "",
                        reminderEnabled = true,
                        reminderName = reminderObj.getString("name"),
                        icon = "💊"
                    )

                    val savedMedicine = medicineRepository.addMedicine(medicine)

                    if (savedMedicine != null) {
                        // Alarmları planla
                        ReminderScheduler.scheduleReminders(context, savedMedicine)
                        syncedCount++
                        Log.d("OnboardingPreferences", "✅ Firebase'e sync edildi: ${savedMedicine.name}")
                    } else {
                        Log.e("OnboardingPreferences", "❌ Firebase'e kayıt başarısız: ${medicine.name}")
                    }
                } catch (e: Exception) {
                    Log.e("OnboardingPreferences", "❌ İlaç sync hatası: ${e.message}", e)
                }
            }

            // Sync başarılı olduysa lokal verileri temizle
            if (syncedCount > 0) {
                prefs.edit().remove("reminders").apply()
                Log.d("OnboardingPreferences", "🔥 $syncedCount ilaç Firebase'e sync edildi ve lokal veriler temizlendi")
            }
        } catch (e: Exception) {
            Log.e("OnboardingPreferences", "❌ Sync hatası", e)
        }
    }
}