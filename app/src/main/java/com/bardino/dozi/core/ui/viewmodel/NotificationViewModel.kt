package com.bardino.dozi.core.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bardino.dozi.core.data.model.DoziNotification
import com.bardino.dozi.core.data.model.NotificationType
import com.bardino.dozi.core.data.repository.NotificationRepository
import com.bardino.dozi.core.data.repository.BadiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bildirim UI durumu
 */
data class NotificationUiState(
    val notifications: List<DoziNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Notification ViewModel
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val badiRepository: BadiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        loadUnreadCount()
    }

    // ==================== Bildirim İşlemleri ====================

    /**
     * Bildirimleri yükle
     */
    private fun loadNotifications() {
        viewModelScope.launch {
            notificationRepository.getNotificationsFlow()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { notifications ->
                    _uiState.update { it.copy(notifications = notifications) }
                }
        }
    }

    /**
     * Okunmamış bildirim sayısını yükle
     */
    private fun loadUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCountFlow()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { count ->
                    _uiState.update { it.copy(unreadCount = count) }
                }
        }
    }

    /**
     * Bildirimi okundu olarak işaretle
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * Tüm bildirimleri okundu olarak işaretle
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            notificationRepository.markAllAsRead()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Bildirimi sil
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * FCM token'ı yenile
     */
    fun refreshFCMToken() {
        viewModelScope.launch {
            notificationRepository.refreshFCMToken()
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * Local notification göster
     */
    fun showLocalNotification(
        context: Context,
        title: String,
        body: String,
        type: NotificationType = NotificationType.GENERAL
    ) {
        viewModelScope.launch {
            notificationRepository.showLocalNotification(context, title, body, type)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * İlaç hatırlatması için badilere bildirim gönder
     */
    fun sendMedicationReminderToBadis(
        medicineId: String,
        medicineName: String,
        dosage: String,
        time: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            notificationRepository.sendMedicationReminderToBadis(
                medicineId, medicineName, dosage, time
            )
                .onSuccess { sentCount ->
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Badi isteğini kabul et
     */
    fun acceptBadiRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            badiRepository.acceptBadiRequest(requestId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Badi isteğini reddet
     */
    fun rejectBadiRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            badiRepository.rejectBadiRequest(requestId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    // ==================== Yardımcı Fonksiyonlar ====================

    /**
     * Hata mesajını temizle
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Bildirimleri türe göre filtrele
     */
    fun getNotificationsByType(type: NotificationType): List<DoziNotification> {
        return _uiState.value.notifications.filter { it.type == type }
    }

    /**
     * Okunmamış bildirimleri getir
     */
    fun getUnreadNotifications(): List<DoziNotification> {
        return _uiState.value.notifications.filter { !it.isRead }
    }
}
