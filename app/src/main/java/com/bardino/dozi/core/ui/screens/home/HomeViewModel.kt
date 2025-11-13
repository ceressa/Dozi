package com.bardino.dozi.core.ui.screens.home

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bardino.dozi.core.data.model.Medicine
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.MedicineRepository
import com.bardino.dozi.core.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * HomeScreen için ViewModel
 * State management ve business logic burada
 */
@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(
    private val medicineRepository: MedicineRepository = MedicineRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    /**
     * UI State data class
     * Tüm UI state'leri tek bir yerde
     */
    data class HomeUiState(
        val user: User? = null,
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
        startPollingData()
        loadSnoozeState()
        startSnoozeTimer()
    }

    /**
     * Kullanıcı ve ilaç verilerini yükle
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                // Kullanıcı verilerini çek
                val userData = userRepository.getUserData()
                _uiState.update { it.copy(user = userData) }

                // İlaç verilerini çek
                refreshMedicines(null)

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
                    _uiState.update { it.copy(user = userData) }
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
     * İlaç alındı
     */
    fun onMedicineTaken(context: Context, medicine: Medicine, time: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentMedicineStatus = MedicineStatus.TAKEN) }

            // Durumu kaydet
            saveMedicineStatus(context, medicine.id, getCurrentDateString(), time, "taken")

            // Stok azalt (eğer stok > 0 ise)
            if (medicine.stockCount > 0) {
                try {
                    val newStockCount = medicine.stockCount - 1
                    medicineRepository.updateMedicineField(medicine.id, "stockCount", newStockCount)
                    android.util.Log.d("HomeViewModel", "Stock decreased: ${medicine.name} -> $newStockCount")
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Failed to decrease stock", e)
                }
            }

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
     * İlaç atlandı
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

            // Durumu kaydet
            saveMedicineStatus(context, medicine.id, getCurrentDateString(), time, "skipped")

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
     * İlaç ertelendi
     */
    fun onMedicineSnoozed(context: Context, medicine: Medicine, time: String, minutes: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    snoozeMinutes = minutes,
                    showSnoozeDialog = false
                )
            }

            // Durumu kaydet
            val snoozeUntil = System.currentTimeMillis() + minutes * 60_000L
            saveMedicineStatus(context, medicine.id, getCurrentDateString(), time, "snoozed_$snoozeUntil")
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
}

enum class MedicineStatus {
    TAKEN, SKIPPED, PLANNED, UPCOMING, NONE
}
