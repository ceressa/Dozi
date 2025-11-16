package com.bardino.dozi.core.data.model

/**
 * Profile statistics data model
 */
data class ProfileStats(
    val profileId: String,
    val profileName: String,
    val totalMedicines: Int = 0,
    val todayMedicines: Int = 0,
    val takenToday: Int = 0,
    val missedToday: Int = 0,
    val complianceRate: Float = 0f,  // 0-100
    val last7DaysCompliance: List<DayCompliance> = emptyList()
)

/**
 * Daily compliance data
 */
data class DayCompliance(
    val date: String,  // "dd/MM"
    val taken: Int,
    val total: Int,
    val rate: Float  // 0-100
)
