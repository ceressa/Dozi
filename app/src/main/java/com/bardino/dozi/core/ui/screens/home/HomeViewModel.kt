package com.bardino.dozi.core.ui.screens.home

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.model.decrementStock
import com.bardino.dozi.core.data.model.daysRemainingInStock
import com.bardino.dozi.core.data.model.isStockLow
import com.bardino.dozi.core.data.model.isStockCritical
import com.bardino.dozi.core.data.model.isStockEmpty
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.UserRepository
import com.bardino.dozi.core.data.repository.AchievementRepository
import com.bardino.dozi.core.data.repository.UserStatsRepository
import com.bardino.dozi.core.data.repository.UserPreferencesRepository
import com.bardino.dozi.core.utils.EscalationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * HomeScreen için ViewModel
 * State management ve business logic burada
 * ✅ Offline-first: MedicationLogRepository kullanır
 */
@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicineRepository: MedicineRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val userRepository: UserRepository,
    private val achievementRepository: AchievementRepository,
    private val userStatsRepository: UserStatsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    /**
     * UI State data class
     * Tüm UI state'leri tek bir yerde
     */
    data class HomeUiState(
        val user: User? = null,
        val isLoggedIn: Boolean = false,
        val todaysMedicines: List<Medicine> = emptyList(),
        val upcomingMedicine: Pair<Medicine, String>? = null,
        val allUpcomingMedicines: List<Pair<Medicine, String>> = emptyList(),
        val currentMedicineStatus: MedicineStatus = MedicineStatus.UPCOMING,
        val snoozeMinutes: Int = 0,
        val lastSnoozeTimestamp: Long = 0L,
        val isLoading: Boolean = true,
        val error: String? = null,
        val showSuccessPopup: Boolean = false,
        val showSkippedPopup: Boolean = false,
        val showSkipDialog: Boolean = false,
        val showSnoozeDialog: Boolean = false
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        // 🔥 Medicines Flow'u dinle (polling yerine)
        observeMedicinesFlow()
        loadSnoozeState()
        startSnoozeTimer()
        // 🔥 FIX: Kritik ilaç eskalasyonlarını kontrol et
        checkEscalations()
    }

    /**
     * 🚨 Kritik ilaç eskalasyonlarını kontrol et
     * Kaçırılan kritik ilaçlar için buddy'lere bildirim gönder
     */
    private fun checkEscalations() {
        viewModelScope.launch {
            try {
                val escalationManager = EscalationManager(context)
                escalationManager.checkAndEscalate()
                Log.d(TAG, "✅ Escalation check completed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Escalation check failed: ${e.message}", e)
            }
        }
    }

    /**
     * 🔥 Medicines Flow
     * Automatically reloads when medicines change
     */
    private fun observeMedicinesFlow() {
        viewModelScope.launch {
            medicineRepository.getMedicinesFlow()
                .catch { error ->
                    android.util.Log.e(TAG, "❌ Error observing medicines: ${error.message}")
                }
                .collect { medicines ->
                    android.util.Log.d(TAG, "🔄 Medicines updated: ${medicines.size} medicines")
                    updateMedicinesState(medicines)
                }
        }
    }

    /**
     * Medicines state'ini güncelle
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updateMedicinesState(allMedicines: List<Medicine>) {
        try {
            // ✅ Bugünün tarihini al
            val today = java.time.LocalDate.now()
            val todayMillis = System.currentTimeMillis()

            // ✅ Bugün için geçerli olan ilaçları filtrele
            val todaysMeds = allMedicines.filter { medicine ->
                // Hatırlatma aktif mi ve tarih aralığında mı?
                if (!medicine.reminderEnabled) return@filter false
                if (medicine.startDate > todayMillis) return@filter false
                if (medicine.endDate != null && medicine.endDate < todayMillis) return@filter false

                // shouldMedicineShowOnDate() mantığını kullan
                shouldMedicineShowOnDate(medicine, today)
            }

            val upcoming = todaysMeds.flatMap { medicine ->
                medicine.times.map { time -> Pair(medicine, time) }
            }.filter { (medicine, time) ->
                val (hour, minute) = time.split(":").map { it.toInt() }
                val currentHour = java.time.LocalTime.now().hour
                val currentMinute = java.time.LocalTime.now().minute
                val medicineTime = hour * 60 + minute
                val currentTime = currentHour * 60 + currentMinute
                medicineTime >= currentTime
            }.sortedBy { it.second }

            _uiState.update {
                it.copy(
                    todaysMedicines = todaysMeds,
                    allUpcomingMedicines = upcoming,
                    upcomingMedicine = upcoming.firstOrNull()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating medicines state: ${e.message}")
        }
    }

    /**
     * Helper: Get current day name in Turkish
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentDayName(): String {
        val dayOfWeek = java.time.LocalDate.now().dayOfWeek.value
        return when (dayOfWeek) {
            1 -> "Pazartesi"
            2 -> "Salı"
            3 -> "Çarşamba"
            4 -> "Perşembe"
            5 -> "Cuma"
            6 -> "Cumartesi"
            7 -> "Pazar"
            else -> ""
        }
    }

    /**
     * Helper: Belirli bir tarihte ilacın gösterilip gösterilmeyeceğini kontrol eder
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun shouldMedicineShowOnDate(medicine: Medicine, date: java.time.LocalDate): Boolean {
        // startDate kontrolü
        val startLocalDate = java.time.Instant.ofEpochMilli(medicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()

        if (date.isBefore(startLocalDate)) {
            return false // Başlangıç tarihinden önce gösterme
        }

        // endDate kontrolü
        if (medicine.endDate != null) {
            val endLocalDate = java.time.Instant.ofEpochMilli(medicine.endDate!!)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            if (date.isAfter(endLocalDate)) {
                return false // Bitiş tarihinden sonra gösterme
            }
        }

        when (medicine.frequency) {
            "Her gün" -> return true

            "Gün aşırı" -> {
                // Başlangıç gününden itibaren gün aşırı: gün 0 (al), gün 1 (alma), gün 2 (al), ...
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startLocalDate, date)
                return daysSinceStart % 2 == 0L
            }

            "Haftada bir" -> {
                // Başlangıç tarihinin haftanın günü ile aynı günlerde al
                return startLocalDate.dayOfWeek == date.dayOfWeek
            }

            "15 günde bir" -> {
                // Her 15 günde bir: gün 0, 15, 30, 45, ...
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startLocalDate, date)
                return daysSinceStart % 15 == 0L
            }

            "Ayda bir" -> {
                // Her 30 günde bir: gün 0, 30, 60, 90, ...
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startLocalDate, date)
                return daysSinceStart % 30 == 0L
            }

            "Her X günde bir" -> {
                // Her X günde bir: gün 0, X, 2X, 3X, ...
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startLocalDate, date)
                return daysSinceStart % medicine.frequencyValue.toLong() == 0L
            }

            "İstediğim tarihlerde" -> {
                // Kullanıcının seçtiği özel tarihlerde
                val dateString = "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)
                return medicine.days.contains(dateString)
            }

            else -> return false
        }
    }

    /**
     * Kullanıcı ve ilaç verilerini yükle
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                // Kullanıcı verilerini çek
                val userData = userRepository.getUserData()
                val isLoggedIn = userData != null
                _uiState.update { it.copy(user = userData, isLoggedIn = isLoggedIn) }

                // İlaç verilerini çek (sadece login olduysa)
                if (isLoggedIn) {
                    refreshMedicines(null)
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Bilinmeyen hata",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Periyodik veri güncellemesi (Firebase için polling)
     */
    private fun startPollingData() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Kullanıcı verilerini güncelle
                    val userData = userRepository.getUserData()
                    val isLoggedIn = userData != null
                    _uiState.update { it.copy(user = userData, isLoggedIn = isLoggedIn) }
                } catch (e: Exception) {
                    // Sessizce devam et
                }
                delay(2000) // Her 2 saniyede bir
            }
        }

        viewModelScope.launch {
            while (true) {
                try {
                    // İlaç verilerini güncelle (sadece context varsa)
                    // Not: Context lazy olarak verilecek
                } catch (e: Exception) {
                    // Sessizce devam et
                }
                delay(3000) // Her 3 saniyede bir
            }
        }
    }

    /**
     * İlaçları yenile
     */
    fun refreshMedicines(context: Context?) {
        viewModelScope.launch {
            try {
                val todaysMeds = medicineRepository.getTodaysMedicines()
                _uiState.update { it.copy(todaysMedicines = todaysMeds) }

                // Context varsa upcoming ilaçları da çek
                context?.let { ctx ->
                    val upcoming = medicineRepository.getUpcomingMedicines(ctx)
                    _uiState.update {
                        it.copy(
                            allUpcomingMedicines = upcoming,
                            upcomingMedicine = upcoming.firstOrNull()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Snooze state'ini SharedPreferences'tan yükle
     */
    private fun loadSnoozeState() {
        // Not: Context gerekli, HomeScreen'den çağrılacak
    }

    fun loadSnoozeStateFromContext(context: Context) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
            val snoozeUntil = prefs.getLong("snooze_until", 0)
            val timestamp = prefs.getLong("snooze_timestamp", 0)

            if (snoozeUntil > System.currentTimeMillis()) {
                val remainingMillis = snoozeUntil - System.currentTimeMillis()
                val remainingMinutes = (remainingMillis / 60_000).toInt() + 1
                _uiState.update {
                    it.copy(
                        snoozeMinutes = remainingMinutes,
                        lastSnoozeTimestamp = timestamp
                    )
                }
            } else if (snoozeUntil > 0) {
                // Süresi dolmuş, temizle
                prefs.edit()
                    .remove("snooze_minutes")
                    .remove("snooze_until")
                    .remove("snooze_timestamp")
                    .apply()
            }
        }
    }

    /**
     * Snooze timer'ı başlat
     */
    private fun startSnoozeTimer() {
        // Not: Context gerekli, HomeScreen'den çağrılacak
    }

    fun startSnoozeTimerWithContext(context: Context) {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val prefs = context.getSharedPreferences("dozi_prefs", Context.MODE_PRIVATE)
                val snoozeUntil = prefs.getLong("snooze_until", 0)
                val timestamp = prefs.getLong("snooze_timestamp", 0)

                val currentState = _uiState.value

                if (timestamp > currentState.lastSnoozeTimestamp && snoozeUntil > System.currentTimeMillis()) {
                    val remainingMillis = snoozeUntil - System.currentTimeMillis()
                    val remainingMinutes = (remainingMillis / 60_000).toInt() + 1
                    _uiState.update {
                        it.copy(
                            snoozeMinutes = remainingMinutes,
                            lastSnoozeTimestamp = timestamp
                        )
                    }
                } else if (snoozeUntil > 0 && snoozeUntil <= System.currentTimeMillis()) {
                    _uiState.update {
                        it.copy(
                            snoozeMinutes = 0,
                            lastSnoozeTimestamp = 0
                        )
                    }
                    prefs.edit()
                        .remove("snooze_minutes")
                        .remove("snooze_until")
                        .remove("snooze_timestamp")
                        .apply()
                }
            }
        }
    }

    /**
     * İlaç alındı (Offline-first)
     * 1. Local DB'ye kaydet
     * 2. Firestore'a sync et
     * 3. Stok azalt
     */
    fun onMedicineTaken(context: Context, medicine: Medicine, time: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentMedicineStatus = MedicineStatus.TAKEN) }

            // ✅ Offline-first: MedicationLogRepository kullan
            try {
                val scheduledTime = getScheduledTimeInMillis(time)
                medicationLogRepository.logMedicationTaken(
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    dosage = medicine.dosage,
                    scheduledTime = scheduledTime,
                    notes = null
                ).onSuccess {
                    Log.d(TAG, "✅ Medication logged to Room DB and queued for sync")
                }.onFailure {
                    Log.e(TAG, "❌ Failed to log medication", it)
                }

                // ⚠️ Fallback: SharedPreferences için backward compatibility
                saveMedicineStatus(context, medicine.id, getCurrentDateString(), time, "taken")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error logging medication taken", e)
            }

            // 📦 Stok azalt (extension function kullan)
            if (medicine.autoDecrementEnabled && medicine.stockCount > 0) {
                try {
                    val updatedMedicine = medicine.decrementStock()
                    medicineRepository.updateMedicineField(medicine.id, "stockCount", updatedMedicine.stockCount)
                    Log.d(TAG, "📦 Stock decreased: ${medicine.name} -> ${updatedMedicine.stockCount} (${updatedMedicine.daysRemainingInStock()} days remaining)")

                    // ⚠️ Stok uyarıları kontrol et
                    checkStockWarnings(context, updatedMedicine)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decrease stock", e)
                }
            } else if (medicine.stockCount == 0) {
                // 🚨 Stok bitti uyarısı
                showOutOfStockNotification(context, medicine)
            }

            // 🏆 Achievement kontrolü
            checkAchievementsAfterMedicineTaken()

            // 📊 UserStats güncelle (streak, compliance, etc.)
            userStatsRepository.onMedicationTaken(medicationLogRepository)

            // 🚫 Escalation alarmlarını iptal et
            cancelEscalationAlarms(context, medicine.id, time)

            // 🚫 Tüm bildirimleri iptal et (notification drawer'dan temizle)
            com.bardino.dozi.notifications.NotificationHelper.cancelAllNotificationsForMedicine(
                context, medicine.id, time
            )

            // Success popup göster
            _uiState.update { it.copy(showSuccessPopup = true) }

            // Listeyi güncelle
            delay(100)
            val updated = medicineRepository.getUpcomingMedicines(context)
            _uiState.update {
                it.copy(
                    allUpcomingMedicines = updated,
                    upcomingMedicine = updated.firstOrNull(),
                    currentMedicineStatus = if (updated.isNotEmpty()) MedicineStatus.UPCOMING else MedicineStatus.TAKEN
                )
            }

            // Popup'ı kapat
            delay(1500)
            _uiState.update { it.copy(showSuccessPopup = false) }
        }
    }

    /**
     * İlaç atlandı (Offline-first)
     */
    fun onMedicineSkipped(context: Context, medicine: Medicine, time: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentMedicineStatus = MedicineStatus.SKIPPED,
                    showSkipDialog = false,
                    showSkippedPopup = true
                )
            }

            // ✅ Offline-first: MedicationLogRepository kullan
            try {
                val scheduledTime = getScheduledTimeInMillis(time)
                medicationLogRepository.logMedicationSkipped(
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    dosage = medicine.dosage,
                    scheduledTime = scheduledTime,
                    reason = null
                ).onSuccess {
                    Log.d(TAG, "✅ Medication skipped logged to Room DB")
                }.onFailure {
                    Log.e(TAG, "❌ Failed to log skipped medication", it)
                }

                // ⚠️ Fallback: SharedPreferences için backward compatibility
                saveMedicineStatus(context, medicine.id, getCurrentDateString(), time, "skipped")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error logging medication skipped", e)
            }

            // 🚫 Escalation alarmlarını iptal et
            cancelEscalationAlarms(context, medicine.id, time)

            // 🚫 Tüm bildirimleri iptal et (notification drawer'dan temizle)
            com.bardino.dozi.notifications.NotificationHelper.cancelAllNotificationsForMedicine(
                context, medicine.id, time
            )

            // Listeyi güncelle
            delay(100)
            val updated = medicineRepository.getUpcomingMedicines(context)
            _uiState.update {
                it.copy(
                    allUpcomingMedicines = updated,
                    upcomingMedicine = updated.firstOrNull(),
                    currentMedicineStatus = if (updated.isNotEmpty()) MedicineStatus.UPCOMING else MedicineStatus.SKIPPED
                )
            }

            // Popup'ı kapat
            delay(1500)
            _uiState.update { it.copy(showSkippedPopup = false) }
        }
    }

    /**
     * İlaç ertelendi (Offline-first)
     */
    fun onMedicineSnoozed(context: Context, medicine: Medicine, time: String, minutes: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    snoozeMinutes = minutes,
                    showSnoozeDialog = false
                )
            }

            // ✅ Offline-first: MedicationLogRepository kullan
            try {
                val scheduledTime = getScheduledTimeInMillis(time)
                medicationLogRepository.logMedicationSnoozed(
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    dosage = medicine.dosage,
                    scheduledTime = scheduledTime,
                    snoozeMinutes = minutes
                ).onSuccess {
                    Log.d(TAG, "✅ Medication snoozed logged to Room DB")
                }.onFailure {
                    Log.e(TAG, "❌ Failed to log snoozed medication", it)
                }

                // ⚠️ Fallback: SharedPreferences için backward compatibility
                val snoozeUntil = System.currentTimeMillis() + minutes * 60_000L
                saveMedicineStatus(context, medicine.id, getCurrentDateString(), time, "snoozed_$snoozeUntil")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error logging medication snoozed", e)
            }
        }
    }

    /**
     * Dialog açma/kapama
     */
    fun setShowSkipDialog(show: Boolean) {
        _uiState.update { it.copy(showSkipDialog = show) }
    }

    fun setShowSnoozeDialog(show: Boolean) {
        _uiState.update { it.copy(showSnoozeDialog = show) }
    }

    /**
     * Helper: Medicine status kaydet
     */
    private fun saveMedicineStatus(context: Context, medicineId: String, date: String, time: String, status: String) {
        val prefs = context.getSharedPreferences("medicine_status", Context.MODE_PRIVATE)
        val key = "dose_${medicineId}_${date}_${time}"
        prefs.edit().putString(key, status).commit()
        android.util.Log.d("HomeViewModel", "Status saved: $key = $status")
    }

    /**
     * Helper: Tarih string'i
     */
    private fun getCurrentDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val year = calendar.get(java.util.Calendar.YEAR)
        return "%02d/%02d/%d".format(day, month, year)
    }

    /**
     * Helper: Time string'i (HH:mm) epoch millis'e çevir
     * Örn: "08:00" -> bugünün 08:00'i için epoch millis
     */
    private fun getScheduledTimeInMillis(time: String): Long {
        return try {
            val parts = time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $time", e)
            System.currentTimeMillis()
        }
    }

    /**
     * Stok uyarılarını kontrol et
     */
    /**
     * Stok uyarılarını kontrol et (Extension fonksiyonlar kullan)
     * 24 saat içinde aynı ilaç için tekrar bildirim göndermez
     */
    private fun checkStockWarnings(context: Context, medicine: Medicine) {
        val prefs = context.getSharedPreferences("stock_warnings", Context.MODE_PRIVATE)
        val lastWarningKey = "last_warning_${medicine.id}"
        val lastWarningTime = prefs.getLong(lastWarningKey, 0)
        val currentTime = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L

        // Stok yeterli seviyedeyse, uyarı timestamp'ini temizle
        if (!medicine.isStockLow() && !medicine.isStockCritical() && !medicine.isStockEmpty()) {
            if (lastWarningTime > 0) {
                prefs.edit().remove(lastWarningKey).apply()
                Log.d(TAG, "✅ Stok yeterli seviyede, uyarı sıfırlandı: ${medicine.name}")
            }
            return
        }

        // Son 24 saat içinde bildirim gönderildiyse, tekrar gönderme
        if (currentTime - lastWarningTime < twentyFourHours) {
            Log.d(TAG, "⏱️ Stok uyarısı son 24 saat içinde gönderildi, atlanıyor: ${medicine.name}")
            return
        }

        when {
            medicine.isStockEmpty() -> {
                // 🚨 Stok bitti
                showOutOfStockNotification(context, medicine)
                Log.w(TAG, "⚠️ STOK BİTTİ: ${medicine.name}")
                // Son uyarı zamanını kaydet
                prefs.edit().putLong(lastWarningKey, currentTime).apply()
                // ✅ Firebase'e senkronize et
                syncStockWarningToFirebase(medicine.id, currentTime)
            }
            medicine.isStockCritical() -> {
                // 🔴 Kritik seviye (3 gün kaldı)
                showLowStockNotification(context, medicine)
                Log.w(TAG, "🔴 KRİTİK STOK: ${medicine.name} - ${medicine.daysRemainingInStock()} gün kaldı")
                // Son uyarı zamanını kaydet
                prefs.edit().putLong(lastWarningKey, currentTime).apply()
                // ✅ Firebase'e senkronize et
                syncStockWarningToFirebase(medicine.id, currentTime)
            }
            medicine.isStockLow() -> {
                // 🟡 Düşük stok (threshold'a göre)
                showLowStockNotification(context, medicine)
                Log.w(TAG, "🟡 DÜŞÜK STOK: ${medicine.name} - ${medicine.daysRemainingInStock()} gün kaldı (${medicine.stockCount} doz)")
                // Son uyarı zamanını kaydet
                prefs.edit().putLong(lastWarningKey, currentTime).apply()
                // ✅ Firebase'e senkronize et
                syncStockWarningToFirebase(medicine.id, currentTime)
            }
        }
    }

    /**
     * Stock warning'i Firebase'e senkronize et
     */
    private fun syncStockWarningToFirebase(medicineId: String, lastWarningTime: Long) {
        viewModelScope.launch {
            try {
                val userPrefsRepo = UserPreferencesRepository(context)
                userPrefsRepo.syncStockWarning(medicineId, lastWarningTime)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Stock warning Firebase'e senkronize edilemedi", e)
            }
        }
    }

    /**
     * Stok uyarısını manuel olarak sıfırla (stok eklendiğinde kullan)
     */
    fun resetStockWarning(medicineId: String) {
        val prefs = context.getSharedPreferences("stock_warnings", Context.MODE_PRIVATE)
        prefs.edit().remove("last_warning_$medicineId").apply()
        Log.d(TAG, "🔄 Stok uyarısı manuel olarak sıfırlandı: $medicineId")
    }

    /**
     * 🚫 Escalation alarmlarını iptal et (ilaç alındığında)
     */
    private fun cancelEscalationAlarms(context: Context, medicineId: String, time: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

            // Escalation 1, 2, 3 alarmlarını iptal et
            listOf(
                Triple("ACTION_ESCALATION_1", "escalation1", 1),
                Triple("ACTION_ESCALATION_2", "escalation2", 2),
                Triple("ACTION_ESCALATION_3", "escalation3", 3)
            ).forEach { (action, escalationType, level) ->
                val intent = android.content.Intent(context, Class.forName("com.bardino.dozi.notifications.NotificationActionReceiver")).apply {
                    this.action = action
                }
                val requestCode = "${escalationType}_${medicineId}_$time".hashCode()
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
                    } else {
                        android.app.PendingIntent.FLAG_NO_CREATE
                    }
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    Log.d(TAG, "✅ Escalation Level $level iptal edildi: $medicineId")
                }
            }
            Log.d(TAG, "🚫 Tüm escalation alarmları iptal edildi (HomeViewModel): $medicineId - $time")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Escalation alarmları iptal edilirken hata", e)
        }
    }

    /**
     * Düşük stok bildirimi göster
     */
    private fun showLowStockNotification(context: Context, medicine: Medicine) {
        try {
            val notificationHelper = Class.forName("com.bardino.dozi.notifications.NotificationHelper")
            val method = notificationHelper.getDeclaredMethod(
                "showLowStockNotification",
                Context::class.java,
                String::class.java,
                Int::class.java
            )
            method.invoke(null, context, medicine.name, medicine.stockCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show low stock notification", e)
        }
    }

    /**
     * Stok bitti bildirimi göster
     */
    private fun showOutOfStockNotification(context: Context, medicine: Medicine) {
        try {
            val notificationHelper = Class.forName("com.bardino.dozi.notifications.NotificationHelper")
            val method = notificationHelper.getDeclaredMethod(
                "showOutOfStockNotification",
                Context::class.java,
                String::class.java
            )
            method.invoke(null, context, medicine.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show out of stock notification", e)
        }
    }

    /**
     * 🏆 İlaç alındıktan sonra başarıları kontrol et
     */
    private fun checkAchievementsAfterMedicineTaken() {
        viewModelScope.launch {
            try {
                // UserStats'ı getir
                val userStats = userStatsRepository.getUserStats() ?: return@launch

                // Tüm ilaçları ve logları al
                val allMedicines = medicineRepository.getAllMedicines()
                val totalMedicines = allMedicines.size

                Log.d(TAG, "🏆 Checking achievements: streak=${userStats.currentStreak}, totalDoses=${userStats.totalMedicationsTaken}, medicines=$totalMedicines")

                // 🔥 Streak achievements
                achievementRepository.checkStreakAchievements(userStats.currentStreak)

                // 🏅 First step achievements
                // Note: hasTakenDose = true because we just took a dose (stats not yet updated)
                achievementRepository.checkFirstStepAchievements(
                    hasMedicine = totalMedicines > 0,
                    hasTakenDose = true
                )

                // 📚 Medicine collector achievements
                achievementRepository.checkMedicineCollectorAchievements(totalMedicines)

                // 💯 Total doses achievements
                achievementRepository.checkTotalDosesAchievements(userStats.totalMedicationsTaken)

                // 🎯 Perfect compliance achievements (TODO: calculate consecutive perfect days)
                // achievementRepository.checkPerfectComplianceAchievements(consecutivePerfectDays)

                Log.d(TAG, "✅ Achievement check completed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking achievements", e)
            }
        }
    }
}

enum class MedicineStatus {
    TAKEN, SKIPPED, PARTIAL, PLANNED, UPCOMING, NONE
}
