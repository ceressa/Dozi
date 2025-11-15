package com.bardino.dozi.core.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Notification Settings UI State
 */
data class NotificationSettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    // DND Settings
    val dndEnabled: Boolean = false,
    val dndStartHour: Int = 22,
    val dndStartMinute: Int = 0,
    val dndEndHour: Int = 8,
    val dndEndMinute: Int = 0,

    // Adaptive Timing
    val adaptiveTimingEnabled: Boolean = false,
    val preferredMorningHour: Int = 8,
    val preferredEveningHour: Int = 20,

    // Smart Reminder
    val smartReminderEnabled: Boolean = false
)

/**
 * Notification Settings ViewModel
 */
class NotificationSettingsViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserSettings()
    }

    /**
     * Kullanıcı ayarlarını yükle
     */
    private fun loadUserSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val user = userRepository.getUserData()
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            user = user,
                            dndEnabled = user.dndEnabled,
                            dndStartHour = user.dndStartHour,
                            dndStartMinute = user.dndStartMinute,
                            dndEndHour = user.dndEndHour,
                            dndEndMinute = user.dndEndMinute,
                            adaptiveTimingEnabled = user.adaptiveTimingEnabled,
                            preferredMorningHour = user.preferredMorningHour,
                            preferredEveningHour = user.preferredEveningHour,
                            smartReminderEnabled = user.smartReminderEnabled,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Kullanıcı bilgisi bulunamadı"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Bilinmeyen hata"
                    )
                }
            }
        }
    }

    // ==================== DND Settings ====================

    fun updateDndEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userRepository.updateUserField("dndEnabled", enabled)
                _uiState.update { it.copy(dndEnabled = enabled) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateDndStartTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "dndStartHour" to hour,
                    "dndStartMinute" to minute
                )
                userRepository.updateUserField("dndStartHour", hour)
                userRepository.updateUserField("dndStartMinute", minute)
                _uiState.update {
                    it.copy(
                        dndStartHour = hour,
                        dndStartMinute = minute
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateDndEndTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                userRepository.updateUserField("dndEndHour", hour)
                userRepository.updateUserField("dndEndMinute", minute)
                _uiState.update {
                    it.copy(
                        dndEndHour = hour,
                        dndEndMinute = minute
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ==================== Adaptive Timing ====================

    fun updateAdaptiveTimingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userRepository.updateUserField("adaptiveTimingEnabled", enabled)
                _uiState.update { it.copy(adaptiveTimingEnabled = enabled) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updatePreferredMorningHour(hour: Int) {
        viewModelScope.launch {
            try {
                userRepository.updateUserField("preferredMorningHour", hour)
                _uiState.update { it.copy(preferredMorningHour = hour) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updatePreferredEveningHour(hour: Int) {
        viewModelScope.launch {
            try {
                userRepository.updateUserField("preferredEveningHour", hour)
                _uiState.update { it.copy(preferredEveningHour = hour) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ==================== Smart Reminder ====================

    fun updateSmartReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userRepository.updateUserField("smartReminderEnabled", enabled)
                _uiState.update { it.copy(smartReminderEnabled = enabled) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Hata mesajını temizle
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
