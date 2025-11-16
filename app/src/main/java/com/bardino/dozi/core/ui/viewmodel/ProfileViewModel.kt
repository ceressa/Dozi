package com.bardino.dozi.core.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bardino.dozi.core.data.local.entity.ProfileEntity
import com.bardino.dozi.core.data.model.ProfileStats
import com.bardino.dozi.core.data.repository.ProfileStatsRepository
import com.bardino.dozi.core.premium.PremiumManager
import com.bardino.dozi.core.profile.ProfileManager
import com.bardino.dozi.core.profile.PremiumRequiredException
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for profile management
 */
data class ProfileUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfile: ProfileEntity? = null,
    val profileStats: Map<String, ProfileStats> = emptyMap(),  // profileId -> stats
    val isLoading: Boolean = false,
    val isLoadingStats: Boolean = false,
    val error: String? = null,
    val isPremium: Boolean = false,
    val canAddMoreProfiles: Boolean = false
)

/**
 * ViewModel for managing user profiles
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileManager: ProfileManager,
    private val premiumManager: PremiumManager,
    private val profileStatsRepository: ProfileStatsRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Direct access to profiles list
    val profiles: StateFlow<List<ProfileEntity>> = _uiState.map { it.profiles }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Direct access to active profile
    val activeProfile: StateFlow<ProfileEntity?> = _uiState.map { it.activeProfile }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    init {
        loadProfiles()
        loadActiveProfile()
        checkPremiumStatus()
    }

    /**
     * Load statistics for a specific profile
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadProfileStats(profileId: String, profileName: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true) }
            try {
                val stats = profileStatsRepository.getProfileStats(userId, profileId, profileName)
                _uiState.update { state ->
                    val updatedStats = state.profileStats.toMutableMap()
                    updatedStats[profileId] = stats
                    state.copy(
                        profileStats = updatedStats,
                        isLoadingStats = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingStats = false, error = e.message) }
            }
        }
    }

    /**
     * Load statistics for all profiles
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadAllProfilesStats() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true) }
            try {
                val profiles = _uiState.value.profiles.map { it.id to it.name }
                val allStats = profileStatsRepository.getAllProfilesStats(userId, profiles)

                val statsMap = allStats.associateBy { it.profileId }
                _uiState.update { state ->
                    state.copy(
                        profileStats = statsMap,
                        isLoadingStats = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingStats = false, error = e.message) }
            }
        }
    }

    /**
     * Load all profiles
     */
    private fun loadProfiles() {
        viewModelScope.launch {
            profileManager.getAllProfiles()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { profiles ->
                    _uiState.update { state ->
                        state.copy(
                            profiles = profiles,
                            isLoading = false
                        )
                    }
                    updateCanAddMoreProfiles()
                }
        }
    }

    /**
     * Load active profile
     */
    private fun loadActiveProfile() {
        viewModelScope.launch {
            profileManager.getActiveProfile()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { profile ->
                    _uiState.update { it.copy(activeProfile = profile) }
                }
        }
    }

    /**
     * Check premium status
     */
    private fun checkPremiumStatus() {
        viewModelScope.launch {
            premiumManager.isPremiumFlow()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { isPremium ->
                    _uiState.update { it.copy(isPremium = isPremium) }
                    updateCanAddMoreProfiles()
                }
        }
    }

    /**
     * Update whether user can add more profiles
     */
    private suspend fun updateCanAddMoreProfiles() {
        val (canCreate, _) = profileManager.canCreateNewProfile()
        _uiState.update { it.copy(canAddMoreProfiles = canCreate) }
    }

    /**
     * Create a new profile
     */
    fun createProfile(
        name: String,
        avatarIcon: String = "👤",
        color: String = "#6200EE",
        setAsActive: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            profileManager.createProfile(
                name = name,
                avatarIcon = avatarIcon,
                color = color,
                setAsActive = setAsActive
            ).fold(
                onSuccess = { profileId ->
                    _uiState.update { it.copy(isLoading = false, error = null) }
                },
                onFailure = { error ->
                    val errorMessage = when (error) {
                        is PremiumRequiredException -> error.message
                        is IllegalArgumentException -> error.message
                        else -> "Profil oluşturulurken bir hata oluştu"
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                }
            )
        }
    }

    /**
     * Update profile name
     */
    fun updateProfileName(profileId: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            profileManager.updateProfileName(profileId, name).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "İsim güncellenirken bir hata oluştu"
                        )
                    }
                }
            )
        }
    }

    /**
     * Update profile avatar
     */
    fun updateProfileAvatar(profileId: String, avatarIcon: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            profileManager.updateProfileAvatar(profileId, avatarIcon).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Avatar güncellenirken bir hata oluştu"
                        )
                    }
                }
            )
        }
    }

    /**
     * Update profile color
     */
    fun updateProfileColor(profileId: String, color: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            profileManager.updateProfileColor(profileId, color).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Renk güncellenirken bir hata oluştu"
                        )
                    }
                }
            )
        }
    }

    /**
     * Switch to a different profile
     */
    fun switchToProfile(profileId: String, pinCode: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            profileManager.switchToProfile(profileId, pinCode).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Profil değiştirilirken bir hata oluştu"
                        )
                    }
                }
            )
        }
    }

    /**
     * Set PIN for profile
     */
    fun setProfilePin(profileId: String, pinCode: String) {
        viewModelScope.launch {
            profileManager.setProfilePin(profileId, pinCode).fold(
                onSuccess = {},
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    /**
     * Remove PIN from profile
     */
    fun removeProfilePin(profileId: String) {
        viewModelScope.launch {
            profileManager.removeProfilePin(profileId).fold(
                onSuccess = {},
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    /**
     * Check if profile has PIN
     */
    suspend fun hasPin(profileId: String): Boolean {
        return profileManager.hasPin(profileId)
    }

    /**
     * Delete a profile
     */
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            profileManager.deleteProfile(profileId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                },
                onFailure = { error ->
                    val errorMessage = when (error) {
                        is IllegalStateException -> error.message
                        else -> "Profil silinirken bir hata oluştu"
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                }
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Ensure default profile exists
     */
    fun ensureDefaultProfile() {
        viewModelScope.launch {
            profileManager.ensureDefaultProfile()
        }
    }
}
