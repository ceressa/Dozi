package com.bardino.dozi.core.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val planType: String = "free",
    val timezone: String = "",
    val language: String = "tr",
    val vibration: Boolean = true,
    val theme: String = "light",
    val onboardingCompleted: Boolean = false
)
