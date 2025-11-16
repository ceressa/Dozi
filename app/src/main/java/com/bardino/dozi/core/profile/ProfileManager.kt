package com.bardino.dozi.core.profile

import com.bardino.dozi.core.data.local.entity.ProfileEntity
import com.bardino.dozi.core.data.repository.ProfileRepository
import com.bardino.dozi.core.premium.PremiumManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 👥 Profile Manager
 *
 * Manages multi-user profiles for the application
 * Premium feature: 1 profile is free, 2+ profiles require premium
 */
@Singleton
class ProfileManager @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val premiumManager: PremiumManager
) {

    companion object {
        const val MAX_FREE_PROFILES = 1
        const val DEFAULT_PROFILE_NAME = "Varsayılan Profil"
        const val DEFAULT_AVATAR = "👤"
        const val DEFAULT_COLOR = "#6200EE"
    }

    /**
     * Get all profiles as Flow
     */
    fun getAllProfiles(): Flow<List<ProfileEntity>> {
        return profileRepository.getAllProfiles()
    }

    /**
     * Get currently active profile
     */
    fun getActiveProfile(): Flow<ProfileEntity?> {
        return profileRepository.getActiveProfile()
    }

    /**
     * Get currently active profile (direct access)
     */
    suspend fun getActiveProfileDirect(): ProfileEntity? {
        return profileRepository.getActiveProfileDirect()
    }

    /**
     * Get active profile ID
     * Returns empty string if no active profile
     */
    suspend fun getActiveProfileId(): String {
        return getActiveProfileDirect()?.id ?: ""
    }

    /**
     * Get profile by ID
     */
    suspend fun getProfileById(profileId: String): ProfileEntity? {
        return profileRepository.getProfileById(profileId)
    }

    /**
     * Check if user can create a new profile
     * Free users: 1 profile max
     * Premium users: unlimited profiles
     *
     * @return Pair<Boolean, String> (canCreate, reason)
     */
    suspend fun canCreateNewProfile(): Pair<Boolean, String> {
        val profileCount = profileRepository.getProfileCount()
        val isPremium = premiumManager.isPremium()

        return when {
            // Free users can only have 1 profile
            !isPremium && profileCount >= MAX_FREE_PROFILES -> {
                Pair(false, "Birden fazla profil premium üyelik gerektirir")
            }
            // Premium users can create unlimited profiles
            else -> {
                Pair(true, "")
            }
        }
    }

    /**
     * Create a new profile
     *
     * @param name Profile name
     * @param avatarIcon Avatar emoji or icon
     * @param color Hex color code
     * @param setAsActive Whether to set this profile as active
     * @return Result with profile ID or error message
     */
    suspend fun createProfile(
        name: String,
        avatarIcon: String = DEFAULT_AVATAR,
        color: String = DEFAULT_COLOR,
        setAsActive: Boolean = false
    ): Result<String> {
        // Check if user can create new profile
        val (canCreate, reason) = canCreateNewProfile()
        if (!canCreate) {
            return Result.failure(PremiumRequiredException(reason))
        }

        // Validate input
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Profil adı boş olamaz"))
        }

        return try {
            val profileId = profileRepository.createProfile(
                name = name,
                avatarIcon = avatarIcon,
                color = color,
                setAsActive = setAsActive
            )
            Result.success(profileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update profile
     */
    suspend fun updateProfile(profile: ProfileEntity): Result<Unit> {
        return try {
            profileRepository.updateProfile(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update profile name
     */
    suspend fun updateProfileName(profileId: String, name: String): Result<Unit> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Profil adı boş olamaz"))
        }

        return try {
            profileRepository.updateProfileName(profileId, name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update profile avatar
     */
    suspend fun updateProfileAvatar(profileId: String, avatarIcon: String): Result<Unit> {
        return try {
            profileRepository.updateProfileAvatar(profileId, avatarIcon)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update profile color
     */
    suspend fun updateProfileColor(profileId: String, color: String): Result<Unit> {
        return try {
            profileRepository.updateProfileColor(profileId, color)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a profile
     * Note: Should not allow deleting the last profile
     */
    suspend fun deleteProfile(profileId: String): Result<Unit> {
        val profileCount = profileRepository.getProfileCount()

        // Don't allow deleting the last profile
        if (profileCount <= 1) {
            return Result.failure(IllegalStateException("Son profil silinemez"))
        }

        return try {
            val isActiveProfile = getActiveProfileDirect()?.id == profileId

            profileRepository.deleteProfile(profileId)

            // If we deleted the active profile, switch to the first available one
            if (isActiveProfile) {
                val firstProfile = profileRepository.getAllProfilesList().firstOrNull()
                firstProfile?.let {
                    profileRepository.switchToProfile(it.id)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Switch to a different profile
     * @param profileId Profile ID to switch to
     * @param pinCode PIN code if profile is protected (hashed)
     */
    suspend fun switchToProfile(profileId: String, pinCode: String? = null): Result<Unit> {
        return try {
            // Check if profile requires PIN
            val profile = profileRepository.getProfileById(profileId)
            if (profile?.pinCode != null && pinCode != profile.pinCode) {
                return Result.failure(PinRequiredException("PIN kodu gerekli"))
            }

            profileRepository.switchToProfile(profileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set PIN for a profile
     */
    suspend fun setProfilePin(profileId: String, pinCode: String): Result<Unit> {
        return try {
            val profile = getProfileById(profileId) ?: return Result.failure(
                IllegalArgumentException("Profil bulunamadı")
            )
            profileRepository.updateProfile(profile.copy(pinCode = pinCode))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove PIN from a profile
     */
    suspend fun removeProfilePin(profileId: String): Result<Unit> {
        return try {
            val profile = getProfileById(profileId) ?: return Result.failure(
                IllegalArgumentException("Profil bulunamadı")
            )
            profileRepository.updateProfile(profile.copy(pinCode = null))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if profile has PIN
     */
    suspend fun hasPin(profileId: String): Boolean {
        val profile = getProfileById(profileId)
        return profile?.pinCode != null
    }

    /**
     * Ensure default profile exists
     * Call this on app startup
     */
    suspend fun ensureDefaultProfile(): String {
        return profileRepository.ensureDefaultProfile()
    }

    /**
     * Get profile count
     */
    suspend fun getProfileCount(): Int {
        return profileRepository.getProfileCount()
    }
}

/**
 * Exception thrown when a premium feature is required
 */
class PremiumRequiredException(message: String) : Exception(message)

/**
 * Exception thrown when PIN is required
 */
class PinRequiredException(message: String) : Exception(message)
