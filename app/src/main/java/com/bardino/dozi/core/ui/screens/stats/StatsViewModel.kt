package com.bardino.dozi.core.ui.screens.stats

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bardino.dozi.core.data.model.UserStats
import com.bardino.dozi.core.data.model.Achievement
import com.bardino.dozi.core.data.repository.UserStatsRepository
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.AchievementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class StatsViewModel(application: Application) : AndroidViewModel(application) {

    data class StatsUiState(
        val stats: UserStats? = null,
        val weeklyLogs: List<DayLog> = emptyList(),
        val achievements: List<Achievement> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val achievementRepository = AchievementRepository()
    private val medicationLogRepository = MedicationLogRepository(application)
    private val userStatsRepository = UserStatsRepository(achievementRepository)

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // UserStats yükle
            val stats = userStatsRepository.getUserStats()

            // Haftalık gerçek veriyi Firestore'dan çek
            val weeklyLogs = loadWeeklyLogs()

            // 🏆 Başarıları yükle
            val achievements = achievementRepository.getAllAchievements()

            _uiState.update {
                it.copy(
                    stats = stats,
                    weeklyLogs = weeklyLogs,
                    achievements = achievements,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Firestore'dan gerçek haftalık ilaç loglarını çek
     */
    private suspend fun loadWeeklyLogs(): List<DayLog> {
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale("tr"))

        // Firestore'dan son 7 günün verilerini al
        val dailyLogs = medicationLogRepository.getWeeklyMedicationLogs()

        // DailyMedicationLogs'u DayLog'a dönüştür
        return dailyLogs.reversed().map { dailyLog ->
            // Tarih string'ini parse et
            val localDate = LocalDate.parse(dailyLog.date)
            val dayName = localDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("tr"))
            val dateStr = localDate.format(dateFormatter)

            DayLog(
                date = dateStr,
                dayName = dayName,
                taken = dailyLog.takenCount,
                total = dailyLog.totalCount
            )
        }
    }

    /**
     * İstatistikleri yenile
     */
    fun refresh() {
        loadStats()
    }
}
