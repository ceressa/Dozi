package com.bardino.dozi.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bardino.dozi.core.data.model.*
import com.bardino.dozi.core.data.repository.BadiRepository
import com.bardino.dozi.core.data.repository.MedicationLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Badi sistem UI durumu
 */
data class BadiUiState(
    val badis: List<BadiWithUser> = emptyList(),
    val pendingRequests: List<BadiRequestWithUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val badiCode: String? = null
)

/**
 * Badi arama durumu
 */
data class BadiSearchState(
    val searchQuery: String = "",
    val foundUser: User? = null,
    val isSearching: Boolean = false,
    val error: String? = null
)

/**
 * Badi ViewModel
 */
@HiltViewModel
class BadiViewModel @Inject constructor(
    private val badiRepository: BadiRepository,
    private val medicationLogRepository: MedicationLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BadiUiState())
    val uiState: StateFlow<BadiUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow(BadiSearchState())
    val searchState: StateFlow<BadiSearchState> = _searchState.asStateFlow()

    private val _selectedBadiLogs = MutableStateFlow<List<MedicationLog>>(emptyList())
    val selectedBadiLogs: StateFlow<List<MedicationLog>> = _selectedBadiLogs.asStateFlow()

    // Expose badis for direct access
    val badis: StateFlow<List<BadiWithUser>> = _uiState.map { it.badis }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        loadBadis()
        loadPendingRequests()
        // Duplicate ve stale kayıtları temizle
        cleanupData()
    }

    /**
     * Duplicate badi ve stale request kayıtlarını temizle
     */
    private fun cleanupData() {
        viewModelScope.launch {
            // Stale request'leri temizle (ÖNCE!)
            badiRepository.cleanupStaleRequests()
                .onSuccess { deletedCount ->
                    if (deletedCount > 0) {
                        android.util.Log.d("BadiViewModel", "🧹 Cleaned up $deletedCount stale requests")
                    }
                }
                .onFailure { error ->
                    android.util.Log.e("BadiViewModel", "Request cleanup failed", error as? Throwable)
                }

            // Duplicate badileri temizle
            badiRepository.cleanupDuplicateBadis()
                .onSuccess { deletedCount ->
                    if (deletedCount > 0) {
                        android.util.Log.d("BadiViewModel", "🧹 Cleaned up $deletedCount duplicate badis")
                    }
                }
                .onFailure { error ->
                    android.util.Log.e("BadiViewModel", "Badi cleanup failed", error as? Throwable)
                }

            // 🔥 Self-buddy kayıtlarını temizle
            badiRepository.cleanupSelfBuddies()
                .onSuccess { deletedCount ->
                    if (deletedCount > 0) {
                        android.util.Log.d("BadiViewModel", "🧹 Cleaned up $deletedCount self-buddy records")
                    }
                }
                .onFailure { error ->
                    android.util.Log.e("BadiViewModel", "Self-buddy cleanup failed", error as? Throwable)
                }
        }
    }

    // ==================== Badi İşlemleri ====================

    /**
     * Badileri yükle
     */
    private fun loadBadis() {
        viewModelScope.launch {
            badiRepository.getBadisFlow()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { badis ->
                    _uiState.update { it.copy(badis = badis) }
                }
        }
    }

    /**
     * Bekleyen istekleri yükle
     */
    private fun loadPendingRequests() {
        viewModelScope.launch {
            android.util.Log.d("BadiViewModel", "Starting to load pending requests...")
            badiRepository.getPendingBadiRequestsFlow()
                .catch { error ->
                    android.util.Log.e("BadiViewModel", "Error loading pending requests", error)
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { requests ->
                    android.util.Log.d("BadiViewModel", "Received ${requests.size} pending requests in ViewModel")
                    _uiState.update { it.copy(pendingRequests = requests) }
                }
        }
    }

    /**
     * Badi izinlerini güncelle
     */
    fun updateBadiPermissions(badiId: String, permissions: BadiPermissions) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            badiRepository.updateBadiPermissions(badiId, permissions)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Badi bildirim tercihlerini güncelle
     */
    fun updateBadiNotificationPreferences(
        badiId: String,
        preferences: BadiNotificationPreferences
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            badiRepository.updateBadiNotificationPreferences(badiId, preferences)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Badiye nickname ekle
     */
    fun updateBadiNickname(badiId: String, nickname: String?) {
        viewModelScope.launch {
            badiRepository.updateBadiNickname(badiId, nickname)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * Badiyi kaldır
     */
    fun removeBadi(badiId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            badiRepository.removeBadi(badiId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    // ==================== Badi İstekleri ====================

    /**
     * Badi kodu oluştur
     */
    fun generateBadiCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val code = badiRepository.generateBadiCode()
            _uiState.update { it.copy(isLoading = false, badiCode = code) }
        }
    }

    /**
     * Badi kodu ile kullanıcı ara
     */
    fun searchUserByBadiCode(code: String) {
        viewModelScope.launch {
            _searchState.update { it.copy(isSearching = true, searchQuery = code) }
            val user = badiRepository.findUserByBadiCode(code)
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
            val user = badiRepository.findUserByEmail(email)
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
     * Badi isteği gönder
     */
    fun sendBadiRequest(toUserId: String, message: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            badiRepository.sendBadiRequest(toUserId, message)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    _searchState.update { BadiSearchState() } // Reset search state
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

    // ==================== Badi İlaç Takibi ====================

    /**
     * Badinin ilaç geçmişini yükle
     */
    fun loadBadiMedicationLogs(badiUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val logs = medicationLogRepository.getBuddyMedicationLogs(badiUserId)
            _selectedBadiLogs.value = logs
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ==================== Badi Yönetimi ====================

    /**
     * Badi rolünü güncelle
     */
    fun updateBadiRole(badiId: String, newRole: BadiRole) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            badiRepository.updateBadiPermissions(badiId, BadiPermissions.fromRole(newRole))
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    /**
     * Badi durumunu güncelle (aktif/duraklatılmış)
     */
    fun updateBadiStatus(badiId: String, status: BadiStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            badiRepository.updateBadiStatus(badiId, status)
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
     * Arama durumunu sıfırla
     */
    fun clearSearchState() {
        _searchState.value = BadiSearchState()
    }

    /**
     * Hata mesajını temizle
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Mevcut badi sayısını döndür
     */
    suspend fun getBadiCount(): Int {
        return badiRepository.getActiveBadiCount()
    }
}
