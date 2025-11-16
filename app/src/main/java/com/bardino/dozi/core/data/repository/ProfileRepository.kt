package com.bardino.dozi.core.data.repository

import com.bardino.dozi.core.data.local.dao.ProfileDao
import com.bardino.dozi.core.data.local.entity.ProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user profiles
 * Handles CRUD operations for local profiles and syncs with Firestore
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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

        // Save to local database
        profileDao.insertProfile(profile)

        // Save to Firestore
        saveProfileToFirestore(profile)

        android.util.Log.d("ProfileRepository", "✅ Profile created and saved to both Room and Firestore: $name ($profileId)")

        return profileId
    }

    /**
     * Update profile
     */
    suspend fun updateProfile(profile: ProfileEntity) {
        android.util.Log.d("ProfileRepository", "📝 Updating profile: ${profile.name} (${profile.id})")
        val updatedProfile = profile.copy(
            updatedAt = System.currentTimeMillis()
        )

        // Update in local database
        profileDao.updateProfile(updatedProfile)

        // Update in Firestore
        saveProfileToFirestore(updatedProfile)

        android.util.Log.d("ProfileRepository", "✅ Profile DAO update completed - Room should trigger Flow")

        // Debug: Verify update
        val verifyProfile = profileDao.getProfileById(profile.id)
        android.util.Log.d("ProfileRepository", "🔍 Verification: name=${verifyProfile?.name}, avatar=${verifyProfile?.avatarIcon}, color=${verifyProfile?.color}")
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
        // Delete from local database
        profileDao.deleteProfileById(profileId)

        // Delete from Firestore
        deleteProfileFromFirestore(profileId)

        android.util.Log.d("ProfileRepository", "✅ Profile deleted from both Room and Firestore: $profileId")
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
     * Uses provided userName if available
     */
    suspend fun ensureDefaultProfile(userName: String? = null): String {
        val count = getProfileCount()
        if (count == 0) {
            val profileName = userName ?: "Varsayılan Profil"
            android.util.Log.d("ProfileRepository", "🆕 Creating default profile with name: $profileName")
            return createProfile(
                name = profileName,
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

    // ==================== Firestore Sync Functions ====================

    /**
     * Save profile to Firestore
     */
    private suspend fun saveProfileToFirestore(profile: ProfileEntity) {
        try {
            val userId = auth.currentUser?.uid ?: return
            val profileData = hashMapOf(
                "id" to profile.id,
                "name" to profile.name,
                "avatarIcon" to profile.avatarIcon,
                "color" to profile.color,
                "pinCode" to profile.pinCode,
                "createdAt" to profile.createdAt,
                "updatedAt" to profile.updatedAt,
                "isActive" to profile.isActive
            )

            firestore.collection("users")
                .document(userId)
                .collection("profiles")
                .document(profile.id)
                .set(profileData)
                .await()

            android.util.Log.d("ProfileRepository", "📤 Profile saved to Firestore: ${profile.name} (${profile.id})")
        } catch (e: Exception) {
            android.util.Log.e("ProfileRepository", "❌ Failed to save profile to Firestore: ${e.message}", e)
            // Don't throw - we want local operations to succeed even if Firestore fails
        }
    }

    /**
     * Delete profile from Firestore
     */
    private suspend fun deleteProfileFromFirestore(profileId: String) {
        try {
            val userId = auth.currentUser?.uid ?: return

            firestore.collection("users")
                .document(userId)
                .collection("profiles")
                .document(profileId)
                .delete()
                .await()

            android.util.Log.d("ProfileRepository", "📤 Profile deleted from Firestore: $profileId")
        } catch (e: Exception) {
            android.util.Log.e("ProfileRepository", "❌ Failed to delete profile from Firestore: ${e.message}", e)
            // Don't throw - we want local operations to succeed even if Firestore fails
        }
    }

    /**
     * Load profiles from Firestore and sync to local database
     * Call this on app startup to sync profiles
     */
    suspend fun syncProfilesFromFirestore() {
        try {
            val userId = auth.currentUser?.uid ?: return

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("profiles")
                .get()
                .await()

            val firestoreProfiles = snapshot.documents.mapNotNull { doc ->
                try {
                    ProfileEntity(
                        id = doc.getString("id") ?: return@mapNotNull null,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        avatarIcon = doc.getString("avatarIcon") ?: "👤",
                        color = doc.getString("color") ?: "#6200EE",
                        pinCode = doc.getString("pinCode"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        isActive = doc.getBoolean("isActive") ?: false
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ProfileRepository", "❌ Failed to parse profile from Firestore: ${e.message}")
                    null
                }
            }

            // Sync to local database
            if (firestoreProfiles.isNotEmpty()) {
                firestoreProfiles.forEach { profile ->
                    profileDao.insertProfile(profile)
                }
                android.util.Log.d("ProfileRepository", "📥 Synced ${firestoreProfiles.size} profiles from Firestore")
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileRepository", "❌ Failed to sync profiles from Firestore: ${e.message}", e)
            // Don't throw - app should work offline
        }
    }
}
