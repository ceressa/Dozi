package com.bardino.dozi.core.data.local.dao

import androidx.room.*
import com.bardino.dozi.core.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    /**
     * Get all profiles
     */
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    /**
     * Get all profiles (non-Flow version for direct access)
     */
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    suspend fun getAllProfilesList(): List<ProfileEntity>

    /**
     * Get profile by ID
     */
    @Query("SELECT * FROM profiles WHERE id = :profileId")
    suspend fun getProfileById(profileId: String): ProfileEntity?

    /**
     * Get currently active profile
     */
    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<ProfileEntity?>

    /**
     * Get currently active profile (non-Flow version)
     */
    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileDirect(): ProfileEntity?

    /**
     * Get profile count
     */
    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int

    /**
     * Insert new profile
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    /**
     * Update profile
     */
    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    /**
     * Delete profile
     */
    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    /**
     * Delete profile by ID
     */
    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: String)

    /**
     * Deactivate all profiles (before setting new active profile)
     */
    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    /**
     * Set active profile
     */
    @Transaction
    suspend fun setActiveProfile(profileId: String) {
        deactivateAllProfiles()
        // Update the selected profile to be active
        val profile = getProfileById(profileId)
        profile?.let {
            updateProfile(it.copy(isActive = true))
        }
    }
}
