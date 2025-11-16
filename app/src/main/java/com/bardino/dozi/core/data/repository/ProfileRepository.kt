package com.bardino.dozi.core.data.repository

import com.bardino.dozi.core.data.local.dao.ProfileDao
import com.bardino.dozi.core.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user profiles
 * Handles CRUD operations for local profiles
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {

    /**
     * Get all profiles as Flow
     */
    fun getAllProfiles(): Flow<List<ProfileEntity>> {
        return profileDao.getAllProfiles()
    }

    /**
     * Get all profiles (direct access)
     */
    suspend fun getAllProfilesList(): List<ProfileEntity> {
        return profileDao.getAllProfilesList()
    }

    /**
     * Get profile by ID
     */
    suspend fun getProfileById(profileId: String): ProfileEntity? {
        return profileDao.getProfileById(profileId)
    }

    /**
     * Get currently active profile
     */
    fun getActiveProfile(): Flow<ProfileEntity?> {
        return profileDao.getActiveProfile()
    }

    /**
     * Get currently active profile (direct access)
     */
    suspend fun getActiveProfileDirect(): ProfileEntity? {
        return profileDao.getActiveProfileDirect()
    }

    /**
     * Get profile count
     */
    suspend fun getProfileCount(): Int {
        return profileDao.getProfileCount()
    }

    /**
     * Create new profile
     * @param name Profile name
     * @param avatarIcon Avatar emoji or icon
     * @param color Hex color code
     * @param setAsActive Whether to set this profile as active immediately
     * @return Created profile ID
     */
    suspend fun createProfile(
        name: String,
        avatarIcon: String = "👤",
        color: String = "#6200EE",
        setAsActive: Boolean = false
    ): String {
        val profileId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()

        val profile = ProfileEntity(
            id = profileId,
            name = name,
            avatarIcon = avatarIcon,
            color = color,
            createdAt = currentTime,
            updatedAt = currentTime,
            isActive = setAsActive
        )

        // If setting as active, deactivate others first
        if (setAsActive) {
            profileDao.deactivateAllProfiles()
        }

        profileDao.insertProfile(profile)
        return profileId
    }

    /**
     * Update profile
     */
    suspend fun updateProfile(profile: ProfileEntity) {
        val updatedProfile = profile.copy(
            updatedAt = System.currentTimeMillis()
        )
        profileDao.updateProfile(updatedProfile)
    }

    /**
     * Update profile name
     */
    suspend fun updateProfileName(profileId: String, name: String) {
        val profile = getProfileById(profileId)
        if (profile == null) {
            android.util.Log.e("ProfileRepository", "❌ updateProfileName: Profile not found: $profileId")
            return
        }
        android.util.Log.d("ProfileRepository", "🔄 Updating profile name: ${profile.name} -> $name (id: $profileId)")
        updateProfile(profile.copy(name = name))
        android.util.Log.d("ProfileRepository", "✅ Profile name updated in database")
    }

    /**
     * Update profile avatar
     */
    suspend fun updateProfileAvatar(profileId: String, avatarIcon: String) {
        val profile = getProfileById(profileId)
        if (profile == null) {
            android.util.Log.e("ProfileRepository", "❌ updateProfileAvatar: Profile not found: $profileId")
            return
        }
        android.util.Log.d("ProfileRepository", "🔄 Updating profile avatar: ${profile.avatarIcon} -> $avatarIcon (id: $profileId)")
        updateProfile(profile.copy(avatarIcon = avatarIcon))
        android.util.Log.d("ProfileRepository", "✅ Profile avatar updated in database")
    }

    /**
     * Update profile color
     */
    suspend fun updateProfileColor(profileId: String, color: String) {
        val profile = getProfileById(profileId)
        if (profile == null) {
            android.util.Log.e("ProfileRepository", "❌ updateProfileColor: Profile not found: $profileId")
            return
        }
        android.util.Log.d("ProfileRepository", "🔄 Updating profile color: ${profile.color} -> $color (id: $profileId)")
        updateProfile(profile.copy(color = color))
        android.util.Log.d("ProfileRepository", "✅ Profile color updated in database")
    }

    /**
     * Delete profile
     * Note: Should check if it's the last profile before deleting
     */
    suspend fun deleteProfile(profileId: String) {
        profileDao.deleteProfileById(profileId)
    }

    /**
     * Switch to a different profile
     * Deactivates all profiles and activates the selected one
     */
    suspend fun switchToProfile(profileId: String) {
        profileDao.setActiveProfile(profileId)
    }

    /**
     * Create default profile if no profiles exist
     * This is useful for first-time users or after app install
     */
    suspend fun ensureDefaultProfile(): String {
        val count = getProfileCount()
        if (count == 0) {
            return createProfile(
                name = "Varsayılan Profil",
                avatarIcon = "👤",
                color = "#6200EE",
                setAsActive = true
            )
        } else {
            // Ensure there's an active profile
            val activeProfile = getActiveProfileDirect()
            if (activeProfile == null) {
                val firstProfile = getAllProfilesList().firstOrNull()
                firstProfile?.let {
                    switchToProfile(it.id)
                    return it.id
                }
            }
            return activeProfile?.id ?: ""
        }
    }
}
