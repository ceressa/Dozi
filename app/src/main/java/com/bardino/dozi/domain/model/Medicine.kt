package com.dozi.dozi.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey val id: String,
    val name: String,
    val dosage: String,
    val form: String,          // Tablet/Kapsül/Şurup...
    val stockCount: Int,
    val boxSize: Int,
    val notes: String? = null,
)
