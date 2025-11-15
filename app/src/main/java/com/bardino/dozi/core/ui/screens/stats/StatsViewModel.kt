package com.bardino.dozi.core.ui.screens.stats

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bardino.dozi.core.data.model.UserStats
import com.bardino.dozi.core.data.repository.UserStatsRepository
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import android.content.Context
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.O)
class StatsViewModel : ViewModel() {

    data class StatsUiState(
        val stats: UserStats? = null,
        val weeklyLogs: List<DayLog> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val userStatsRepository = UserStatsRepository()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // UserStats yükle
            val stats = userStatsRepository.getUserStats()

            // Haftalık dummy data (gerçek implementasyon için MedicationLogRepository kullanılabilir)
            val weeklyLogs = generateWeeklyLogs()

            _uiState.update {
                it.copy(
                    stats = stats,
                    weeklyLogs = weeklyLogs,
                    isLoading = false
                )
            }
        }
    }

    private fun generateWeeklyLogs(): List<DayLog> {
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale("tr"))

        return (0..6).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("tr"))
            val dateStr = date.format(dateFormatter)

            // Dummy data - gerçek implementasyonda MedicationLogRepository'den gelecek
            val taken = (3..5).random()
            val total = 5

            DayLog(
                date = dateStr,
                dayName = dayName,
                taken = taken,
                total = total
            )
        }.reversed()
    }
}
