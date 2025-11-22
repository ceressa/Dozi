package com.bardino.dozi.notifications

import android.content.Context
import android.util.Log
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.UserPreferencesRepository
import com.bardino.dozi.core.data.model.MedicationStatus
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 🧠 Akıllı hatırlatma önerileri sunan helper
 *
 * Pattern tanıma:
 * - Kullanıcı bu ilacı genellikle ne kadar geç alıyor?
 * - En çok hangi erteleme süresini seçiyor?
 * - Zamanı değiştirme önerisi yapılmalı mı?
 */
object SmartReminderHelper {

    private const val TAG = "SmartReminderHelper"
    private const val ANALYSIS_DAYS = 14 // Son 14 günü analiz et
    private const val MIN_SAMPLES = 3 // En az 3 örnek olmalı pattern tanıma için

    /**
     * Akıllı erteleme önerisi
     *
     * @return Pair<snoozeMinutes, suggestion>
     * örnek: (30, "Genellikle 30 dk sonra alıyorsunuz")
     */
    suspend fun getSmartSnoozeSuggestion(
        context: Context,
        medicineId: String,
        scheduledTime: Long
    ): Pair<Int?, String?> = withContext(Dispatchers.IO) {
        try {
            val logRepository = MedicationLogRepository(
                context,
                com.google.firebase.auth.FirebaseAuth.getInstance(),
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            )

            // Son 14 gün içinde bu ilaç için alınan/ertelenen logları al
            val startTime = System.currentTimeMillis() - (ANALYSIS_DAYS * 24 * 60 * 60 * 1000L)
            val logs = logRepository.getLogsForMedicine(medicineId, startTime)
            val snoozedLogs = logs.filter { it.status == MedicationStatus.SNOOZED.name }

            // Eğer yeterli örnek varsa, pattern analizi yap
            if (snoozedLogs.size >= MIN_SAMPLES) {
                // En çok kullanılan erteleme süresini bul
                // Not: Şu an notes'tan parse ediyoruz, ileride daha iyi bir yöntem olabilir
                val snoozeMinutes = snoozedLogs.mapNotNull { log ->
                    log.notes?.let { note ->
                        val regex = """Snoozed for (\d+) minutes""".toRegex()
                        regex.find(note)?.groupValues?.get(1)?.toIntOrNull()
                    }
                }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

                if (snoozeMinutes != null) {
                    return@withContext (snoozeMinutes to "Genellikle $snoozeMinutes dk erteliyorsunuz")
                }
            }

            // Firestore'da pattern yoksa, SharedPreferences'tan sofistike pattern verilerini oku
            val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)

            // Öncelik sırası: mode (en çok kullanılan) > ortalama > son erteleme
            val modeSnoozeMinutes = prefs.getInt("mode_snooze_minutes_$medicineId", -1)
            val avgSnoozeMinutes = prefs.getInt("avg_snooze_minutes_$medicineId", -1)
            val lastSnoozeMinutes = prefs.getInt("last_snooze_minutes_$medicineId", -1)

            // En çok kullanılan erteleme süresi varsa onu öner
            if (modeSnoozeMinutes > 0) {
                return@withContext (modeSnoozeMinutes to "Genellikle $modeSnoozeMinutes dk erteliyorsunuz")
            }

            // Ortalama varsa onu öner
            if (avgSnoozeMinutes > 0) {
                return@withContext (avgSnoozeMinutes to "Ortalama $avgSnoozeMinutes dk erteliyorsunuz")
            }

            // Son erteleme varsa onu öner
            if (lastSnoozeMinutes > 0) {
                return@withContext (lastSnoozeMinutes to "Geçen seferde $lastSnoozeMinutes dk ertelemiştiniz")
            }

            // Pattern yok
            return@withContext (null to null)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting smart snooze suggestion", e)
            return@withContext (null to null)
        }
    }

    /**
     * Zamanı değiştirme önerisi yap
     *
     * "Hep yarım saat geç alıyorsunuz, artık şu saatte almak ister misiniz?"
     *
     * @return Pair<newTime, suggestion>
     * örnek: ("09:30", "Bu ilacı genellikle 09:30'da alıyorsunuz. Hatırlatma zamanını değiştirmek ister misiniz?")
     */
    suspend fun getTimeAdjustmentSuggestion(
        context: Context,
        medicineId: String,
        scheduledTimeStr: String // "09:00"
    ): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val logRepository = MedicationLogRepository(
                context,
                com.google.firebase.auth.FirebaseAuth.getInstance(),
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            )

            // Son N hatırlatmayı analiz et
            val startTime = System.currentTimeMillis() - (ANALYSIS_DAYS * 24 * 60 * 60 * 1000L)
            val logs = logRepository.getLogsForMedicine(medicineId, startTime)
            val takenLogs = logs.filter { it.status == MedicationStatus.TAKEN.name && it.takenAt != null && it.scheduledTime != null }

            // Eğer yeterli örnek varsa, gecikme analizi yap
            if (takenLogs.size >= MIN_SAMPLES) {
                // scheduledTime vs takenAt farkını hesapla
                val delays = takenLogs.mapNotNull { log ->
                    val scheduled = log.scheduledTime?.toDate()?.time ?: return@mapNotNull null
                    val taken = log.takenAt?.toDate()?.time ?: return@mapNotNull null
                    val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(taken - scheduled)
                    if (delayMinutes > 0) delayMinutes.toInt() else null
                }

                // Ortalama gecikme hesapla
                if (delays.isNotEmpty()) {
                    val avgDelayMinutes = delays.average().toInt()

                    if (avgDelayMinutes >= 30) {
                        // Yeni saat hesapla
                        val (hour, minute) = scheduledTimeStr.split(":").map { it.toInt() }
                        val newHour = (hour + avgDelayMinutes / 60) % 24
                        val newMinute = (minute + avgDelayMinutes % 60) % 60
                        val newTime = String.format("%02d:%02d", newHour, newMinute)

                        return@withContext (newTime to "Bu ilacı genellikle $newTime'de alıyorsunuz. Hatırlatma zamanını değiştirmek ister misiniz?")
                    }
                }
            }

            // Firestore'da pattern yoksa, SharedPreferences'tan son alma gecikmesini oku
            val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
            val avgDelayMinutes = prefs.getInt("avg_delay_minutes_$medicineId", -1)

            if (avgDelayMinutes >= 30) {
                // Yeni saat hesapla
                val (hour, minute) = scheduledTimeStr.split(":").map { it.toInt() }
                val newHour = (hour + avgDelayMinutes / 60) % 24
                val newMinute = (minute + avgDelayMinutes % 60) % 60
                val newTime = String.format("%02d:%02d", newHour, newMinute)

                return@withContext (newTime to "Bu ilacı genellikle $newTime'de alıyorsunuz. Hatırlatma zamanını değiştirmek ister misiniz?")
            }

            // Pattern yok
            return@withContext (null to null)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting time adjustment suggestion", e)
            return@withContext (null to null)
        }
    }

    /**
     * Erteleme sonrası analiz yap ve pattern'i kaydet
     *
     * Kullanıcı erteleme seçtikten sonra bu fonksiyon çağrılır
     */
    suspend fun recordSnoozePattern(
        context: Context,
        medicineId: String,
        snoozeMinutes: Int,
        scheduledTime: Long
    ) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
            val gson = Gson()

            // Son 5 erteleme süresini array olarak sakla (circular buffer)
            val snoozeHistoryKey = "snooze_history_$medicineId"
            val snoozeHistoryJson = prefs.getString(snoozeHistoryKey, "[]")
            val snoozeHistory = gson.fromJson(snoozeHistoryJson, Array<Int>::class.java)?.toMutableList() ?: mutableListOf()

            // Yeni erteleme süresini ekle
            snoozeHistory.add(snoozeMinutes)

            // En fazla 5 örnek tut (en eski kaydı sil)
            if (snoozeHistory.size > 5) {
                snoozeHistory.removeAt(0)
            }

            // Ortalama erteleme süresini hesapla
            val avgSnooze = if (snoozeHistory.isNotEmpty()) {
                snoozeHistory.average().toInt()
            } else {
                snoozeMinutes
            }

            // En çok kullanılan erteleme süresini bul (mode)
            val modeSnooze = snoozeHistory
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: snoozeMinutes

            // Verileri kaydet
            prefs.edit()
                .putInt("last_snooze_minutes_$medicineId", snoozeMinutes)
                .putLong("last_snooze_timestamp_$medicineId", System.currentTimeMillis())
                .putString(snoozeHistoryKey, gson.toJson(snoozeHistory))
                .putInt("avg_snooze_minutes_$medicineId", avgSnooze)
                .putInt("mode_snooze_minutes_$medicineId", modeSnooze)
                .apply()

            // ✅ Firebase'e senkronize et
            try {
                val userPrefsRepo = UserPreferencesRepository(context)
                userPrefsRepo.syncSmartPattern(
                    medicineId = medicineId,
                    modeSnoozeMinutes = modeSnooze,
                    avgSnoozeMinutes = avgSnooze,
                    lastSnoozeMinutes = snoozeMinutes
                )
                userPrefsRepo.syncSnoozeHistory(medicineId, snoozeHistory)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Smart pattern Firebase'e senkronize edilemedi", e)
            }

            Log.d(TAG, "✅ Snooze pattern kaydedildi: $medicineId -> son: $snoozeMinutes dk, ortalama: $avgSnooze dk, en çok: $modeSnooze dk")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error recording snooze pattern", e)
        }
    }

    /**
     * İlaç alındıktan sonra gecikme analizi yap
     *
     * Scheduled time vs actual taken time farkını hesapla ve kaydet
     */
    suspend fun recordDelayPattern(
        context: Context,
        medicineId: String,
        scheduledTime: Long,
        takenTime: Long
    ) = withContext(Dispatchers.IO) {
        try {
            val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(takenTime - scheduledTime)

            if (delayMinutes <= 0) {
                // Zamanında veya erken alındı, pattern yok
                return@withContext
            }

            val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)

            // Son 5 gecikmeyi kaydet (circular buffer)
            val delaysKey = "delay_history_$medicineId"
            val delaysJson = prefs.getString(delaysKey, "[]")
            val delays = com.google.gson.Gson().fromJson(delaysJson, Array<Int>::class.java)?.toMutableList() ?: mutableListOf()

            delays.add(delayMinutes.toInt())
            if (delays.size > 5) {
                delays.removeAt(0) // En eski kaydı sil
            }

            // Ortalama gecikme hesapla
            val avgDelay = if (delays.isNotEmpty()) delays.average().toInt() else 0

            prefs.edit()
                .putString(delaysKey, com.google.gson.Gson().toJson(delays))
                .putInt("avg_delay_minutes_$medicineId", avgDelay)
                .apply()

            // ✅ Firebase'e senkronize et
            try {
                val userPrefsRepo = UserPreferencesRepository(context)
                userPrefsRepo.syncSmartPattern(
                    medicineId = medicineId,
                    avgDelayMinutes = avgDelay
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Delay pattern Firebase'e senkronize edilemedi", e)
            }

            Log.d(TAG, "✅ Delay pattern kaydedildi: $medicineId -> ortalama $avgDelay dk gecikme")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error recording delay pattern", e)
        }
    }

    /**
     * Kullanıcının geçmiş davranışlarına göre önerilen erteleme süreleri
     *
     * @return List<Pair<minutes, displayText>>
     * örnek: [(10, "10 dakika"), (30, "30 dakika ⭐"), (60, "1 saat")]
     * ⭐ işareti en çok kullanılan süreyi gösterir
     */
    suspend fun getSuggestedSnoozeTimes(
        context: Context,
        medicineId: String
    ): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)

            // En çok kullanılan erteleme süresini al (mode)
            val modeSnoozeMinutes = prefs.getInt("mode_snooze_minutes_$medicineId", -1)

            val defaultTimes = listOf(
                10 to "10 dakika",
                20 to "20 dakika",
                30 to "30 dakika",
                60 to "1 saat"
            )

            // Eğer kullanıcının en çok kullandığı erteleme süresi varsa, o süreyi ⭐ ile işaretle
            if (modeSnoozeMinutes > 0) {
                return@withContext defaultTimes.map { (minutes, text) ->
                    if (minutes == modeSnoozeMinutes) {
                        minutes to "$text ⭐"
                    } else {
                        minutes to text
                    }
                }
            }

            return@withContext defaultTimes
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting suggested snooze times", e)
            return@withContext listOf(
                10 to "10 dakika",
                20 to "20 dakika",
                30 to "30 dakika",
                60 to "1 saat"
            )
        }
    }
}
