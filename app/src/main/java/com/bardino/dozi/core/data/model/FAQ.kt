package com.bardino.dozi.core.data.model

data class FAQ(
    val id: String = "",
    val question: String = "",
    val answer: String = "",
    val category: String = "",
    val order: Int = 0,
    val isActive: Boolean = true
)

data class FAQCategory(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val order: Int = 0
)
