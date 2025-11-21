package com.bardino.dozi.core.utils

import android.content.Context
import android.util.Log

/**
 * İlaç durumu kaydetme ve okuma yardımcı fonksiyonları
 * SharedPreferences kullanarak doz durumlarını saklar
 */
object MedicationStatusHelper {

    private const val PREFS_NAME = "medicine_status"
    private const val TAG = "MedicationStatusHelper"

    /**
     * İlaç durumunu kaydet
     */
    fun saveMedicineStatus(
        context: Context,
        medicineId: String,
        date: String,
        time: String,
        status: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "dose_${medicineId}_${date}_${time}"
        prefs.edit().putString(key, status).commit() // commit() senkron, hemen kaydet
        Log.d(TAG, "Status saved: $key = $status")
    }

    /**
     * İlaç durumunu oku
     */
    fun getMedicineStatus(
        context: Context,
        medicineId: String,
        date: String,
        time: String
    ): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "dose_${medicineId}_${date}_${time}"
        return prefs.getString(key, null)
    }

    /**
     * Bugünün tarihini string olarak al
     */
    fun getCurrentDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val year = calendar.get(java.util.Calendar.YEAR)
        return "%02d/%02d/%d".format(day, month, year)
    }
}

// Top-level extension fonksiyonları (backward compatibility için)
fun saveMedicineStatus(context: Context, medicineId: String, date: String, time: String, status: String) {
    MedicationStatusHelper.saveMedicineStatus(context, medicineId, date, time, status)
}

fun getMedicineStatus(context: Context, medicineId: String, date: String, time: String): String? {
    return MedicationStatusHelper.getMedicineStatus(context, medicineId, date, time)
}

fun getCurrentDateString(): String {
    return MedicationStatusHelper.getCurrentDateString()
}
