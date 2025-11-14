package com.bardino.dozi.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bardino.dozi.core.data.model.*
import com.bardino.dozi.core.data.repository.BuddyRepository
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Buddy sistem UI durumu
 */
data class BuddyUiState(
    val buddies: List<BuddyWithUser> = emptyList(),
    val pendingRequests: List<BuddyRequestWithUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val buddyCode: String? = null
)

/**
 * Buddy arama durumu
 */
data class BuddySearchState(
    val searchQuery: String = "",
    val foundUser: User? = null,
    val isSearching: Boolean = false,
    val error: String? = null
)

/**
 * Buddy ViewModel
 */
@HiltViewModel
class BuddyViewModel @Inject constructor(
    private val buddyRepository: BuddyRepository,
    private val medicationLogRepository: MedicationLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuddyUiState())
    val uiState: StateFlow<BuddyUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow(BuddySearchState())
    val searchState: StateFlow<BuddySearchState> = _searchState.asStateFlow()

    private val _selectedBuddyLogs = MutableStateFlow<List<MedicationLog>>(emptyList())
    val selectedBuddyLogs: StateFlow<List<MedicationLog>> = _selectedBuddyLogs.asStateFlow()

    init {
        loadBuddies()
        loadPendingRequests()
    }

    // ==================== Buddy İşlemleri ====================

    /**
     * Buddy'leri yükle
     */
    private fun loadBuddies() {
        viewModelScope.launch {
            buddyRepository.getBuddiesFlow()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { buddies ->
                    _uiState.update { it.copy(buddies = buddies) }
                }
        }
    }

    /**
     * Bekleyen istekleri yükle
     */
    private fun loadPendingRequests() {
        viewModelScope.launch {
            android.util.Log.d("BuddyViewModel", "Starting to load pending requests...")
            buddyRepository.getPendingBuddyRequestsFlow()
                .catch { error ->
                    android.util.Log.e("BuddyViewModel", "Error loading pending requests", error)
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { requests ->
                    android.util.Log.d("BuddyViewModel", "Received ${requests.size} pending requests in ViewModel")
                    _uiState.update { it.copy(pendingRequests = requests) }
                }
        }
    }

    /**
     * Buddy izinlerini güncelle
     */
    fun updateBuddyPermissions(buddyId: String, permissions: BuddyPermissions) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            buddyRepository.updateBuddyPermissions(buddyId, permissions)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Buddy bildirim tercihlerini güncelle
     */
    fun updateBuddyNotificationPreferences(
        buddyId: String,
        preferences: BuddyNotificationPreferences
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            buddyRepository.updateBuddyNotificationPreferences(buddyId, preferences)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Buddy'ye nickname ekle
     */
    fun updateBuddyNickname(buddyId: String, nickname: String?) {
        viewModelScope.launch {
            buddyRepository.updateBuddyNickname(buddyId, nickname)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * Buddy'yi kaldır
     */
    fun removeBuddy(buddyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            buddyRepository.removeBuddy(buddyId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    // ==================== Buddy İstekleri ====================

    /**
     * Buddy kodu oluştur
     */
    fun generateBuddyCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val code = buddyRepository.generateBuddyCode()
            _uiState.update { it.copy(isLoading = false, buddyCode = code) }
        }
    }

    /**
     * Buddy kodu ile kullanıcı ara
     */
    fun searchUserByBuddyCode(code: String) {
        viewModelScope.launch {
            _searchState.update { it.copy(isSearching = true, searchQuery = code) }
            val user = buddyRepository.findUserByBuddyCode(code)
            if (user != null) {
                _searchState.update { it.copy(isSearching = false, foundUser = user, error = null) }
            } else {
                _searchState.update {
                    it.copy(
                        isSearching = false,
                        foundUser = null,
                        error = "Kullanıcı bulunamadı"
                    )
                }
            }
        }
    }

    /**
     * Email ile kullanıcı ara
     */
    fun searchUserByEmail(email: String) {
        viewModelScope.launch {
            _searchState.update { it.copy(isSearching = true, searchQuery = email) }
            val user = buddyRepository.findUserByEmail(email)
            if (user != null) {
                _searchState.update { it.copy(isSearching = false, foundUser = user, error = null) }
            } else {
                _searchState.update {
                    it.copy(
                        isSearching = false,
                        foundUser = null,
                        error = "Kullanıcı bulunamadı"
                    )
                }
            }
        }
    }

    /**
     * Buddy isteği gönder
     */
    fun sendBuddyRequest(toUserId: String, message: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            buddyRepository.sendBuddyRequest(toUserId, message)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    _searchState.update { BuddySearchState() } // Reset search state
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Buddy isteğini kabul et
     */
    fun acceptBuddyRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            buddyRepository.acceptBuddyRequest(requestId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Buddy isteğini reddet
     */
    fun rejectBuddyRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            buddyRepository.rejectBuddyRequest(requestId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    // ==================== Buddy İlaç Takibi ====================

    /**
     * Buddy'nin ilaç geçmişini yükle
     */
    fun loadBuddyMedicationLogs(buddyUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val logs = medicationLogRepository.getBuddyMedicationLogs(buddyUserId)
            _selectedBuddyLogs.value = logs
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ==================== Yardımcı Fonksiyonlar ====================

    /**
     * Arama durumunu sıfırla
     */
    fun clearSearchState() {
        _searchState.value = BuddySearchState()
    }

    /**
     * Hata mesajını temizle
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
