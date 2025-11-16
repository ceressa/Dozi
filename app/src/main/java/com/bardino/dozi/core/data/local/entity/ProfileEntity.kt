package com.bardino.dozi.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for user profiles
 * Allows multiple people (e.g., family members) to track medications separately
 *
 * Premium feature: 1 profile is free, 2+ profiles require premium
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,                     // UUID
    val name: String,                   // Profile name (e.g., "Anne", "Baba", "Çocuk")
    val avatarIcon: String,             // Emoji or icon identifier
    val color: String,                  // Hex color code for UI personalization
    val createdAt: Long,                // Epoch millis
    val updatedAt: Long,                // Epoch millis
    val isActive: Boolean = false       // Currently active profile
)
