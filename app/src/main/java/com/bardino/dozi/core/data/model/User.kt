package com.bardino.dozi.core.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = 0L,
    val planType: String = "free",
    val timezone: String = "Europe/Istanbul",
    val language: String = "tr",
    val vibration: Boolean = true,
    val theme: String = "light",
    val onboardingCompleted: Boolean = false
)
